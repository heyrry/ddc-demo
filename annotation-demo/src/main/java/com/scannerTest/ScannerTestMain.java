package com.scannerTest;

import com.person.PersonService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author baofeng
 * @date 2022/03/14
 */
public class ScannerTestMain {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        ATest fooService = context.getBean(ATest.class);
        PersonService personService = context.getBean(PersonService.class);
    }
}
