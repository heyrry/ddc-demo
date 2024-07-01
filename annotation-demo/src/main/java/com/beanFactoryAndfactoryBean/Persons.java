package com.beanFactoryAndfactoryBean;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;

/**
 * @author baofeng
 * @date 2022/03/05
 */
@Data
public class Persons {
    private String name;
    private Integer age;
    @Value("${address}")
    private String address;

    public Persons() {
        System.out.println("Persons construct");
    }

    @PostConstruct
    public void postMan(){
        System.out.println("=======postMan PostConstruct======");
    }
}
