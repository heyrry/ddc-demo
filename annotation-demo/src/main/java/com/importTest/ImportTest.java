package com.importTest;

import com.invokeTest.SingerService;
import com.invokeTest.SingerServiceImpl;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 导入的类大体可以分成三大类:
 * 1.普通类
 * 2.实现了ImportSelector接口的类
 * 3.实现了ImportBeanDefinitionRegistrar接口的类
 *
 * @author baofeng
 * @date 2022/03/07
 */
public class ImportTest {
    public static void main(String[] args) {
        // 1.普通类
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ImportConfig1.class);
        Person person = context.getBean(Person.class);
        System.out.println(person);
        System.out.println();

        // 2.实现了ImportSelector接口的类
        AnnotationConfigApplicationContext context2 = new AnnotationConfigApplicationContext(ImportConfig.class);
//        SingerService singerService =  context.getBean(SingerServiceImpl.class);
//        SingerService singerService =  (SingerService)context.getBean("com.invokeTest.SingerService");
//        System.out.println(singerService);
        Person person2 = context2.getBean(Person.class);
        System.out.println(person2);
        SingerService singerService = context2.getBean(SingerService.class);
        System.out.println(singerService);
        System.out.println();

        // 3.实现了ImportBeanDefinitionRegistrar接口的类
        AnnotationConfigApplicationContext context3 = new AnnotationConfigApplicationContext(ImportConfig2.class);
        Person person3 = context3.getBean(Person.class);
        System.out.println(person3);

    }
}
