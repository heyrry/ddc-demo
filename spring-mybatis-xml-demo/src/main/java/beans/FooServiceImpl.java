package beans;

import beans.dao.EmployeeNoAnnotationMapper;
import model.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.logging.Logger;

/**
 * @author baofeng
 * @date 2022/08/29
 */
//@Slf4j
@Service
public class FooServiceImpl {

    private static final Logger logger = Logger.getLogger(FooServiceImpl.class.getName());

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /*@Autowired
    private EmployeeMapper employeeMapper;*/

    @Autowired
    private EmployeeNoAnnotationMapper employeeNoAnnotationMapper;

    @Transactional(rollbackFor = Exception.class)
    @Event(domain = "noInsert")
    public void noInsert(Employee employee){
        logger.info("[noInsert] start insert foo");
        if (null != eventPublisher) {
            eventPublisher.publishEvent(new MyTransactionEvent("test", this));
        }
        logger.info("[noInsert] finish insert foo");
        if(TransactionSynchronizationManager.isSynchronizationActive()){
            // 提交前
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    logger.info("[noInsert]========beforeCommit========");
                }
            });

            // 提交后
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    logger.info("[noInsert]=======afterCommit======");
                }
            });

        }else{
            logger.info("[noInsert]no active TransactionSynchronization");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Event(domain = "update")
    public void update(Employee employee){
        logger.info("[update] start insert foo");

        int upt = employeeNoAnnotationMapper.update(new Employee(1, "a1", "b", "c"));
        logger.info("[update] upt = " + upt);
        if (null != eventPublisher) {
            eventPublisher.publishEvent(new MyTransactionEvent("test", this));
        }
        logger.info("[update] finish insert foo");
        if(TransactionSynchronizationManager.isSynchronizationActive()){
            // 提交前
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    logger.info("[update]========beforeCommit========");
                }
            });

            // 提交后
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    logger.info("[update]=======afterCommit======");
                }
            });

        }else{
            logger.info("[update]no active TransactionSynchronization");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void update2(Employee employee){
        logger.info("[update] start insert foo");

        int upt = employeeNoAnnotationMapper.update(new Employee(1, "a1", "b", "2"));
        logger.info("[update] upt = " + upt);

        int upt2 = employeeNoAnnotationMapper.update(new Employee(1, "a2", "b", "3"));
        logger.info("[update] upt2 = " + upt2);

    }

}
