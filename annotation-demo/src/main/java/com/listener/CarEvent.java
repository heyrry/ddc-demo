package com.listener;

import org.springframework.context.ApplicationEvent;

/**
 * @author baofeng
 * @date 2022/03/05
 */
public class CarEvent extends ApplicationEvent {

    /**
     * 事件的类型
     */
    private String type;

    public CarEvent(Object source, String type) {
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
