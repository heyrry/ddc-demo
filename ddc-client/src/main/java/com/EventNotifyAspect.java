package com;

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
@Order(1)
public class EventNotifyAspect {

    /**
     * 领域事件通知注解
     */
    private final String DOMAIN_EVENT_NOTIFY_PROPERTIES = "DomainEventNotify";

    public EventNotifyAspect() {
        System.out.println("EventAspect");
    }

    @Pointcut("@annotation(com.annotation.EventNotify)")
    public void eventPointCut() {
        System.out.println("11111111");
    }

    @Around("eventPointCut()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method method = ms.getMethod();
        String methodName = method.getName();
        if (log.isDebugEnabled()) {
            log.debug("DomainEventNotifyAspect doAround start,method:" + methodName);
        }
        Object[] args = pjp.getArgs();
        DomainEventApplicationContext domainEventApplicationContext = DomainEventApplicationContext.getInstance();
        if (domainEventApplicationContext == null) {
            // 环境未初始化
            log.error("DomainEventNotifyAspect doAround domainEventApplicationContext is null,method:{}", methodName);
            throw new Exception("DomainEventApplicationContext is not inited");
        }
        if (DomainEventNotifyContext.get(DOMAIN_EVENT_NOTIFY_PROPERTIES) != null) {
            // 嵌套传播机制先忽略，后续可由用户定制
            log.error("DomainEventNotifyAspect doAround ignore,already start,method:{}", methodName);
            return pjp.proceed(args);
        }
        DomainEventLifecycle<DomainEventNotifyDTO> domainEventNotifyLifecycle = domainEventApplicationContext.getDomainEventNotifyLifecycle();
        try {
            //设置参数
            Method targetMethod = pjp.getTarget().getClass().getDeclaredMethod(methodName, method.getParameterTypes());
            EventNotify annotation = AnnotationUtils.findAnnotation(targetMethod, EventNotify.class);
            log.info("annotation:" + JSON.toJSONString(annotation));
            // 防止标签嵌套
            DomainEventNotifyContext.put(DOMAIN_EVENT_NOTIFY_PROPERTIES, annotation);
            //切点开始
            DomainEventNotifyDTO dto = buildDomainEventNotifyDTO(annotation);
            domainEventNotifyLifecycle.start(dto);
            // 业务处理
            Object result = pjp.proceed(args);
            if (log.isDebugEnabled()) {
                log.debug("DomainEventNotifyAspect doAround proceed success, method:{}", methodName);
            }
            return result;
        } catch (Exception e) {
            log.error("DomainEventNotifyAspect doAround error,method:{}", methodName, e);
            throw e;
        } finally {
            try {
                //切点结束
                domainEventNotifyLifecycle.end();
            } catch (Throwable e) {
                log.error("DomainEventNotifyAspect doAround DomainEventNotifyLifecycle end error", e);
            }
        }
    }

    private DomainEventNotifyDTO buildDomainEventNotifyDTO(EventNotify annotation) {
        DomainEventNotifyDTO domainEventNotifyDTO = new DomainEventNotifyDTO();
        domainEventNotifyDTO.setDomain(annotation.domain());
        domainEventNotifyDTO.setEvent(annotation.event());
        return domainEventNotifyDTO;
    }

}
