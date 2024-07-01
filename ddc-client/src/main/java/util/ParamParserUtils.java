package util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.util.TypeUtils;
import com.annotation.EventParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 参数解析工具类
 *
 * @Author: wanglinhua
 * @Date: 2019-05-14
 */
public class ParamParserUtils {


    public static Object[] parseMethodParam(Method method, JSONObject paramsJSON) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        int parameterLength = parameterTypes.length;
        if (parameterLength == 0) {
            return new Object[0];
        }
        Object[] value = new Object[parameterLength];
        for (int i = 0; i < parameterLength; i++) {
            EventParam eventParam = AnnotationUtil.findAnnotation(parameterAnnotations[i], EventParam.class);
            if (eventParam == null) {
                continue;
            }
            Class<?> clazz = parameterTypes[i];
            String name = eventParam.name();
            if (paramsJSON.containsKey(name)) {
                value[i] = paramsJSON.getObject(name, clazz);
                continue;
            }
            //默认值
            value[i] = com.alibaba.fastjson2.util.TypeUtils.cast(eventParam.defaultValue(), clazz);
        }
        return value;
    }

    /**
     * 根据listener数量，获取初始值
     * @param listenerSize
     * @return
     */
    public static Long getInitListenResult (int listenerSize) {
        return Math.round(Math.pow(2, listenerSize)) - 1;
    }

}
