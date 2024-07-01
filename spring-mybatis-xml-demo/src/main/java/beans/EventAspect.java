package beans;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author baofeng
 * @date 2023/01/02
 */
@Slf4j
@Component
@EnableAspectJAutoProxy
public class EventAspect {

    @Pointcut("@annotation(beans.Event)")
    public void domainEventNotifyPointcut() {
    }

    @Around("domainEventNotifyPointcut()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method method = ms.getMethod();
        String methodName = method.getName();
        if (log.isDebugEnabled()) {
            log.debug("DomainEventNotifyAspect doAround start,method:" + methodName);
        }
        Object[] args = pjp.getArgs();
        //设置参数
        Method targetMethod = pjp.getTarget().getClass().getDeclaredMethod(methodName, method.getParameterTypes());
        Event annotation = AnnotationUtils.findAnnotation(targetMethod, Event.class);
        log.info("annotation:" + JSON.toJSONString(annotation));
        //业务处理
        Object result = pjp.proceed(args);
        if (log.isDebugEnabled()) {
            log.debug("DomainEventNotifyAspect doAround proceed succ,method:" + methodName);
        }
        return result;

    }

}
