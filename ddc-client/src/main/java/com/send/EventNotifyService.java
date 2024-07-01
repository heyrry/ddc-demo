package com.send;

import com.notify.dto.EventDO;

/**
 * @author baofeng
 * @date 2023/06/06
 */
public interface EventNotifyService {
    /**
     * 事件通知
     * @param event
     */
    void eventNotify(EventDO event);
}
