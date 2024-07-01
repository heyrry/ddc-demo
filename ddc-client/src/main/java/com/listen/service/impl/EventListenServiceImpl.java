package com.listen.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.config.DomainEventPostProcessorManager;
import com.constant.ErrorCode;
import com.listen.dao.EventListenDAO;
import com.listen.dto.DomainEventListenDTO;
import com.listen.dto.EventListenDO;
import com.listen.dto.EventListenHandlerDTO;
import com.listen.handler.EventListenHandler;
import com.listen.service.EventListenCallback;
import com.listen.service.EventListenService;
import com.listen.support.DomainEventListenSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.util.CollectionUtils;
import util.ParamParserUtils;
import util.Result;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author baofeng
 * @date 2023/06/06
 */
@Slf4j
public class EventListenServiceImpl implements EventListenService {

    private EventListenHandler eventListenHandler;
    private EventListenDAO eventListenDAO;

    @Override
    public Result saveListenEvent(DomainEventListenDTO domainEventListenDTO) {
        // 判断是否重复
        EventListenDO eventListenDO = eventListenDAO.queryEventListen(domainEventListenDTO.getEventId());
        if (eventListenDO != null) {
            log.info("DomainEventListenServiceImpl listen repeat,domainEventListen:" + domainEventListenDTO + ",result:" + eventListenDO);
            return Result.fail(ErrorCode.REPEAT_RECV, "eventListenDO is exist,eventId:" + domainEventListenDTO.getEventId());
        }
        eventListenDO = buildEventListenDO(domainEventListenDTO);
        if (eventListenDO == null) {
            return Result.fail(ErrorCode.UNREGISTER_EVENT, "event unregister, domain:" + domainEventListenDTO.getDomain() + ", event:" + domainEventListenDTO.getEvent());
        }
        try {
            eventListenDAO.saveEventListen(eventListenDO);
            return Result.success(eventListenDO);
        } catch (Exception e) {
            log.error("save eventListen error", e);
            return Result.failByMessage(e.getMessage());
        }
    }

    private EventListenDO buildEventListenDO(DomainEventListenDTO dto) {
        EventListenDO eventListenDO = new EventListenDO();
        eventListenDO.setGmtCreate(new Date());
        eventListenDO.setGmtModified(new Date());
        eventListenDO.setEventId(dto.getEventId());
        eventListenDO.setDomain(dto.getDomain());
        eventListenDO.setEvent(dto.getEvent());
        eventListenDO.setListenContent(dto.getEventContext());
        eventListenDO.setTraceId(dto.getTraceId());

        List<String> listenerNameList = DomainEventListenSupport.filterDomainEventListen(dto.getDomain(), dto.getEvent());
        if (CollectionUtils.isEmpty(listenerNameList)) {
            log.info("no listener register, domain:{}, event:{}", dto.getDomain(), dto.getEvent());
            return null;
        }
        eventListenDO.setListenNames(JSON.toJSONString(listenerNameList));

        eventListenDO.setListenResult(ParamParserUtils.getInitListenResult(listenerNameList.size()));
        eventListenDO.setRetryTimes(0);
        return eventListenDO;
    }

