package com.listen.service;

import com.listen.dto.DomainEventListenDTO;
import com.listen.dto.EventListenDO;
import util.Result;

/**
 * @author baofeng
 * @date 2023/06/06
 */
public interface EventListenService<T> {

    /**
     * 保存监听事件
     * @param domainEventListenDTO
     * @return
     */
    Result<T> saveListenEvent(DomainEventListenDTO domainEventListenDTO);

    /**
     * 监听事件分发
     * @param eventListen
     * @return
     */
    Result<Void> dispatch(EventListenDO eventListen);
}
