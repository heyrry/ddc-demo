package com;

import com.annotation.EventListen;
import com.listen.support.DomainEventListenSupport;
import com.notify.DomainEventNotifyDTO;
import com.notify.DomainEventNotifyLifecycle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import spi.lifecycle.DomainEventLifecycle;
import util.AnnotationUtil;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author baofeng
 * @date 2023/06/05
 */
@Slf4j
public class DomainEventApplicationContext implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    private static DomainEventApplicationContext instance;
    private DomainEventLifecycle<DomainEventNotifyDTO> domainEventNotifyLifecycle;

    public static DomainEventApplicationContext getInstance() {
        return instance;
    }

    @PostConstruct
    public void init() {
        if (instance == null) {
            synchronized (DomainEventApplicationContext.class) {
                if (instance != null) {
                    return;
                }
                log.info("DomainEventApplicationContext init begin");
                initDomainEventLifecycle();
                initEventListenService();
                instance = this;
            }
        }
    }

    private void initEventListenService() {
        //注册监听器
        registerDomainEventListen();
        //初始化服务
        DomainEventListenSupport.initDomainEventListenService(dataSource);
    }

    private void registerDomainEventListen() {
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        if (beanDefinitionNames == null || beanDefinitionNames.length == 0) {
            log.info("no beanDefinitionNames, return");
            return;
        }
        for (String bean : beanDefinitionNames) {
            Object obj = applicationContext.getBean(bean);
            //查找目标类
            Object targetObj = AnnotationUtil.unwrapProxy(obj);
            if (targetObj == null) {
                continue;
            }
            Class<?> clazz = targetObj.getClass();
            Method[] allDeclaredMethods = clazz.getDeclaredMethods();
            if (allDeclaredMethods == null || allDeclaredMethods.length == 0) {
                continue;
            }
            for (Method method : allDeclaredMethods) {
                EventListen annotation = AnnotationUtils.findAnnotation(method, EventListen.class);
                if (annotation == null) {
                    continue;
                }
                //查找代理方法(注意不能是clazz方法，因为方法需要被Spring代理)
                Method targetMethod = AnnotationUtil.getObjectMethod(obj, method);

                DomainEventListenSupport.registerDomainEventListen(annotation, targetMethod, obj);
                log.info("registerDomainEventListen get DomainEventListen beanName:" + bean + ",className:" + clazz.getName() + ",method:" + method.getName());
            }
        }
    }

    private void initDomainEventLifecycle() {
        domainEventNotifyLifecycle = new DomainEventNotifyLifecycle(dataSource);
        domainEventNotifyLifecycle.init();
    }

    public DomainEventLifecycle<DomainEventNotifyDTO> getDomainEventNotifyLifecycle() {
        return domainEventNotifyLifecycle;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
