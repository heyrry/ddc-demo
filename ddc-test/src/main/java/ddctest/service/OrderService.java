package ddctest.service;

import com.annotation.EventNotify;
import com.runtime.DomainEventNotifyContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 模拟业务服务，覆盖 ddc-client 的典型使用场景
 */
@Service
public class OrderService {

    /** TC-01 正常流程：事务提交，事件落库，监听器执行 */
    @EventNotify(domain = "order", event = "paid")
    @Transactional(rollbackFor = Exception.class)
    public void pay(String orderId) {
        DomainEventNotifyContext.put(DomainEventNotifyContext.ENTITY_ID, orderId);
        DomainEventNotifyContext.put("orderId", orderId);
        DomainEventNotifyContext.put("amount", 100);
    }

    /** TC-02 事务回滚：业务异常，事件不落库 */
    @EventNotify(domain = "order", event = "paid")
    @Transactional(rollbackFor = Exception.class)
    public void payAndRollback(String orderId) {
        DomainEventNotifyContext.put(DomainEventNotifyContext.ENTITY_ID, orderId);
        throw new RuntimeException("模拟业务异常，触发回滚");
    }

    /** TC-04 监听器异常：触发会失败的监听器 */
    @EventNotify(domain = "order", event = "fail")
    @Transactional(rollbackFor = Exception.class)
    public void payTriggerFail(String orderId) {
        DomainEventNotifyContext.put(DomainEventNotifyContext.ENTITY_ID, orderId);
    }
}
