package spi.pointcut;

import com.notify.DomainEventNotifyDTO;
import com.notify.DomainEventNotifyInstance;
import com.notify.dto.EventDO;

/**
 * @author baofeng
 * @date 2023/07/10
 */
public class PointcutInstanceFactory {
    public static PointcutInstance<EventDO> createDomainEventNotifyInstance(DomainEventNotifyDTO domainEventNotify) {
        return new DomainEventNotifyInstance(domainEventNotify);
    }
}
