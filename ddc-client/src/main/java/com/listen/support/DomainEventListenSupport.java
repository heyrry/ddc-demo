package com.listen.support;

import com.alibaba.fastjson.JSONObject;
import com.annotation.EventListen;
import com.listen.dao.EventListenDAO;
import com.listen.dao.impl.EventListenDAOImpl;
import com.listen.dto.EventListenHandlerDTO;
import com.listen.handler.EventListenHandler;
import com.listen.handler.EventListenHandlerImpl;
import com.listen.service.EventListenService;
import com.listen.service.impl.EventListenServiceImpl;
import com.send.DomainEventDaoFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import util.AnnotationUtil;
import util.ParamParserUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author baofeng
 * @date 2023/06/06
 */
@Slf4j
public class DomainEventListenSupport {
    private static EventListenService eventListenService;

    /**
     * 分隔符
     */
    private static final String SEPARATOR = "@";

    /**
     * name为key的监听者集合
     */
    private static Map<String, DomainEventListenBO> domainEventListenMap = new LinkedHashMap<String, DomainEventListenBO>();

    /**
     * domain和event与对应的Listener集合
     */
    private static Map<String, List<String>> domainEventUniqueKeyMap = new LinkedHashMap<String, List<String>>();

    public static void initDomainEventListenService(DataSource dataSource) {
        EventListenServiceImpl service = new EventListenServiceImpl();
        eventListenService = service;

        // 设置处理器
        EventListenHandlerImpl eventListenHandler = new EventListenHandlerImpl();
        service.setEventListenHandler(eventListenHandler);

        // 设置eventListenDAO
        EventListenDAOImpl eventListenDAO = DomainEventDaoFactory.createEventListenDAO(dataSource);
        service.setEventListenDAO(eventListenDAO);
    }

    public static EventListenService getDomainEventListenService() {
        return eventListenService;
    }

    public static boolean isAsynchronousExecute(String name) {
        EventListen domainEventListen = getDomainEventListen(name);
        if (domainEventListen == null) {
            return false;
        }
        return domainEventListen.asynchronousExecute();
    }

    private static EventListen getDomainEventListen(String name) {
        DomainEventListenBO domainEventListenBO = domainEventListenMap.get(name);
        if (domainEventListenBO == null) {
            return null;
        }
        return domainEventListenBO.eventListen;
    }

    public static Object execute(String name, JSONObject paramsJSON) throws Exception {
        DomainEventListenBO domainEventListenBO = domainEventListenMap.get(name);
        if (domainEventListenBO == null) {
            log.error("execute error, domainEventListenBO null, name:{}", name);
            throw new IllegalStateException("execute error");
        }
        Method method = domainEventListenBO.method;
        // 空指针异常问题定位
        if (method == null || domainEventListenBO.object == null) {
            log.error("execute error, method or object null, name:{}, domainEventListenBO:{}", name, domainEventListenBO);
            throw new IllegalStateException("execute error");
        }
        // 参数解析
        Object[] params = ParamParserUtils.parseMethodParam(method, paramsJSON);
        return method.invoke(domainEventListenBO.object, params);
    }

    public static boolean canExecute(String name, int retryTimes) {
        if (retryTimes <= 0) {
            return true;
        }
        DomainEventListenBO domainEventListenBO = domainEventListenMap.get(name);
        if (domainEventListenBO == null) {
            log.error("canExecute is false, domainEventListenBO is null, name:{}", name);
            return false;
        }
        int maxRetryTimes = domainEventListenBO.maxRetryTimes;
        if (maxRetryTimes <= -1) {
            return true;
        }
        if (maxRetryTimes == 0) {
            log.error("canExecute is false, maxRetryTimes=0, name:{}", name);
            return false;
        }
        return retryTimes <= maxRetryTimes;
    }

    public static void registerDomainEventListen(EventListen annotation, Method method, Object obj) {
        DomainEventListenBO domainEventListenBO = new DomainEventListenBO(method, obj, annotation);
        String name = domainEventListenBO.name;
        log.info("registerDomainEventListen begin, name:" + name);
        if (domainEventListenMap.containsKey(name)) {
            log.error("registerDomainEventListen error,name is exist,name:" + name);
            throw new IllegalStateException("registerDomainEventListen error,name is exist,name:" + name);
        }
        // domain必须有值
        if (ArrayUtils.isEmpty(annotation.listenDomain())) {
            log.error("registerDomainEventListen error,listenDomain is empty,name:" + name);
            throw new IllegalStateException("registerDomainEventListen error,listenDomain is empty,name:" + name);
        }
        // event必须有值
        if (ArrayUtils.isEmpty(annotation.listenEvent())) {
            log.error("registerDomainEventListen error,listenEvent is empty,name:" + name);
            throw new IllegalStateException("registerDomainEventListen error,listenEvent is empty,name:" + name);
        }

        // 同一个domain + event可能被多个Listener监听
        for (String domain : annotation.listenDomain()) {
            for (String event : annotation.listenEvent()) {
                String uniKey = buildDomainEventUniKey(domain, event);
                List<String> nameList = domainEventUniqueKeyMap.get(uniKey);
                if (nameList == null) {
                    nameList = new ArrayList<>();
                }
                nameList.add(name);
                domainEventUniqueKeyMap.put(uniKey, nameList);
            }
        }

        domainEventListenMap.put(name, domainEventListenBO);
        log.info("registerDomainEventListen end, name:" + name);
    }

    public static List<String> filterDomainEventListen(String domain, String event) {
        // TODO 暂未放开通配符事件发送匹配
        if (StringUtils.isAnyBlank(domain, event)) {
            log.error("filterDomainEventListen error, domain or event can not be null, domain:{}, event:{}", domain, event);
            return null;
        }
        String uniKey = buildDomainEventUniKey(domain, event);
        return domainEventUniqueKeyMap.get(uniKey);
    }

    public static String buildDomainEventUniKey (String domain, String event) {
        if (StringUtils.isAnyBlank(domain, event)) {
            throw new IllegalStateException("buildDomainEventUniKey error. domain or event can not be null");
        }
        return String.format("%s@%s", domain, event);
    }

    @Data
    public static class DomainEventListenBO {
        String name;
        String fullName;
        int maxRetryTimes;
        Method method;
        Object object;
        EventListen eventListen;

        public DomainEventListenBO(Method method, Object object, EventListen eventListen) {
            this.fullName = AnnotationUtil.generateFullName(object, method);
            this.name = eventListen.name();
            if (StringUtils.isEmpty(this.name)) {
                this.name = this.fullName;
            }
            this.method = method;
            this.object = object;
            this.eventListen = eventListen;
        }

    }

}
