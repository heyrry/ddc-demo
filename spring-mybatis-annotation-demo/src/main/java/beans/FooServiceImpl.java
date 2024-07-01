package beans;

import beans.annotation.NotifyA;
import beans.annotation.NotifyB;
import beans.annotation.NotifyC;
import beans.dao.EmployeeMapper;
import com.annotation.EventNotify;
import com.runtime.DomainEventNotifyContext;
import model.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.logging.Logger;

import static com.runtime.DomainEventNotifyContext.ENTITY_ID;
import static java.util.Optional.ofNullable;

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

    @Autowired
    private EmployeeMapper employeeMapper;

    @Transactional(rollbackFor = Exception.class)
    public void noInsert(){
        logger.info("[noInsert] start insert foo");
        if (null != eventPublisher) {
            eventPublisher.publishEvent(new MyTransactionEvent("test", this));
        }
        logger.info("[noInsert] finish insert foo");
        // 判断是否有事务
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager
                    .registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void beforeCommit(boolean readOnly) {
                            System.out.println("================beforeCommit============");
                        }

                        @Override
                        public void afterCommit() {
                            // 事务提交后执行回调
                            System.out.println("afterCommit");
                        }

                        @Override
                        public void flush() {
                            System.out.println("================flush============");
                        }

                        @Override
                        public void beforeCompletion() {
                            System.out.println("================beforeCompletion============");
                        }

                        @Override
                        public void afterCompletion(int status) {
                            if(TransactionSynchronization.STATUS_COMMITTED == status){
                                System.out.println("=========afterCompletion======事务提交==============");
                            }else if(TransactionSynchronization.STATUS_ROLLED_BACK == status){
                                System.out.println("========afterCompletion=========事务回滚============");
                            }
                        }
                    });
        } else {
            // 事务提交后执行回调
            System.out.println("isActualTransactionActive false");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void noInsert2(){
        logger.info("[noInsert2] start insert foo");

        if(TransactionSynchronizationManager.isSynchronizationActive()){
            // 提交前
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    logger.info("[noInsert2]========beforeCommit========");
                }
            });

            // 提交后
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    logger.info("[noInsert2]=======afterCommit======");
                }
            });

        }else{
            logger.info("[noInsert2]no active TransactionSynchronization");
        }
    }

    public void noInsert3(){
        logger.info("[noInsert3] start insert foo");
        if(TransactionSynchronizationManager.isSynchronizationActive()){
            // 提交前
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    logger.info("[noInsert3]========beforeCommit========");
                }
            });

            // 提交后
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    logger.info("[noInsert3]=======afterCommit======");
                }
            });

        }else{
            logger.info("[noInsert3]no active TransactionSynchronization");
        }
        if (null != eventPublisher) {
            eventPublisher.publishEvent(new MyTransactionEvent("test3", this));
        }
        logger.info("[noInsert3] finish insert foo");
    }

//    @EventNotify(domain = "noInsert4")
    public void noInsert4(){
        logger.info("[noInsert2] start insert foo");

        if(TransactionSynchronizationManager.isSynchronizationActive()){
            // 提交前
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    logger.info("[noInsert2]========beforeCommit========");
                }
            });

            // 提交后
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    logger.info("[noInsert2]=======afterCommit======");
                }
            });

        }else{
            logger.info("[noInsert2]no active TransactionSynchronization");
        }
    }

    @EventNotify(domain = "testDomain", event = "update")
    @Transactional(rollbackFor = Exception.class)
    public void update(){
        logger.info("[update] start insert foo");

        Employee emp = employeeMapper.getEmpById(1);
        logger.info("emp=" + emp);

        int upt = employeeMapper.update(new Employee(1, "a1", "b", "c"));
        logger.info("[update] upt11111111 = " + upt);
        /*if (null != eventPublisher) {
            eventPublisher.publishEvent(new MyTransactionEvent("test", this));
        }*/

        /*transactionTemplate.execute(status -> {
            try {
                int upt22 = employeeMapper.update(new Employee(1, "a2222", "b222", "c222"));
                logger.info("[update] upt22222222 = " + upt22);
            } catch (Exception ex) {
                status.setRollbackOnly();
            }
            return null;
        });*/
        //

//        upt = employeeMapper.update(new Employee(1, "a1", "b", "c"));
//        logger.info("[update] upt3333333 = " + upt);
//        logger.info("[update] finish insert foo");
        DomainEventNotifyContext.put(ENTITY_ID, 1);
        DomainEventNotifyContext.put("a", "aaa");
    }

    /**
     * 测试AOP注解执行顺序
     */
    @Transactional(rollbackFor = Exception.class)
    @NotifyA()
    @NotifyB()
    @NotifyC()
    public void update2(){
        logger.info("[update2] start insert foo");

        Employee emp = employeeMapper.getEmpById(1);
        logger.info("emp=" + emp);

        int upt = employeeMapper.update(new Employee(1, "a1", "b", "c"));
        logger.info("[update2] upt = " + upt);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update3(){
        logger.info("[update3] start insert foo");

        Employee emp = employeeMapper.getEmpById(1);
        logger.info("emp=" + emp);

        int upt = employeeMapper.update(new Employee(1, "a1", "b", "c"));
        logger.info("[update3] upt = " + upt);

        logger.info("[update3] finish insert foo");
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
            logger.info("[update3]no active TransactionSynchronization");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void update4(){
        logger.info("[update4] start insert foo");

        // 提交前
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                logger.info("[Thread-beforeCommit]" + Thread.currentThread().getId());
                logger.info("[update4]========beforeCommit========");
            }
        });

        // 提交后
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            @Async
            public void afterCommit() {
                logger.info("[Thread-afterCommit]" + Thread.currentThread().getId());
                logger.info("[update4]=======afterCommit======");
                // 报错不影响已提交的回滚
//                int i = 1/0;
            }
        });

        int upt = employeeMapper.update(new Employee(1, "a222", "b", "c"));
        logger.info("[update4] upt = " + upt);

        logger.info("[update4] finish insert foo");
        logger.info("[Thread-]" + Thread.currentThread().getId());
    }

}
