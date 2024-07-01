package com.listen.dto;

import com.alibaba.fastjson.JSONObject;
import com.listen.service.EventListenCallback;
import lombok.Data;

/**
 * @author baofeng
 * @date 2023/07/04
 */
@Data
public class EventListenHandlerDTO {
    private String name;
    /**
     * 回调，可异步执行
     */
    private EventListenCallback callback;
    /**
     * 重试次数
     */
    private int retryTimes;

    /**
     * notify参数
     */
    private JSONObject param;
}
