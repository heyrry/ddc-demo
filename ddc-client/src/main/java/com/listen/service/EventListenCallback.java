package com.listen.service;

/**
 * @author baofeng
 * @date 2023/07/04
 */
public interface EventListenCallback {
    /**
     * 处理成功回调
     */
    void onHandleAfterSuccess();

    /**
     * 处理失败回调
     * @param message
     */
    void onHandleAfterFail(String message);
}
