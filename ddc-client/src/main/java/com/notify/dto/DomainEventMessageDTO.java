package com.notify.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author baofeng
 * @date 2023/07/24
 */
@Data
public class DomainEventMessageDTO {
    /**
     * 实体ID
     */
    private String entityId;
    /**
     * 事件
     */
    private String event;
    /**
     * 内容
     */
    private Map<String, Object> content;
}
