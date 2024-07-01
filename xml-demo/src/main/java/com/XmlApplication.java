package com;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author baofeng
 * @date 2022/02/13
 */
public class XmlApplication {
    public static void main(String[] args) {
        //获取容器对象
        ApplicationContext ac = new ClassPathXmlApplicationContext("spring-config.xml");
        //通过容器获取配置的javabean
        Car car = (Car) ac.getBean("car");
        System.out.println(car);
    }
}
