package ddctest.listener;

import com.annotation.EventListen;
import com.annotation.EventParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 测试用监听器，覆盖成功/多监听器/失败场景
 */
@Slf4j
@Component
public class OrderEventListener {

    /** 已执行的 orderId 列表，供断言使用 */
    private final List<String> executedOrderIds = Collections.synchronizedList(new ArrayList<>());
    private final List<String> notifiedOrderIds = Collections.synchronizedList(new ArrayList<>());

    // ─── TC-01 / TC-03 ───────────────────────────────────────────────────

    /** 第一个监听器（TC-01 正常流程 & TC-03 多监听器） */
    @EventListen(name = "couponListener",
                 listenDomain = {"order"}, listenEvent = {"paid"},
                 order = 1)
    public void sendCoupon(@EventParam(name = "entityId") String orderId) {
        log.info("couponListener executed, orderId:{}", orderId);
        executedOrderIds.add(orderId);
    }

    /** 第二个监听器（TC-03 多监听器） */
    @EventListen(name = "notificationListener",
                 listenDomain = {"order"}, listenEvent = {"paid"},
                 order = 2)
    public void sendNotification(@EventParam(name = "entityId") String orderId) {
        log.info("notificationListener executed, orderId:{}", orderId);
        notifiedOrderIds.add(orderId);
    }

    // ─── TC-04 ───────────────────────────────────────────────────────────

    /** 故意抛出异常（TC-04 监听器失败场景） */
    @EventListen(name = "failingListener",
                 listenDomain = {"order"}, listenEvent = {"fail"},
                 maxRetryTimes = 0)
    public void onFailEvent(@EventParam(name = "entityId") String orderId) {
        log.info("failingListener executed, orderId:{}", orderId);
        throw new RuntimeException("监听器故意抛出异常，测试失败处理");
    }

    // ─── 供测试断言使用 ────────────────────────────────────────────────

    public List<String> getExecutedOrderIds()  { return executedOrderIds; }
    public List<String> getNotifiedOrderIds()  { return notifiedOrderIds; }

    public void reset() {
        executedOrderIds.clear();
        notifiedOrderIds.clear();
    }
}
