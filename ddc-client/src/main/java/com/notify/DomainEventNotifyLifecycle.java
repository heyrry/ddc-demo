package com.notify;

import com.PointcutManager;
import com.notify.dto.EventDO;
import com.runtime.DomainEventNotifyContext;
import com.send.EventNotifyServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import spi.lifecycle.DomainEventLifecycle;
import spi.pointcut.PointcutInstance;
import spi.pointcut.PointcutInstanceFactory;
import util.DdcTaskExecutor;

import javax.sql.DataSource;
import java.util.function.Supplier;

/**
 * @author baofeng
 * @date 2023/06/05
 */
@Slf4j
public class DomainEventNotifyLifecycle implements DomainEventLifecycle<DomainEventNotifyDTO> {

    private DataSource dataSource;

    public DomainEventNotifyLifecycle(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void init() {
        EventNotifyServiceFactory.init(dataSource);
    }

    @Override
    public void start(DomainEventNotifyDTO domainEvent) {
        if (domainEvent == null) {
            return;
        }
        PointcutInstance<EventDO> pointcutInstance = PointcutInstanceFactory.createDomainEventNotifyInstance(domainEvent);
        PointcutManager.setPointcutInstance(pointcutInstance);
    }

    @Override
    public void end() {
        // 若事务尚未提交，待事务提交后执行
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                 @Override
                public void afterCompletion(int status) {
                    try {
                        doEndEvent();
                    } catch (Exception e) {
                        log.error("DomainEventNotifyAspect afterCommit end error", e);
                    }
                }
            });
        } else {
            doEndEvent();
        }
    }

    private void doEndEvent() {
        // 清除缓存
        DomainEventNotifyContext.clear();
        // Listener执行
        PointcutInstance<EventDO> pointcutInstance = PointcutManager.removePointcutInstance();
        // 清除缓存
        PointcutManager.clear();
        if (pointcutInstance == null) {
            log.info("PointcutManager remove domainEventNotify null, return");
            return;
        }
        EventDO eventDO = pointcutInstance.finishEvent();
        if (eventDO == null) {
            log.info("pointcutInstance finishEvent, eventDO  null, return");
            return;
        }
        // notify 和 Listen 事务解耦
        DdcTaskExecutor.execute(() -> {
            EventNotifyServiceFactory.getEventNotifyService().eventNotify(eventDO);
        });
    }



    @Override
    public void destroy() {

    }


}
