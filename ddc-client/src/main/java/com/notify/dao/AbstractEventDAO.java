package com.notify.dao;

import com.notify.dto.EventDO;

import java.util.List;

/**
 * @author baofeng
 * @date 2023/07/12
 */
public abstract class AbstractEventDAO implements EventDAO {
    @Override
    public Long saveEvent(EventDO event) {
        throw new UnsupportedOperationException("saveEvent");
    }

    @Override
    public void updateEventNotify(EventDO event) {
        throw new UnsupportedOperationException("updateEventNotify");
    }

    @Override
    public EventDO getEvent(Long id) {
        throw new UnsupportedOperationException("getEvent");
    }

    @Override
    public List<EventDO> queryPendingEventList() {
        throw new UnsupportedOperationException("queryPendingEventList");
    }

}
