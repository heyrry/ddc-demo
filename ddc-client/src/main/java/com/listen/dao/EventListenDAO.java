package com.listen.dao;

import com.listen.dto.EventListenDO;

/**
 * @author baofeng
 * @date 2023/07/04
 */
public interface EventListenDAO {
    /**
     * 查询事件监听
     *
     * @return
     */
    EventListenDO queryEventListen(Long eventId);

    /**
     * 保存事件监听
     *
     * @return
     */
    Long saveEventListen(EventListenDO eventListen);

    /**
     * 更新事件监听结果
     *
     * @param id
     * @param index 监听器所在监听列表的下标
     * @return
     */
    boolean updateEventListenResult(Long id, int index);

    /**
     * 更新错误信息
     *
     * @param id
     * @param errorInfo
     * @return
     */
    boolean updateErrorInfo(Long id, String errorInfo);

}
