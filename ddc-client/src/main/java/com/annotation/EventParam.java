package com.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事件参数
 *
 * @author baofeng
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface EventParam {
    /**
     * 名称
     */
    String name();

    /**
     * 默认值
     *
     * @return
     */
    String defaultValue() default "";
}
