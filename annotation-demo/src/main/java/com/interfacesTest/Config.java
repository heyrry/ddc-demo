package com.interfacesTest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author baofeng
 * @date 2022/02/13
 */
@Configuration
public class Config {

    @Bean
    public Car car(){
        Car car = new Car();
        car.setName("奔驰");
        car.setPrice(100000);
        return car;
    }

}
