package com.config;

import com.DomainEventApplicationContext;
import com.EventNotifyAspect;
import com.config.postProcessorAlarm.AppLogAlarm;
import com.config.postProcessorAlarm.DingDingAlarm;
import com.transactionManager.MyDataSourceTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * @author baofeng
 * @date 2023/07/26
 */
@Configuration
public class ClientConfiguration {

    @Resource
    private DataSource dataSource;

    /**
     * 领域事件环境
     *
     * @return
     */
    @Bean
    public DomainEventApplicationContext domainEventApplicationContext() {
        return new DomainEventApplicationContext();
    }

    /**
     * 领域事件通知切面
     *
     * @return
     */
    @Bean
    public EventNotifyAspect eventNotifyAspect() {
        return new EventNotifyAspect();
    }

    /**
     * 钉钉报警
     *
     * @return
     */
    @Bean
    public DingDingAlarm dingDingAlarm() {
        return new DingDingAlarm();
    }

    /**
     * 日志告警
     * @return
     */
    @Bean
    public AppLogAlarm appLogAlarm() {
        return new AppLogAlarm();
    }

    @Bean
    public PlatformTransactionManager transactionManager() throws Exception {
        return new MyDataSourceTransactionManager(dataSource);
    }

}
