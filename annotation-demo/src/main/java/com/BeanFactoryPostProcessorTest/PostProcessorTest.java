package com.BeanFactoryPostProcessorTest;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author baofeng
 * @date 2022/03/12
 */
public class PostProcessorTest {
    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(ImportConfig3.class);
        CustomBean bean = (CustomBean) context.getBean("customBean");
        System.out.println("################ 实例化、初始化bean完成");
        System.out.println("****************下面输出结果");
        System.out.println("描述：" + bean.getDesc());
        System.out.println("备注：" + bean.getRemark());

    }
}
