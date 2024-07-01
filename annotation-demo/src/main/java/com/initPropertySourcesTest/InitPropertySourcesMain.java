package com.initPropertySourcesTest;

import com.beanFactoryAndfactoryBean.Persons;

/**
 * @author baofeng
 * @date 2022/03/07
 */
public class InitPropertySourcesMain {
    public static void main(String[] args) {
        MyClassPathXmlApplicationContext ac = new MyClassPathXmlApplicationContext("application.xml");
        Persons persons = (Persons)ac.getBean("demo");
        System.out.println(System.getProperty("address"));
        System.out.println(persons);
    }
}
