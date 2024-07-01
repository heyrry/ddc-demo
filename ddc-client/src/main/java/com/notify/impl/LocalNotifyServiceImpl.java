package com.notify.impl;

import com.constant.ErrorCode;
import com.listen.dto.DomainEventListenDTO;
import com.listen.dto.EventListenDO;
import com.listen.support.DomainEventListenSupport;
import com.notify.dto.EventDO;
import com.send.NotifyService;
import com.send.dto.NotifyDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import util.Result;

import java.util.UUID;

/**
 * @author baofeng
 * @date 2023/06/06
 */
@Slf4j
public class LocalNotifyServiceImpl implements NotifyService {

    @Override
    public Result<String> send(NotifyDTO notifyDTO) {
        // 串联event_notify和event_listen
        String traceId = notifyDTO.getTraceId();
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        DomainEventListenDTO domainEventListenDTO = buildDomainEventListenDTO(notifyDTO);
        Result<Void> result = this.handle(domainEventListenDTO);
        if (result.isSuccess()) {
            return Result.success(traceId);
        }
        return Result.fail(result);
    }

    private Result<Void> handle(DomainEventListenDTO domainEventListenDTO) {
        if (domainEventListenDTO == null) {
            return Result.fail();
        }
        try {
            //保存
            Result<EventListenDO> result = DomainEventListenSupport.getDomainEventListenService().saveListenEvent(domainEventListenDTO);
            //判重
            if (!result.isSuccess() && ErrorCode.REPEAT_RECV.equals(result.getCode())) {
                log.info("AbstractMessageListener handle ignore by repeat, domainEventListenDTO:" + domainEventListenDTO + ",result:" + result);
                return Result.success();
            }
            //接收异常，重试
            if (!result.isSuccess() || result.getData() == null) {
                log.info("AbstractMessageListener handle fail, domainEventListen:" + domainEventListenDTO + ",result:" + result);
                return Result.fail(result.getCode(), result.getMessage());
            }
            log.info("saveListenEvent success, begin to dispatch, domainEventListenDTO:{}", domainEventListenDTO);
            //分发
            EventListenDO eventListen = result.getData();
            Result<Void> dispatchResult = DomainEventListenSupport.getDomainEventListenService().dispatch(eventListen);
            log.info("dispatch success, eventListen:{}, dispatchResult:{}", eventListen, dispatchResult);
            return dispatchResult;
        } catch (Exception e) {
            log.error("AbstractMessageListener handle error, domainEventListenDTO:" + domainEventListenDTO, e);
            return Result.fail(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }

    private DomainEventListenDTO buildDomainEventListenDTO(NotifyDTO notifyDTO) {
        DomainEventListenDTO dto = new DomainEventListenDTO();
        dto.setEventId(notifyDTO.getId());
        dto.setDomain(notifyDTO.getDomain());
        dto.setEvent(notifyDTO.getEvent());
        dto.setEventContext(notifyDTO.getEventContext());

        return dto;
    }


}
