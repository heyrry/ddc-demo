package com.initBeanTest;

import org.springframework.beans.factory.InitializingBean;

import javax.annotation.PostConstruct;

/**
 * @author baofeng
 * @date 2022/03/14
 */
public class Foo implements InitializingBean {

    public Foo() {
        System.out.println("Foo construct");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("afterPropertiesSet()");
    }

    @PostConstruct
    public void init() {
        System.out.println("@PostConstruct");
    }

    private void initMethod() {
        System.out.println("initMethod()");
    }


}
