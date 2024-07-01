package com.transactionManager;

import com.PointcutManager;
import com.alibaba.fastjson.JSONObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import spi.pointcut.PointcutInstance;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author baofeng
 * @date 2023/02/22
 */
@Configuration
@Slf4j
public class MyDataSourceTransactionManager extends DataSourceTransactionManager {

    public MyDataSourceTransactionManager(DataSource dataSource) {
        super(dataSource);
        log.info("MyDataSourceTransactionManager construct enter");
    }

    @Override
    public Object doGetTransaction() {
        log.info("MyDataSourceTransactionManager doGetTransaction begin");
        return super.doGetTransaction();
    }

    @Override
    public boolean isExistingTransaction(Object transaction) {
        log.info("MyDataSourceTransactionManager isExistingTransaction transaction: +");
        return super.isExistingTransaction(transaction);
    }

    @SneakyThrows
    @Override
    public void doBegin(Object transaction, TransactionDefinition definition) {
        log.info("MyDataSourceTransactionManager doBegin start, transaction: +" + ",definition:" + JSONObject.toJSONString(definition));

        super.doBegin(transaction, definition);

        // 获取和当前处理事务相同连接 设置参数必须放在super.doBegin后面，否则会导致不是同一个连接，没法一次性同时提交
        Connection connection = DataSourceUtils.getConnection(super.obtainDataSource());
        //设置参数
        PointcutManager.setParam(PointcutInstance.CONNECTION, connection);
        PointcutInstance pointcutInstance = PointcutManager.getPointcutInstance();
        // 先进入标签，再进入事务
        if (pointcutInstance != null) {
            Map globalParam = PointcutManager.getParam();
            pointcutInstance.setParam(globalParam);
            pointcutInstance.startEvent();
        }
        // 设置自动提交
        log.info("autoCommit:" + connection.getAutoCommit());

        log.info("MyDataSourceTransactionManager doBegin end,  transaction: +"  + ",definition:" + JSONObject.toJSONString(definition));
    }

    @Override
    public void doCommit(DefaultTransactionStatus status) {
        log.info("MyDataSourceTransactionManager doCommit begin" + status.toString());
        PointcutInstance pointcutInstance = PointcutManager.getPointcutInstance();
        try {
            pointcutInstance.createEvent();
        } catch (Throwable e) {
            //回滚
            doRollback(status);
            log.error("DdcConnection createEvent error", e);
            if (e instanceof SQLException) {
                throw e;
            }
            throw e;
        }
        super.doCommit(status);
        pointcutInstance.onSubmitted();
        log.info("MyDataSourceTransactionManager doCommit end");
    }

    @Override
    public void doRollback(DefaultTransactionStatus status) {
        if (log.isDebugEnabled()) {
            log.debug("DdcConnection rollback start...");
        }
        log.info("MyDataSourceTransactionManager doRollback begin" + status.toString());
        super.doRollback(status);
        PointcutInstance pointcutInstance = PointcutManager.getPointcutInstance();
        //回滚完成
        try {
            pointcutInstance.onRolledBack();
        } catch (Throwable e) {
            logger.error("DdcConnection onRolledBack error", e);
        }
        log.info("MyDataSourceTransactionManager doRollback end");
    }

}
