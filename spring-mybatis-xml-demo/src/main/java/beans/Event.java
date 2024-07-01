package beans;

import java.lang.annotation.*;

/**
 * @author baofeng
 * @date 2023/01/06
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface Event {
    /**
     * 领域
     */
    String domain();
}
