package com.initBeanTest;

import org.springframework.context.annotation.Bean;

/**
 * @author baofeng
 * @date 2022/03/14
 */
public class FooConfiguration {

    @Bean(initMethod = "initMethod")
    public Foo foo() {
        return new Foo();
    }

}
