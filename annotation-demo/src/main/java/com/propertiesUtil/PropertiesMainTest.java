package com.propertiesUtil;

import com.importTest.ImportConfig1;
import com.importTest.Person;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author baofeng
 * @date 2022/03/13
 */
public class PropertiesMainTest {
    public static void main(String[] args) {
        // 1.普通类
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(PropertiesUtil.class);
        PropertiesUtil propertiesUtil = context.getBean(PropertiesUtil.class);
        System.out.println(propertiesUtil.getPropertiesValue("db.user"));
    }
}
