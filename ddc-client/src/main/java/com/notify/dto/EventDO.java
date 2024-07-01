package com.notify.dto;

import lombok.Data;

import java.util.Date;

/**
 * @author baofeng
 * @date 2023/06/06
 */
@Data
public class EventDO {

    private Long id;

    private Date gmtCreate;
    private Date gmtModified;
    /**
     * 事件发生时间
     */
    private Date gmtEvent;

    /**
     * 实体ID
     */
    private String entityId;

    /**
     * 领域
     */
    private String domain;

    /**
     * 事件
     */
    private String event;

    /**
     * 状态
     */
    private String state;

    /**
     * 通知类型
     */
    private String notifyType;

    /**
     * 通知时间
     */
    private Date gmtNotify;

    /**
     * 通知结果
     */
    private String notifyResult;

    /**
     * 通知ID
     */
    private String notifyId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 事件上下文
     */
    private String eventContext;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 本地发送
     */
    private Boolean localSend;
    /**
     * 远程发送
     */
    private Boolean remoteSend;


}
