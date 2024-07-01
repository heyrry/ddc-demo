package com.listener;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.time.Clock;

/**
 * @author baofeng
 * @date 2022/03/05
 */
public class BusinessEvent extends ApplicationEvent {

    /**
     * 事件的类型
     */
    private String type;

    public BusinessEvent(Object source, String type) {
        super(source);
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