    @Override
    public Result<Void> dispatch(EventListenDO eventListen) {
        if (eventListen == null) {
            return Result.fail(ErrorCode.PARAM_ERROR, "EventListenDO is null");
        }
        Long listenResult = eventListen.getListenResult();
        if (listenResult == null || listenResult < 0) {
            log.warn("DomainEventListenServiceImpl dispatch listenResult illegal,eventListen:" + eventListen);
            return Result.fail(ErrorCode.PARAM_ERROR, "eventListen is illegal,eventListen:" + eventListen);
        }
        // 处理完毕的，不再重复处理
        if (listenResult == 0) {
            log.warn("DomainEventListenServiceImpl dispatch listenResult is zero,eventListen:" + eventListen);
            return Result.success();
        }
        String listenNames = eventListen.getListenNames();
        if (StringUtils.isEmpty(listenNames)) {
            log.warn("DomainEventListenServiceImpl dispatch listenNames illegal,eventListen:" + eventListen);
            return Result.fail(ErrorCode.PARAM_ERROR, "listenNames is null");
        }
        try {
            List<String> listenNameList = JSON.parseArray(listenNames, String.class);
            for (int i = 0; i < listenNameList.size(); i++) {
                String name = listenNameList.get(i);
                //已处理的不再重复派发
                if (isDispatched(i, listenResult)) {
                    log.info("DomainEventListenServiceImpl dispatch repeat,name:" + name + ",eventListen:" + eventListen);
                    continue;
                }

                EventListenHandlerDTO dto = buildEventListenHandlerDTO(eventListen, name, i);
                eventListenHandler.handle(dto);
            }
            log.info("DomainEventListenServiceImpl dispatch succ,eventListen:{}", eventListen);
            return Result.success();
        } catch (Exception e) {
            log.error("DomainEventListenServiceImpl dispatch fail,eventListen:{}", eventListen, e);
            return Result.fail();
        }
    }

    private EventListenHandlerDTO buildEventListenHandlerDTO(EventListenDO eventListen, String name, int index) {
        EventListenHandlerDTO eventListenHandlerDTO = new EventListenHandlerDTO();
        JSONObject param = JSONObject.parseObject(eventListen.getListenContent());
        eventListenHandlerDTO.setParam(param);
        eventListenHandlerDTO.setName(name);
        eventListenHandlerDTO.setCallback(new EventListenCallbackImpl(eventListen, name, param, index));
        eventListenHandlerDTO.setRetryTimes(eventListen.getRetryTimes());
        return eventListenHandlerDTO;
    }

    private boolean isDispatched(int index, Long listenResult) {
        long weight = getWeight(index);
        return listenResult % (weight * 2) < weight;
    }

    private long getWeight(int index) {
        return Math.round(Math.pow(2, index));
    }

    public void setEventListenHandler(EventListenHandler eventListenHandler) {
        this.eventListenHandler = eventListenHandler;
    }

    public void setEventListenDAO(EventListenDAO eventListenDAO) {
        this.eventListenDAO = eventListenDAO;
    }

    public class EventListenCallbackImpl implements EventListenCallback {
        EventListenDO eventListen;
        String name;
        JSONObject params;
        int index;

        public EventListenCallbackImpl(EventListenDO eventListen, String name, JSONObject params, int index) {
            this.eventListen = eventListen;
            this.name = name;
            this.params = params;
            this.index = index;
        }

        @Override
        public void onHandleAfterSuccess() {
            // 更新Listener执行结果
            updateEventListenResult(eventListen.getId(), index);
            // 后置处理器
            Result<Void> result = Result.success();
            DomainEventPostProcessorManager.postProcessAfterListen(eventListen, name, params, result);
        }

        @Override
        public void onHandleAfterFail(String message) {
            // 更新Listener执行结果
            updateErrorInfo(eventListen.getId(), message);
            // 后置处理器
            Result<Void> result = Result.failByMessage(message);
            DomainEventPostProcessorManager.postProcessAfterListen(eventListen, name, params, result);
        }

        private void updateEventListenResult(Long id, int index) {
            try {
                boolean result = eventListenDAO.updateEventListenResult(id, index);
                log.info("DomainEventListenServiceImpl updateEventListenResult finish,id:" + id + ",index:" + index + ",result:" + result);
            } catch (Exception e) {
                log.error("DomainEventListenServiceImpl updateEventListenResult error,id:" + id + ",index:" + index, e);
            }
        }

        private void updateErrorInfo(Long id, String message) {
            try {
                boolean result = eventListenDAO.updateErrorInfo(id, message);
                log.info("DomainEventListenServiceImpl updateEventListenResult finish,id:" + id + ",index:" + index + ",result:" + result);
            } catch (Exception e) {
                log.error("DomainEventListenServiceImpl updateEventListenResult error,id:" + id + ",index:" + index, e);
            }
        }

    }

}
