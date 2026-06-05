package com.compensate;

import com.listen.dao.EventListenDAO;
import com.listen.dto.EventListenDO;
import com.listen.support.DomainEventListenSupport;
import com.notify.dao.EventDAO;
import com.notify.dto.EventDO;
import com.send.DomainEventDaoFactory;
import com.send.EventNotifyServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import util.DdcTaskExecutor;

import javax.sql.DataSource;
import java.util.List;

/**
 * @author baofeng
 */
@Slf4j
public class DdcCompensateServiceImpl implements DdcCompensateService {

    /**
     * 超过该秒数仍未处理，才纳入补偿（给正常首次投递留出缓冲时间）
     */
    private static final int DEFAULT_DELAY_SECONDS = 300;

    /**
     * 单次扫描上限，避免瞬间压垮线程池
     */
    private static final int DEFAULT_LIMIT = 100;

    private final DataSource dataSource;
    private final int delaySeconds;
    private final int limit;

    public DdcCompensateServiceImpl(DataSource dataSource) {
        this(dataSource, DEFAULT_DELAY_SECONDS, DEFAULT_LIMIT);
    }

    public DdcCompensateServiceImpl(DataSource dataSource, int delaySeconds, int limit) {
        this.dataSource = dataSource;
        this.delaySeconds = delaySeconds;
        this.limit = limit;
    }

    @Override
    public void compensateNotify() {
        EventDAO eventDAO = DomainEventDaoFactory.createEventDAO(dataSource);
        List<EventDO> pendingList;
        try {
            pendingList = eventDAO.queryPendingEventList(delaySeconds, limit);
        } catch (Exception e) {
            log.error("compensateNotify query error", e);
            return;
        }
        if (CollectionUtils.isEmpty(pendingList)) {
            log.info("compensateNotify: no pending events");
            return;
        }
        log.info("compensateNotify: found {} pending events, delaySeconds:{}", pendingList.size(), delaySeconds);
        for (EventDO event : pendingList) {
            DdcTaskExecutor.execute(() -> {
                try {
                    EventNotifyServiceFactory.getEventNotifyService().eventNotify(event);
                } catch (Exception e) {
                    log.error("compensateNotify eventNotify error, eventId:{}", event.getId(), e);
                }
            });
        }
    }

    @Override
    public void compensateListen() {
        EventListenDAO eventListenDAO = DomainEventDaoFactory.createEventListenDAO(dataSource);
        List<EventListenDO> pendingList;
        try {
            pendingList = eventListenDAO.queryPendingEventListenList(delaySeconds, limit);
        } catch (Exception e) {
            log.error("compensateListen query error", e);
            return;
        }
        if (CollectionUtils.isEmpty(pendingList)) {
            log.info("compensateListen: no pending event listens");
            return;
        }
        log.info("compensateListen: found {} pending event listens, delaySeconds:{}", pendingList.size(), delaySeconds);
        for (EventListenDO eventListen : pendingList) {
            // 先递增 DB 中的 retry_times，canExecute 依据此判断是否超过最大重试次数
            try {
                eventListenDAO.incrementRetryTimes(eventListen.getId());
            } catch (Exception e) {
                log.error("compensateListen incrementRetryTimes error, id:{}", eventListen.getId(), e);
                continue;
            }
            eventListen.setRetryTimes(eventListen.getRetryTimes() + 1);
            DdcTaskExecutor.execute(() -> {
                try {
                    DomainEventListenSupport.getDomainEventListenService().dispatch(eventListen);
                } catch (Exception e) {
                    log.error("compensateListen dispatch error, id:{}", eventListen.getId(), e);
                }
            });
        }
    }
}
