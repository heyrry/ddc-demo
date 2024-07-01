package com.listen.handler;

import com.listen.dto.EventListenHandlerDTO;
import com.listen.service.EventListenCallback;
import com.listen.support.DomainEventListenSupport;
import com.send.EventNotifyServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import util.DdcTaskExecutor;
import util.Result;

/**
 * @author baofeng
 * @date 2023/07/04
 */
@Slf4j
public class EventListenHandlerImpl implements EventListenHandler {
    @Override
    public void handle(EventListenHandlerDTO dto) {
        if (dto == null) {
            return;
        }
        String name = dto.getName();
        //异步执行
        if (DomainEventListenSupport.isAsynchronousExecute(name)) {
            DdcTaskExecutor.execute(() -> {
                //同步执行
                handle0(dto);
            });
            return;
        }
        //同步执行
        handle0(dto);
    }

    private void handle0(EventListenHandlerDTO dto) {
        String name = dto.getName();
        int retryTimes = dto.getRetryTimes();
        //判断是否能执行
        boolean needHandle = DomainEventListenSupport.canExecute(name, retryTimes);
        if (!needHandle) {
            log.info("handle ignore,dto:" + dto);
            return;
        }
        EventListenCallback callback = dto.getCallback();
        try {
            DomainEventListenSupport.execute(name, dto.getParam());
            callback.onHandleAfterSuccess();
        } catch (Exception e) {
            log.error("EventListenHandlerImpl handle0 execute error,dto:{}", dto, e);
            callback.onHandleAfterFail(e.getMessage());
        }

    }

}
