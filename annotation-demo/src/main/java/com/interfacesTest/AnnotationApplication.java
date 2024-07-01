package com.interfacesTest;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author baofeng
 * @date 2022/02/13
 */
public class AnnotationApplication {
    public static void main(String[] args) {
        //获取容器对象
        ApplicationContext ac = new AnnotationConfigApplicationContext(Config.class);
        //通过容器获取配置的javabean
        Car car = (Car) ac.getBean("car");
        System.out.println(car);

        CarType demoCar = Context.DEFAULT.create("a");
        demoCar.printLogo();
        System.out.println(demoCar);
        CarType demoCarB = Context.DEFAULT.create("b");
        System.out.println(demoCarB);
        CarType demoCarC = Context.DEFAULT.create("c");
        System.out.println(demoCarC);
    }
}
