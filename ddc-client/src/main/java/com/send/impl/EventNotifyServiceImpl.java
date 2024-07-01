package com.send.impl;

import com.alibaba.fastjson.JSON;
import com.config.DomainEventPostProcessorManager;
import com.constant.NotifyMethodEnum;
import com.notify.dao.EventDAO;
import com.notify.dto.EventDO;
import com.send.DomainEventDaoFactory;
import com.send.EventNotifyService;
import com.send.NotifyService;
import com.send.constant.EventStateEnum;
import com.send.dto.NotifyDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import util.Result;

import javax.sql.DataSource;
import java.util.Date;

/**
 * @author baofeng
 * @date 2023/06/06
 */
@Slf4j
public class EventNotifyServiceImpl implements EventNotifyService {

    private NotifyService remoteNotifyService;
    private NotifyService localNotifyService;
    private DataSource dataSource;

    @Override
    public void eventNotify(EventDO event) {
        if (event == null) {
            return;
        }
        if (EventStateEnum.completed.name().equals(event.getState())) {
            log.warn("EventNotifyServiceImpl send fail,state is completed,id:" + event.getId() + ",entityId:" + event.getEntityId());
            return;
        }
        // 发送通知
        Result<String> result = sendNotify(event);
        // 更新notify执行结果
        updateEventResult(event, result);
        //后置处理
        DomainEventPostProcessorManager.postProcessAfterNotify(event);
    }

    private void updateEventResult(EventDO event, Result<String> result) {
        if (dataSource == null) {
            log.error("dataSource null, return");
            return;
        }
        event.setGmtNotify(new Date());
        event.setNotifyResult(JSON.toJSONString(result));
        if (result.isSuccess()) {
            event.setState(EventStateEnum.completed.name());
            event.setNotifyId(result.getData());
        } else {
            event.setState(EventStateEnum.failed.name());
        }
        if (event.getVersion() == null) {
            event.setVersion(1);
        }

        EventDAO eventDAO = DomainEventDaoFactory.createEventDAO(dataSource);
        eventDAO.updateEventNotify(event);
    }

    private Result<String> sendNotify(EventDO event) {
        //构建信息
        NotifyDTO notifyDTO = buildNotifyDTO(event);
        Result<String> result = Result.fail();
        // TODO 暂时只支持本地调用
        /*if (!NotifyMethodEnum.isLocalNotify(event.getNotifyType())) {
            log.error("unsupported notify type");
            return result;
        }*/
        return localSendNotify(notifyDTO);
    }

    private Result<String> localSendNotify(NotifyDTO notifyDTO) {
        if (localNotifyService == null) {
            throw new IllegalStateException("localNotifyService is null");
        }
        return localNotifyService.send(notifyDTO);
    }

    private NotifyDTO buildNotifyDTO(EventDO event) {
        NotifyDTO notifyDTO = new NotifyDTO();
        notifyDTO.setId(event.getId());
        notifyDTO.setEntityId(event.getEntityId());
        notifyDTO.setDomain(event.getDomain());
        notifyDTO.setEvent(event.getEvent());
        notifyDTO.setEventTime(event.getGmtEvent());
        notifyDTO.setTraceId(event.getNotifyId());
        notifyDTO.setEventContext(event.getEventContext());
        return notifyDTO;
    }

    public NotifyService getRemoteNotifyService() {
        return remoteNotifyService;
    }

    public void setRemoteNotifyService(NotifyService remoteNotifyService) {
        this.remoteNotifyService = remoteNotifyService;
    }

    public NotifyService getLocalNotifyService() {
        return localNotifyService;
    }

    public void setLocalNotifyService(NotifyService localNotifyService) {
        this.localNotifyService = localNotifyService;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

}
