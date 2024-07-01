package com.send.constant;

/**
 * @author baofeng
 * @date 2023/06/06
 */
public enum EventStateEnum {
    pending("待处理"),
    completed("已完成"),
    failed("处理失败");

    private String desc;

    private EventStateEnum(String desc) {
        this.desc = desc;
    }

}
