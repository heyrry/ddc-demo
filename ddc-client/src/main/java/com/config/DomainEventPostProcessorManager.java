package com.config;

import com.alibaba.fastjson.JSONObject;
import com.listen.dto.EventListenDO;
import com.notify.dto.EventDO;
import lombok.extern.slf4j.Slf4j;
import util.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * @author baofeng
 * @date 2023/07/02
 */
@Slf4j
public class DomainEventPostProcessorManager {
    private static List<DomainEventPostProcessor> domainEventPostProcessorList = new ArrayList<DomainEventPostProcessor>();

    public static void postProcessAfterNotify(EventDO event) {
        for (DomainEventPostProcessor postProcessor : domainEventPostProcessorList) {
            postProcessor.postProcessAfterNotify(event);
        }
    }

    public static void postProcessAfterListen(EventListenDO eventListenDO, String name, JSONObject params, Result<Void> result) {
        for (DomainEventPostProcessor postProcessor : domainEventPostProcessorList) {
            postProcessor.postProcessAfterListen(eventListenDO, name, params, result);
        }
    }

    public static void register(DomainEventPostProcessor domainEventPostProcessor) {
        if (domainEventPostProcessor == null) {
            return;
        }
        synchronized (domainEventPostProcessorList) {
            domainEventPostProcessorList.add(domainEventPostProcessor);
        }
    }

}
