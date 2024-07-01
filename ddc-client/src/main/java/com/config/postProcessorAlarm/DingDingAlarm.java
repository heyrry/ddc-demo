package com.config.postProcessorAlarm;

import com.config.AbstractDomainEventPostProcessor;
import com.notify.dto.EventDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 执行失败事件钉钉提醒
 * @author baofeng
 * @date 2023/07/02
 */
@Slf4j
public class DingDingAlarm extends AbstractDomainEventPostProcessor {

    @Override
    public void failPostProcessAfterNotify(EventDO event) {
        log.info("DingDingAlarm failPostProcessAfterNotify begin, event:{}", event);
    }

}
