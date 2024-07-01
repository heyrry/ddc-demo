package com.initBeanTest;

import com.importTest.ImportConfig1;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author baofeng
 * @date 2022/03/14
 */
public class FooMain {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(FooConfiguration.class);
    }
}
