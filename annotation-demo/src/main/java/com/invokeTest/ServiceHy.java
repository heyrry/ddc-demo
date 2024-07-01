package com.invokeTest;

import java.lang.annotation.*;

/**
 * @author baofeng
 * @date 2022/09/08
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServiceHy {
    String value() default "";
}
