package com.beanFactoryAndfactoryBean;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author baofeng
 * @date 2022/03/05
 */
public class SpringBeanFactoryMain {
    public static void main(String[] args) {
        //这个是BeanFactory
        BeanFactory beanFactory = new ClassPathXmlApplicationContext("application.xml");
        //获取对应的对象化
        Object demo = beanFactory.getBean("demo");
        System.out.println(demo instanceof Persons);
        System.out.println("demo=" + demo);
        //获取从工厂Bean中获取对象
        Persons demoFromFactory = beanFactory.getBean("demoFromFactory", Persons.class);
        System.out.println("demoFromFactory=" + demoFromFactory);
        //获取对应的personFactory
        Object bean = beanFactory.getBean("&demoFromFactory");
        System.out.println(bean instanceof PersonFactoryBean);
        PersonFactoryBean factoryBean=(PersonFactoryBean) bean;
        System.out.println("初始化参数为："+factoryBean.getInitStr());
    }
}
