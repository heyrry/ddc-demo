package com.send;

import com.notify.impl.LocalNotifyServiceImpl;
import com.send.impl.EventNotifyServiceImpl;

import javax.sql.DataSource;

/**
 * @author baofeng
 * @date 2023/06/06
 */
public class EventNotifyServiceFactory {
    private static EventNotifyService eventNotifyService;

    public static void init(DataSource dataSource) {
        EventNotifyServiceImpl eventNotifyServiceImpl = new EventNotifyServiceImpl();
        eventNotifyServiceImpl.setDataSource(dataSource);
        // 本地notify
        eventNotifyServiceImpl.setLocalNotifyService(new LocalNotifyServiceImpl());
        // 远程notify
        EventNotifyServiceFactory.eventNotifyService = eventNotifyServiceImpl;
    }

    public static EventNotifyService getEventNotifyService() {
        return eventNotifyService;
    }

}
