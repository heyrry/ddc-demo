package com.listener;

import com.alibaba.fastjson.JSON;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;

/**
 * @author baofeng
 * @date 2022/03/05
 */
@Component
public class CommonPublisher implements ApplicationEventPublisherAware {

    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * 发布事件
     */
    public void publishBusinessEvent(BusinessEvent businessEvent) {
        System.out.println("发布事件:" + JSON.toJSONString(businessEvent));
        this.applicationEventPublisher.publishEvent(businessEvent);
    }

    /**
     * 发布事件
     */
    public void publishCarEvent(CarEvent carEvent) {
        System.out.println("发布事件:" + JSON.toJSONString(carEvent));
        this.applicationEventPublisher.publishEvent(carEvent);
    }

}
