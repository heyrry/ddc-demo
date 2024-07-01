package com.config;

import com.alibaba.fastjson.JSONObject;
import com.listen.dto.EventListenDO;
import com.notify.dto.EventDO;
import com.send.constant.EventStateEnum;
import lombok.extern.slf4j.Slf4j;
import util.Result;

import java.util.HashSet;
import java.util.Set;

/**
 * @author baofeng
 * @date 2023/07/02
 */
@Slf4j
public class AbstractDomainEventPostProcessor implements DomainEventPostProcessor{

    private static Set<Class<? extends DomainEventPostProcessor>> clazzMap = new HashSet<Class<? extends DomainEventPostProcessor>>();

    public AbstractDomainEventPostProcessor() {
        register(this);
    }

    private void register(DomainEventPostProcessor domainEventPostProcessor) {
        Class<? extends DomainEventPostProcessor> clazz = domainEventPostProcessor.getClass();
        synchronized (clazzMap) {
            if (!clazzMap.contains(clazz)) {
                DomainEventPostProcessorManager.register(domainEventPostProcessor);
                clazzMap.add(clazz);
                log.info("register DomainEventPostProcessor,class:" + clazz.getName());
            }
        }
    }

    @Override
    public void postProcessAfterNotify(EventDO event) {
        String state = event.getState();
        if (EventStateEnum.completed.name().equals(state)) {
            succPostProcessAfterNotify(event);
            return;
        }
        if (EventStateEnum.failed.name().equals(state)) {
            failPostProcessAfterNotify(event);
            return;
        }
        log.error("postProcessAfterNotify state is illegal,state:" + state + ",event:" + event);
    }

    @Override
    public void postProcessAfterListen(EventListenDO eventListenDO, String name, JSONObject params, Result<Void> result) {
        if (result.isSuccess()) {
            succPostProcessAfterListen(eventListenDO, name, params);
            return;
        }
        failPostProcessAfterListen(eventListenDO, name, params, result.getMessage());
    }

    protected void failPostProcessAfterNotify(EventDO event) {
    }

    protected void succPostProcessAfterNotify(EventDO event) {
    }

    /**
     * 监听失败后置处理
     *
     * @param eventListen
     * @param name
     * @param params
     * @param errMsg
     */
    protected void failPostProcessAfterListen(EventListenDO eventListen, String name, JSONObject params, String errMsg) {
    }

    /**
     * 监听成功后置处理
     *
     * @param eventListen
     * @param name
     * @param params
     */
    protected void succPostProcessAfterListen(EventListenDO eventListen, String name, JSONObject params) {
    }

}
