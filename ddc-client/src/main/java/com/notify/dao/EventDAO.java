package com.notify.dao;

import com.notify.dto.EventDO;

import java.util.List;

/**
 * @author baofeng
 * @date 2023/07/04
 */
public interface EventDAO {

    /**
     * 保存事件
     * @param event
     * @return
     */
    Long saveEvent(EventDO event);

    /**
     * 更新notify表
     * @param event
     * @return
     */
    void updateEventNotify(EventDO event);

    /**
     * 查询事件
     * @param id
     * @return
     */
    EventDO getEvent(Long id);

    /**
     * 查询未处理/处理失败事件
     * @return
     */
    List<EventDO> queryPendingEventList();
}
