package com.config.postProcessorAlarm;

import com.config.AbstractDomainEventPostProcessor;
import com.notify.dto.EventDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 执行失败事件短信提醒
 * @author baofeng
 * @date 2023/07/02
 */
@Slf4j
public class MessageAlarm extends AbstractDomainEventPostProcessor {

    @Override
    public void failPostProcessAfterNotify(EventDO event) {
        log.info("MessageAlarm failPostProcessAfterNotify begin, event:{}", event);
    }

}
