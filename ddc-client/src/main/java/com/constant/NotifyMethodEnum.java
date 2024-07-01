package com.constant;

/**
 * 通知方式枚举
 *
 * @Author: wanglinhua
 * @Date: 2019-05-06
 */
public enum NotifyMethodEnum {

    local("本地通知"),
    remote("远程通知"),
    localAndRemote("本地与远程通知");

    private String desc;

    private NotifyMethodEnum(String desc) {
        this.desc = desc;
    }

    public static boolean isLocalNotify(String method) {
        return local.name().equals(method);
    }

}
