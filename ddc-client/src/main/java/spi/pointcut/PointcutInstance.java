package spi.pointcut;

import java.util.Map;

/**
 * @author baofeng
 * @date 2023/06/06
 */
public interface PointcutInstance<T> {

    /**
     * 连接
     */
    String CONNECTION = "com.alibaba.ddc.spi.pointcut.connection";

    /**
     * 参数设置
     * @param param
     */
    void setParam(Map<String, Object> param);

    /**
     * 开始事件
     *
     * @return
     */
    void startEvent();

    /**
     * 创建事件
     */
    void createEvent();

    /**
     * 已提交
     */
    void onSubmitted();

    /**
     * 已回滚
     */
    void onRolledBack();

    /**
     * 完成事件
     *
     * @return
     */
    T finishEvent();
}
