package com;

import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import spi.pointcut.PointcutInstance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author baofeng
 * @date 2023/02/22
 */
public class PointcutManager {

    private PointcutManager() {
    }

//    public static String DOMAIN_EVENT_NOTIFY = "domainEventNotify";
    /**
     * 连接
     */
//    public static String CONNECTION = "com.alibaba.ddc.spi.pointcut.connection";

//    private static ThreadLocal<Map<String, PointcutInstance>> context = new ThreadLocal<Map<String, PointcutInstance>>();
    private static ThreadLocal<PointcutInstance> context = new ThreadLocal<PointcutInstance>();
    private static ThreadLocal<Map<String, Object>> param = new ThreadLocal<Map<String, Object>>();

    public static void setParam(String key, Object value) {
        Map<String, Object> map = param.get();
        if (map == null) {
            map = new LinkedHashMap<>();
            param.set(map);
        }
        map.put(key, value);
    }

    public static Object getParam(String key) {
        Map<String, Object> map = param.get();
        return map.get(key);
    }

    public static Map<String, Object> getParam() {
        Map<String, Object> map = param.get();
        return map;
    }

    public static <T> PointcutInstance<T> getPointcutInstance() {
        return context.get();
    }

    public static void setPointcutInstance(PointcutInstance pointcutInstance) {
        context.set(pointcutInstance);
    }

    public static <T> PointcutInstance<T> removePointcutInstance() {
        PointcutInstance pointcutInstance = context.get();
        context.remove();
        return pointcutInstance;
    }

    public static void clear() {
        param.remove();
        context.remove();
    }

}
