package com.BeanFactoryPostProcessorTest;

import com.importTest.Person;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

/**
 * @author baofeng
 * @date 2022/03/07
 */
@Configuration
public class ImportConfig3 {

    @Bean(initMethod = "initMethod")
//    @Lazy
    public CustomBean customBean() {
        CustomBean customBean = new CustomBean();
        customBean.setDesc("111");
        customBean.setRemark("222");
        return customBean;
    }

    @Bean
    public MyBeanFactoryPostProcessor myBeanFactoryPostProcessor(){
        return new MyBeanFactoryPostProcessor();
    }

    @Bean
    public MyBeanPostProcessor myBeanPostProcessor(){
        return new MyBeanPostProcessor();
    }
}
