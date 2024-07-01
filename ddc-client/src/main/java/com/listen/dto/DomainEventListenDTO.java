package com.listen.dto;

import lombok.Data;

import java.util.Date;

/**
 * @author baofeng
 * @date 2023/06/06
 */
@Data
public class DomainEventListenDTO {
    private Long eventId;

    /**
     * traceId
     */
    private String traceId;

    /**
     * 事件发生时间
     */
    private Date eventTime;

    /**
     * 领域
     */
    private String domain;

    /**
     * 事件
     */
    private String event;

    /**
     * 事件上下文
     */
    private String eventContext;
}
