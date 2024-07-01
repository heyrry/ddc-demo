package com.config;

import com.alibaba.fastjson.JSONObject;
import com.listen.dto.EventListenDO;
import com.notify.dto.EventDO;
import util.Result;

/**
 * 领域事件Post处理器
 * @author baofeng
 * @date 2023/07/02
 */
public interface DomainEventPostProcessor {
    /**
     * 事件通知后置处理
     *
     * @param event
     */
    void postProcessAfterNotify(EventDO event);


    /**
     * 事件监听后置处理器
     * @param eventListenDO
     */
    void postProcessAfterListen(EventListenDO eventListenDO, String name, JSONObject params, Result<Void> result);
}
