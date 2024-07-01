package com.annotation;


import java.lang.annotation.*;

/**
 * 领域事件监听
 *
 * @author baofeng
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface EventListen {
    /**
     * 名称(同一系统名称唯一)
     */
    String name() default "";

    /**
     * 监听领域
     */
    String[] listenDomain() default {};

    /**
     * 监听事件
     */
    String[] listenEvent() default {};

    /**
     * 异步执行
     *
     * @return
     */
    boolean asynchronousExecute() default false;

    /**
     * 顺序(小的先执行)
     *
     * @return
     */
    int order() default 0;

    /**
     * 最大重试次数，-1表示默认，0表示不重试，超过默认值，以默认值为准
     */
    int maxRetryTimes() default -1;
}
