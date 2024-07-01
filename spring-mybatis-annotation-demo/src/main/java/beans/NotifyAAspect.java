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
@Order(3)
public class NotifyAAspect {


    public NotifyAAspect() {
        System.out.println("NotifyAAspect construct");
    }

    @Pointcut("@annotation(beans.annotation.NotifyA)")
    public void eventPointCutA() {
        System.out.println("NotifyAAspect Pointcut");
    }

    @Around("eventPointCutA()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("NotifyAAspect doAround");
        return pjp.proceed();
    }

}
