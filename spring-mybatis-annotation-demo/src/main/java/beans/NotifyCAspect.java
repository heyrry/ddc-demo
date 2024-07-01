package beans;

import com.DomainEventApplicationContext;
import com.alibaba.fastjson.JSON;
import com.annotation.EventNotify;
import com.notify.DomainEventNotifyDTO;
import com.runtime.DomainEventNotifyContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import spi.lifecycle.DomainEventLifecycle;

import java.lang.reflect.Method;

/**
 * @author baofeng
 * @date 2023/01/02
 */
@Slf4j
@Component
@Aspect
//@Order(1)
public class NotifyCAspect {


    public NotifyCAspect() {
        System.out.println("NotifyCAspect construct");
    }

    @Pointcut("@annotation(beans.annotation.NotifyC)")
    public void eventPointCutC() {
        System.out.println("NotifyCAspect Pointcut");
    }

    @Around("eventPointCutC()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("NotifyCAspect doAround");
        return pjp.proceed();
    }

}
