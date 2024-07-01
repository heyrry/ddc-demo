package beans;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.logging.Logger;

/**
 * @author baofeng
 * @date 2022/08/29
 */
//@Slf4j
@Component
public class MyTransactionListener {
    private static final Logger logger = Logger.getLogger(MyTransactionListener.class.getName());

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedEvent(MyTransactionEvent event) {
        System.out.println("transactionEventListener start");
        // do transaction event
        logger.info("event : " + event.getName());
        // finish transaction event
        logger.info("transactionEventListener finish");
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleOrderCreatedEvent2(MyTransactionEvent event) {
        System.out.println("handleOrderCreatedEvent2 start");
        // do transaction event
        logger.info("event2 : " + event.getName());
        // finish transaction event
        logger.info("handleOrderCreatedEvent2 finish");
    }

}
