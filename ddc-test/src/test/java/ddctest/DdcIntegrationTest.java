package ddctest;

import com.compensate.DdcCompensateService;
import com.compensate.DdcCompensateServiceImpl;
import com.constant.ErrorCode;
import com.listen.dto.DomainEventListenDTO;
import com.listen.dto.EventListenDO;
import com.listen.support.DomainEventListenSupport;
import com.notify.dto.EventDO;
import ddctest.listener.OrderEventListener;
import ddctest.service.OrderService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import util.Result;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.Assert.*;

/**
 * ddc-client 全流程集成测试。
 *
 * 覆盖场景：
 *   TC-01  正常流程：事件落库 + 监听器被调用
 *   TC-02  事务回滚：事件不落库
 *   TC-03  多监听器：位图全部清零
 *   TC-04  监听器失败：错误信息写入、listenResult 位保留
 *   TC-05  幂等防重：重复 eventId 只消费一次
 *   TC-06  补偿任务：pending 事件被重新投递
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DdcTestConfig.class)
public class DdcIntegrationTest {

    @Autowired private OrderService      orderService;
    @Autowired private OrderEventListener listener;
    @Autowired private DataSource         dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        // 每个用例前清空两张表，保证隔离
        jdbc.update("DELETE FROM ddc_event_listen");
        jdbc.update("DELETE FROM ddc_event");
        listener.reset();
    }

    // ─────────────────────────────────────────────────────────────────────
    // TC-01  正常流程
    // ─────────────────────────────────────────────────────────────────────

    @Test
    public void TC01_正常流程_事件落库且监听器被调用() throws Exception {
        orderService.pay("TC01");

        // ddc_event 在事务提交时同步写入，直接查
        EventDO event = awaitEvent("TC01", "completed", 3_000);
        assertNotNull("ddc_event 应有记录", event);
        assertEquals("completed", event.getState());
        assertEquals("order",     event.getDomain());
        assertEquals("paid",      event.getEvent());

        // ddc_event_listen 由线程池异步写入，等待 listen_result=0（两个 listener 都完成）
        EventListenDO listen = awaitListen(event.getId(), 0L, 5_000);
        assertNotNull("ddc_event_listen 应有记录", listen);
        assertEquals("两个 listener 都应执行完毕", 0L, listen.getListenResult().longValue());

        // 断言 listener 的业务方法确实被调用
        assertTrue("couponListener 应被调用",       listener.getExecutedOrderIds().contains("TC01"));
        assertTrue("notificationListener 应被调用", listener.getNotifiedOrderIds().contains("TC01"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // TC-02  事务回滚
    // ─────────────────────────────────────────────────────────────────────

    @Test
    public void TC02_事务回滚_事件不落库() throws Exception {
        try {
            orderService.payAndRollback("TC02");
        } catch (RuntimeException ignored) {}

        Thread.sleep(300);   // 等待可能的异步操作结束

        List<EventDO> events = queryEvents("TC02");
        assertEquals("事务回滚后 ddc_event 不应有任何记录", 0, events.size());
        assertFalse("监听器不应被调用", listener.getExecutedOrderIds().contains("TC02"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // TC-03  多监听器位图
    // ─────────────────────────────────────────────────────────────────────

    @Test
    public void TC03_多监听器_listenResult位图全部清零() throws Exception {
        orderService.pay("TC03");

        EventDO event = awaitEvent("TC03", "completed", 3_000);
        // 2 个 listener → 初始 listenResult = 2^2 - 1 = 3，全成功后应为 0
        EventListenDO listen = awaitListen(event.getId(), 0L, 5_000);

        assertEquals("两个 listener 都成功，位图应全部清零", 0L, listen.getListenResult().longValue());

        String names = listen.getListenNames();
        assertTrue("应包含 couponListener",       names.contains("couponListener"));
        assertTrue("应包含 notificationListener", names.contains("notificationListener"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // TC-04  监听器失败
    // ─────────────────────────────────────────────────────────────────────

    @Test
    public void TC04_监听器失败_错误信息记录且位图保留() throws Exception {
        orderService.payTriggerFail("TC04");

        // notify 侧正常完成
        EventDO event = awaitEvent("TC04", "completed", 3_000);
        assertNotNull(event);

        // listen 侧 listener 抛出异常，位图对应位不清零，等待写入 error_info
        Thread.sleep(1_000);

        List<EventListenDO> listens = jdbc.query(
                "SELECT * FROM ddc_event_listen WHERE event_id = ?",
                new BeanPropertyRowMapper<>(EventListenDO.class), event.getId());

        assertFalse("应有 listen 记录", listens.isEmpty());
        EventListenDO listen = listens.get(0);
        assertTrue("listener 失败后 listenResult 不应为 0", listen.getListenResult() > 0);
        assertNotNull("应记录 errorInfo",                    listen.getErrorInfo());
        assertFalse("errorInfo 不应为空",                    listen.getErrorInfo().trim().isEmpty());
    }

    // ─────────────────────────────────────────────────────────────────────
    // TC-05  幂等防重
    // ─────────────────────────────────────────────────────────────────────

    @Test
    public void TC05_幂等防重_相同eventId只消费一次() throws Exception {
        orderService.pay("TC05");

        EventDO event = awaitEvent("TC05", "completed", 3_000);
        awaitListen(event.getId(), 0L, 5_000);  // 等第一次消费完

        // 模拟补偿任务重复投递：用相同 eventId 再次调用 saveListenEvent
        DomainEventListenDTO dto = new DomainEventListenDTO();
        dto.setEventId(event.getId());
        dto.setDomain("order");
        dto.setEvent("paid");
        dto.setEventContext("{}");

        Result<?> result = DomainEventListenSupport.getDomainEventListenService().saveListenEvent(dto);
        assertEquals("重复 eventId 应返回 REPEAT_RECV", ErrorCode.REPEAT_RECV, result.getCode());

        // DB 中仍只有一条记录
        List<EventListenDO> listens = jdbc.query(
                "SELECT * FROM ddc_event_listen WHERE event_id = ?",
                new BeanPropertyRowMapper<>(EventListenDO.class), event.getId());
        assertEquals("幂等后只应有一条 listen 记录", 1, listens.size());
    }

    // ─────────────────────────────────────────────────────────────────────
    // TC-06  补偿任务
    // ─────────────────────────────────────────────────────────────────────

    @Test
    public void TC06_补偿任务_pending事件被重新投递() throws Exception {
        // 直接插入一条 state=pending 的事件，gmt_event 设为 600 秒前
        // 避免与 date_sub(now(), interval 300 second) 的边界条件冲突
        jdbc.update(
            "INSERT INTO ddc_event(gmt_create,gmt_modified,gmt_event,domain,entity_id,event," +
            "                      state,retry_times,version,event_context) " +
            "VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,DATEADD(SECOND,-600,CURRENT_TIMESTAMP)," +
            "       'order','TC06','paid','pending',0,1,'{\"entityId\":\"TC06\"}')");

        // delaySeconds=300：查找 300 秒前的 pending 事件，600s 前的记录满足条件
        DdcCompensateService compensate = new DdcCompensateServiceImpl(dataSource, 300, 10);
        compensate.compensateNotify();

        // 等待补偿异步执行完成
        EventDO event = awaitEvent("TC06", "completed", 5_000);
        assertNotNull("补偿后事件状态应变为 completed", event);

        EventListenDO listen = awaitListen(event.getId(), 0L, 5_000);
        assertNotNull("补偿后应创建并完成 listen 记录", listen);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 工具方法
    // ─────────────────────────────────────────────────────────────────────

    /** 轮询等待 ddc_event 达到指定状态，超时后返回当前值 */
    private EventDO awaitEvent(String entityId, String expectedState, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            List<EventDO> list = queryEvents(entityId);
            if (!list.isEmpty() && expectedState.equals(list.get(0).getState())) {
                return list.get(0);
            }
            Thread.sleep(100);
        }
        List<EventDO> list = queryEvents(entityId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 轮询等待 ddc_event_listen.listenResult 达到期望值 */
    private EventListenDO awaitListen(Long eventId, Long expectedResult, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            List<EventListenDO> list = jdbc.query(
                    "SELECT * FROM ddc_event_listen WHERE event_id = ?",
                    new BeanPropertyRowMapper<>(EventListenDO.class), eventId);
            if (!list.isEmpty() && expectedResult.equals(list.get(0).getListenResult())) {
                return list.get(0);
            }
            Thread.sleep(100);
        }
        List<EventListenDO> list = jdbc.query(
                "SELECT * FROM ddc_event_listen WHERE event_id = ?",
                new BeanPropertyRowMapper<>(EventListenDO.class), eventId);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<EventDO> queryEvents(String entityId) {
        return jdbc.query(
                "SELECT * FROM ddc_event WHERE entity_id = ?",
                new BeanPropertyRowMapper<>(EventDO.class), entityId);
    }
}
