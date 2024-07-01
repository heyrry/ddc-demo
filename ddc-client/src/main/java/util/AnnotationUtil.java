package util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.support.AopUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author baofeng
 * @date 2023/07/08
 */
@Slf4j
public class AnnotationUtil {

    public static <T extends Annotation> T findAnnotation(Annotation[] annotations, Class<T> clazz) {
        if (annotations == null || annotations.length == 0 || clazz == null) {
            return null;
        }
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(clazz)) {
                return (T)annotation;
            }
        }
        return null;
    }

    /**
     * 获取代理的目标对象
     *
     * @param bean
     * @return
     * @throws Exception
     */
    public static Object unwrapProxy(Object bean) {
        if (bean == null) {
            return null;
        }
        if (!AopUtils.isAopProxy(bean)) {
            return bean;
        }
        return unwrapProxy(getTarget(bean));
    }

    private static Object getTarget(Object bean) {
        if (bean instanceof Advised) {
            Advised advised = (Advised)bean;
            try {
                Object target = advised.getTargetSource().getTarget();
                log.info("get target object [" + target.getClass().getName() + "] from Advised ,bean:" + bean.getClass().getName());
                return target;
            } catch (Exception e) {
                log.error("AnnotationUtils getTarget parse Advised error,beanClass:" + bean.getClass().getName(), e);
                throw new IllegalStateException(e);
            }
        }
        if (AopUtils.isCglibProxy(bean)) {
            try {
                return getCglibProxyTargetObject(bean);
            } catch (Exception e) {
                log.error("AnnotationUtils getTarget parse cglibProxy error,beanClass:" + bean.getClass().getName(), e);
                throw new IllegalStateException(e);
            }
        }
        if (AopUtils.isJdkDynamicProxy(bean)) {
            try {
                return getJdkDynamicProxyTargetObject(bean);
            } catch (Exception e) {
                log.error("AnnotationUtils getTarget parse jdkDynamicProxy error,beanClass:" + bean.getClass().getName(), e);
                throw new IllegalStateException(e);
            }
        }
        log.error("AnnotationUtils getTarget fail,beanClass:" + bean.getClass().getName());
        throw new IllegalStateException("AnnotationUtils getTarget fail,beanClass:" + bean.getClass());
    }

    private static Object getCglibProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
        h.setAccessible(true);
        Object dynamicAdvisedInterceptor = h.get(proxy);
        Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        Object target = ((AdvisedSupport)advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();
        log.info("get target object [" + target.getClass().getName() + "] from Cglib reflect ,from proxy:" + proxy.getClass().getName());
        return target;
    }

    private static Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
        h.setAccessible(true);
        AopProxy aopProxy = (AopProxy)h.get(proxy);
        Field advised = aopProxy.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        Object target = ((AdvisedSupport)advised.get(aopProxy)).getTargetSource().getTarget();
        log.info("get target object [" + target.getClass().getName() + "] from Jdk reflect ,from proxy:" + proxy.getClass().getName());
        return target;
    }

    public static Method getObjectMethod(Object obj, Method method) {
        Method declaredMethod;
        try {
            Class<?> declaringType = obj.getClass();
            declaredMethod = declaringType.getDeclaredMethod(method.getName(), method.getParameterTypes());
        } catch (Exception e) {
            log.error("AnnotationUtils getObjectMethod error,method:" + method.getName(), e);
            throw new IllegalStateException(e);
        }
        return declaredMethod;
    }

    public static String generateFullName(Object object, Method method) {
        String className = AnnotationUtil.unwrapProxy(object).getClass().getName();
        String methodName = method.getName();
        return className + "." + methodName;
    }



}
