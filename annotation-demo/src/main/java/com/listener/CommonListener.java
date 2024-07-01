package com.listener;

import com.alibaba.fastjson.JSON;
import com.apple.eawt.AppEventListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author baofeng
 * @date 2022/03/05
 */
@Component
public class CommonListener implements ApplicationListener<ApplicationEvent> {
    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        System.out.println("监听到事件：" + JSON.toJSONString(event));
        if (event instanceof BusinessEvent) {
            System.out.println("监听到BusinessEvent事件：" + JSON.toJSONString((BusinessEvent)event));
        }
        if (event instanceof CarEvent) {
            System.out.println("监听到CarEvent事件：" + JSON.toJSONString((CarEvent)event));
        }
    }
}
