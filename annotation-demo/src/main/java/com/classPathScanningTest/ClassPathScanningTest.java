package com.classPathScanningTest;

import com.classPathScanningTest.annotation.ScanAnnotation;
import com.classPathScanningTest.interfaces.ScanClassInterface;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.Set;

/**
 * @author baofeng
 * @date 2022/02/16
 */
public class ClassPathScanningTest {
    private static Class annotationclass = ScanAnnotation.class;
    private static Class interfaceClass = ScanClassInterface.class;
    private static String packageName1 = "com.classPathScanningTest";
    private static String packageName2 = "com.classPathScanningTest.*";
    private static String packageName3 = "*";
    private static String packageName4 = "com.classPathScanningTest.annotation";
    private static String packageName5 = "com.classPathScanningTest.interfaces";

    public static void main(String[] args) throws Exception{
        findAnnotationClassBySpring();
        //仿spring写法
        findAnnotationClassBySpringModel();
        //
        findInterfaceImplClassBySpring();
    }

    private static void findAnnotationClassBySpringModel() {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(packageName1);
        scanner.doScan();
    }


    private static void findAnnotationClassBySpring() {
        // true：默认TypeFilter生效，这种模式会查询出许多不符合你要求的class名
        // false：关闭默认TypeFilter
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        // 扫描带有自定义注解的类
        provider.addIncludeFilter(new AnnotationTypeFilter(annotationclass));
        Set<BeanDefinition> scanList = provider.findCandidateComponents(packageName1);
        System.out.println("=====findAnnotationClassBySpring,pakage com.classPathScanningTest====");
        for (BeanDefinition beanDefinition : scanList) {
            System.out.println(beanDefinition.getBeanClassName());
        }
        Set<BeanDefinition> scanList2 = provider.findCandidateComponents(packageName2);
        System.out.println("=====findAnnotationClassBySpring,pakage com.classPathScanningTest.*====");
        for (BeanDefinition beanDefinition : scanList2) {
            System.out.println(beanDefinition.getBeanClassName());
        }
        Set<BeanDefinition> scanList3 = provider.findCandidateComponents(packageName3);
        System.out.println("=====findAnnotationClassBySpring,pakage *====");
        for (BeanDefinition beanDefinition : scanList3) {
            System.out.println(beanDefinition.getBeanClassName());
        }
        Set<BeanDefinition> scanList4 = provider.findCandidateComponents(packageName4);
        System.out.println("=====findAnnotationClassBySpring,pakage com.classPathScanningTest.annotation====");
        for (BeanDefinition beanDefinition : scanList4) {
            System.out.println(beanDefinition.getBeanClassName());
        }
        Set<BeanDefinition> scanList5 = provider.findCandidateComponents(packageName5);
        System.out.println("=====findAnnotationClassBySpring,pakage com.classPathScanningTest.interfaces====");
        for (BeanDefinition beanDefinition : scanList5) {
            System.out.println(beanDefinition.getBeanClassName());
        }
    }

    private static void findInterfaceImplClassBySpring() {
        // true：默认TypeFilter生效，这种模式会查询出许多不符合你要求的class名
        // false：关闭默认TypeFilter
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        // 扫描带有自定义注解的类
        provider.addIncludeFilter(new AssignableTypeFilter(interfaceClass));
        Set<BeanDefinition> scanList = provider.findCandidateComponents(packageName1);
        System.out.println("=====findInterfaceImplClassBySpring,pakage com.classPathScanningTest====");
        for (BeanDefinition beanDefinition : scanList) {
            System.out.println(beanDefinition.getBeanClassName());
        }
        Set<BeanDefinition> scanList2 = provider.findCandidateComponents(packageName2);
        System.out.println("=====findInterfaceImplClassBySpring,pakage com.classPathScanningTest.*====");
        for (BeanDefinition beanDefinition : scanList2) {
            System.out.println(beanDefinition.getBeanClassName());
        }
        Set<BeanDefinition> scanList3 = provider.findCandidateComponents(packageName3);
        System.out.println("=====findInterfaceImplClassBySpring,pakage *====");
        for (BeanDefinition beanDefinition : scanList3) {
            System.out.println(beanDefinition.getBeanClassName());
        }
        Set<BeanDefinition> scanList4 = provider.findCandidateComponents(packageName4);
        System.out.println("=====findInterfaceImplClassBySpring,pakage com.classPathScanningTest.annotation====");
        for (BeanDefinition beanDefinition : scanList4) {
            System.out.println(beanDefinition.getBeanClassName());
        }
        Set<BeanDefinition> scanList5 = provider.findCandidateComponents(packageName5);
        System.out.println("=====findInterfaceImplClassBySpring,pakage com.classPathScanningTest.interfaces====");
        for (BeanDefinition beanDefinition : scanList5) {
            System.out.println(beanDefinition.getBeanClassName());
        }
    }

    private static void findClassBySpring() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        // true：默认TypeFilter生效，这种模式会查询出许多不符合你要求的class名
        // false：关闭默认TypeFilter
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(
                false);

        // 扫描带有自定义注解的类
        provider.addIncludeFilter(new AnnotationTypeFilter(ScanAnnotation.class));

        // 接口不会被扫描，其子类会被扫描出来
        provider.addIncludeFilter(new AssignableTypeFilter(ScanClassInterface.class));

        // Spring会将 .换成/  ("."-based package path to a "/"-based)
        // Spring拼接的扫描地址：classpath*:xxx/xxx/xxx/**/*.class
        // Set<BeanDefinition> scanList = provider.findCandidateComponents("com.p7.demo.scanclass");
        Set<BeanDefinition> scanList = provider.findCandidateComponents("*");

        for (BeanDefinition beanDefinition : scanList) {
            System.out.println(beanDefinition.getBeanClassName());
            Class clazz = Class.forName(beanDefinition.getBeanClassName());
            clazz.newInstance();
        }
        System.out.println("=========loadClass default=======");
        for (BeanDefinition beanDefinition : scanList) {
            System.out.println(beanDefinition.getBeanClassName());
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            Class<?> clazz = loader.loadClass(beanDefinition.getBeanClassName());
            System.out.println(clazz);
        }
    }
}
