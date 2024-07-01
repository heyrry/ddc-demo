package com.importTest;

import com.BeanFactoryPostProcessorTest.CustomBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

/**
 * @author baofeng
 * @date 2022/03/07
 */
//@Configuration
@Import(Person.class)
public class ImportConfig1 {

    @Bean()
    CustomBean customBean() {
        CustomBean customBean = new CustomBean();
        return customBean;
    }

}
