package com.notify;

import lombok.Data;

/**
 * @author baofeng
 * @date 2023/06/05
 */
@Data
public class DomainEventNotifyDTO {
    /**
     * 领域
     */
    private String domain;

    /**
     * 事件
     */
    private String event;
}
