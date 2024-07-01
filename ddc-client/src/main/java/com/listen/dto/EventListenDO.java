package com.listen.dto;

import lombok.Data;

import java.util.Date;

/**
 * @author baofeng
 * @date 2023/06/06
 */
@Data
public class EventListenDO {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    /**
     * 事件ID
     */
    private Long eventId;

    private String domain;

    private String event;

    private String listenContent;

    /**
     * 消息ID
     */
    private String traceId;

    /**
     * 执行方法
     */
    private String listenNames;

    /**
     * 执行结果。0-成功；其他-失败
     */
    private Long listenResult;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 错误信息
     */
    private String errorInfo;
}
