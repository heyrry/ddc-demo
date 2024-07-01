package com.classPathScanningTest;

import com.classPathScanningTest.annotation.ScanAnnotation;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Set;

/**
 * @author baofeng
 * @date 2022/03/07
 */
public class ClassPathBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider {

    private String basePackage;

    public ClassPathBeanDefinitionScanner (String basePackage) {
        this.basePackage = basePackage;
        // 注册默认component
        registerDefaultFilters();
        // 可以再加入默认的注解类
        super.addIncludeFilter(new AnnotationTypeFilter(ScanAnnotation.class));
    }

    public void doScan(){
        Set<BeanDefinition> scanList = findCandidateComponents(basePackage);
        System.out.println("=====ClassPathBeanDefinitionScanner,pakage com.classPathScanningTest====");
        for (BeanDefinition beanDefinition : scanList) {
            System.out.println(beanDefinition.getBeanClassName());
        }

        /*for (String basePackage : basePackages){
            Set<BeanDefinition> scanList = findCandidateComponents(basePackage);
            System.out.println("=====findAnnotationClassBySpring,pakage com.classPathScanningTest====");
            for (BeanDefinition beanDefinition : scanList) {
                System.out.println(beanDefinition.getBeanClassName());
            }
        }*/

    }
}
