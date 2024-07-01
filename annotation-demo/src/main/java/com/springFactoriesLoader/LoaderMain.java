package com.springFactoriesLoader;

import com.invokeTest.SingerService;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.List;

/**
 * @author baofeng
 * @date 2022/03/07
 */
public class LoaderMain {
    public static void main(String[] args) {
        List<String> classes = SpringFactoriesLoader.loadFactoryNames(SingerService.class, LoaderMain.class.getClassLoader());
        classes.forEach(clazz -> {
            System.out.println("==== " + clazz);
        });

        List<SingerService> instances = SpringFactoriesLoader.loadFactories(SingerService.class, LoaderMain.class.getClassLoader());
        instances.forEach(clazz -> {
            System.out.println("**** " + clazz);
        });
    }
}
