package com.listener;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author baofeng
 * @date 2022/03/05
 */
public class ListenerMain {
    public static void main(String[] args) {
        //创建springIOC容器
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(BeanConfig.class);
        //从容器中获取事件发布器实例
        CommonPublisher commonPublisher = applicationContext.getBean(CommonPublisher.class);
        //创建事件
//        BusinessEvent businessEvent = new BusinessEvent(new ListenerMain(), "test");
        BusinessEvent businessEvent = new BusinessEvent(new Object(), "BusinessEvent");
        CarEvent carEvent = new CarEvent(new Object(), "CarEvent");
        //发布事件
        commonPublisher.publishBusinessEvent(businessEvent);
        commonPublisher.publishCarEvent(carEvent);
    }
}
