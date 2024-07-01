package com.runtime;

import com.send.NotifyService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author baofeng
 * @date 2023/07/10
 */
public class DomainEventNotifyContext {

    public static final String ENTITY_ID = NotifyService.ENTITY_ID;
    public static final String EVENT = NotifyService.EVENT;
    public static final String DOMAIN = NotifyService.DOMAIN;

    private static ThreadLocal<Map<String, Object>> context = new ThreadLocal<Map<String, Object>>();

    private DomainEventNotifyContext() {
    }

    public static void put(String key, Object value) {
        Map<String, Object> map = context.get();
        if (map == null) {
            map = new HashMap<String, Object>();
            context.set(map);
        }
        map.put(key, value);
    }

    public static Object get(String key) {
        Map<String, Object> map = context.get();
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

    public static Map<String, Object> getMap() {
        Map<String, Object> map = context.get();
        if (map == null) {
            return null;
        }
        return Collections.unmodifiableMap(map);
    }

    public static void clear() {
        context.remove();
    }

}
