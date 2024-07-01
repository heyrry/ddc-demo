package com.annotation;

import com.constant.NotifyMethodEnum;
import org.springframework.core.annotation.Order;

import java.lang.annotation.*;

/**
 * @author baofeng
 * @date 2023/01/06
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface EventNotify {
    /**
     * 领域
     */
    String domain();

    /**
     * 事件
     */
    String event() default "";

    /**
     * 通知方式
     * 预留 支持远程调用
     *
     * @return
     */
    NotifyMethodEnum notifyMethod() default NotifyMethodEnum.local;
}
