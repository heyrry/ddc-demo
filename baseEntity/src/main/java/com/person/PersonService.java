package com.person;

import org.springframework.stereotype.Service;

/**
 * @author baofeng
 * @date 2023/01/16
 */
@Service
public class PersonService {
    public PersonService() {
        System.out.println("PersonService init");
    }
}
