package beans.annotation;

import com.constant.NotifyMethodEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author baofeng
 * @date 2023/01/06
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface NotifyA {
    /**
     * 领域
     */
    String domain() default "";

    /**
     * 事件
     */
    String event() default "";

    /**
     * 通知方式
     *
     * @return
     */
    NotifyMethodEnum notifyMethod() default NotifyMethodEnum.local;
}
