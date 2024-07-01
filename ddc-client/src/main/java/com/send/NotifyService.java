package com.send;

import com.notify.dto.EventDO;
import com.send.dto.NotifyDTO;
import util.Result;

/**
 * @author baofeng
 * @date 2023/06/06
 */
public interface NotifyService {

    /**
     * 消息体Key定义
     */
    String DOMAIN = "domain";
    String ENTITY_ID = "entityId";
    String EVENT = "event";
    String GMT_EVENT = "gmtEvent";
    String EVENT_ID = "eventId";

    /**
     * 事件通知
     * @param notifyDTO
     * @return
     */
    Result<String> send(NotifyDTO notifyDTO);
}
