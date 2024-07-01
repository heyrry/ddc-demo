package com.notify;

import com.PointcutManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.enums.PointcutStateEnum;
import com.notify.dao.EventDAO;
import com.notify.dto.DomainEventMessageDTO;
import com.notify.dto.EventDO;
import com.runtime.DomainEventNotifyContext;
import com.send.DomainEventDaoFactory;
import com.send.constant.EventStateEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import spi.pointcut.PointcutInstance;
import util.StringUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author baofeng
 * @date 2023/06/06
 */
@Slf4j
public class DomainEventNotifyInstance implements PointcutInstance<EventDO> {

    protected DomainEventNotifyDTO domainEventNotify;
    protected String state;
    private static List<String> rolledBackStateList = Arrays.asList(PointcutStateEnum.checked.name(), PointcutStateEnum.created.name());

    /**
     * 参数
     */
    protected Map<String, Object> paramMap = new HashMap<String, Object>();

    /**
     * 事件
     */
    protected EventDO event;

    public DomainEventNotifyInstance(DomainEventNotifyDTO domainEventNotify) {
        this.domainEventNotify = domainEventNotify;
        // 设置参数 （有可能先进事务，再执行这里，故需要将DdcConnection中设置中的参数设置在pointcutInstance中）
        Map param = PointcutManager.getParam();
        // 如果先进入事务，则初始状态为启动状态
        if (param != null) {
            this.setParam(param);
            state = PointcutStateEnum.checked.name();
        } else {
            state = PointcutStateEnum.pending.name();
        }

    }

    @Override
    public void setParam(Map<String, Object> param) {
        if (param == null) {
            return;
        }
        paramMap.putAll(param);
    }

    @Override
    public void startEvent() {
        if (!PointcutStateEnum.pending.name().equals(state)) {
            log.info("DomainEventNotifyInstance startEvent ignore,state:" + state + ",domainEventNotify:" + domainEventNotify);
            return;
        }
        state = PointcutStateEnum.checked.name();
    }

    @Override
    public void createEvent() {
        if (!PointcutStateEnum.checked.name().equals(state)) {
            log.info("DomainEventNotifyInstance createEvent uncheck,state:" + state + ",domainEventNotify:" + domainEventNotify);
            return;
        }
        // 创建消息
        DomainEventMessageDTO messageDTO = createMessageDTO();
        if (messageDTO == null) {
            log.info("DomainEventNotifyInstance createEvent cancelled, domainEventNotify:{}", domainEventNotify);
            return;
        }
        // 构建事件
        EventDO eventDO = buildEventDO(messageDTO);

        // 保存事件
        Connection connection = (Connection)paramMap.get(CONNECTION);
        EventDAO eventDAO = DomainEventDaoFactory.createInsertEventDAO(connection);
        eventDAO.saveEvent(eventDO);

        this.event = eventDO;
        state = PointcutStateEnum.created.name();
        log.info("DomainEventNotifyInstance createEvent succ,event:" + event);
    }

    private EventDO buildEventDO(DomainEventMessageDTO messageDTO) {
        EventDO event = new EventDO();
        event.setDomain(domainEventNotify.getDomain());
        event.setEntityId(messageDTO.getEntityId());
        event.setEvent(messageDTO.getEvent());
        Date date = new Date();
        event.setGmtEvent(date);
        event.setGmtCreate(date);
        event.setGmtModified(date);
        event.setState(EventStateEnum.pending.name());
        event.setEventContext(JSON.toJSONString(messageDTO.getContent(), SerializerFeature.DisableCircularReferenceDetect));
        event.setRetryTimes(0);
        event.setVersion(1);
//        event.setLocalSend(domainEventNotify.getLocalSend());
//        event.setRemoteSend(domainEventNotify.getRemoteSend());
//        event.setAppName(appName);
//        event.setEnv(env);
        return event;
    }

    private DomainEventMessageDTO createMessageDTO() {
        DomainEventMessageDTO domainEventMessageDTO = new DomainEventMessageDTO();
        Map<String, Object> contextMap = DomainEventNotifyContext.getMap();
        //领域实体ID,以消息内容为准

        String entityId = StringUtil.getString(contextMap.get(DomainEventNotifyContext.ENTITY_ID));
        if (StringUtils.isEmpty(entityId)) {
            log.info("DefaultMessageHandler createMessageDTO entityId is null,content:{}" + contextMap);
            return null;
        }
        // 以消息为准
        String event = (String)contextMap.get(DomainEventNotifyContext.EVENT);
        if (event == null) {
            event = domainEventNotify.getEvent();
        }
        domainEventMessageDTO.setEntityId(entityId);
        domainEventMessageDTO.setEvent(event);
        domainEventMessageDTO.setContent(contextMap);
        return domainEventMessageDTO;
    }

    @Override
    public void onSubmitted() {
        if (!PointcutStateEnum.created.name().equals(state)) {
            log.warn("DomainEventNotifyInstance onSubmitted fail,state:" + state + ",domainEventNotify:" + domainEventNotify);
            return;
        }
        state = PointcutStateEnum.submitted.name();
        log.info("DomainEventNotifyInstance onSubmitted succ,event:" + event);
    }

    @Override
    public void onRolledBack() {
        if (!rolledBackStateList.contains(state)) {
            log.warn("DomainEventNotifyInstance onRolledBack fail,state:" + state + ",domainEventNotify:" + domainEventNotify);
            return;
        }
        state = PointcutStateEnum.rolledBack.name();
    }

    @Override
    public EventDO finishEvent() {
        if (PointcutStateEnum.submitted.name().equals(state)) {
            log.info("DomainEventNotifyInstance finishEvent succ,domainEventNotify:" + domainEventNotify);
            return event;
        }
        log.info("DomainEventNotifyInstance finishEvent fail,state:" + state + ",domainEventNotify:" + domainEventNotify);
        if (!PointcutStateEnum.rolledBack.name().equals(state)) {
            state = PointcutStateEnum.cancelled.name();
        }
        return null;
    }
}
