package com.config.postProcessorAlarm;

import com.config.AbstractDomainEventPostProcessor;
import com.notify.dto.EventDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 执行失败事件日志提醒
 * @author baofeng
 * @date 2023/07/02
 */
@Slf4j
public class AppLogAlarm extends AbstractDomainEventPostProcessor {

    @Override
    public void failPostProcessAfterNotify(EventDO event) {
        log.info("AppLogAlarm failPostProcessAfterNotify begin, event:{}", event);
    }

}
