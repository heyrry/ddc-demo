package beans;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author baofeng
 * @date 2023/01/02
 */
@Slf4j
@Component
@Aspect
@Order(2)
public class NotifyBAspect {


    public NotifyBAspect() {
        System.out.println("NotifyBAspect construct");
    }

    @Pointcut("@annotation(beans.annotation.NotifyB)")
    public void eventPointCutB() {
        System.out.println("NotifyBAspect Pointcut");
    }

    @Around("eventPointCutB()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("NotifyBAspect doAround");
        return pjp.proceed();
    }

}
