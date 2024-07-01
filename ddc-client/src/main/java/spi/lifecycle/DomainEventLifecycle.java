package spi.lifecycle;

import java.util.function.Supplier;

/**
 * @author baofeng
 * @date 2023/06/05
 */
public interface DomainEventLifecycle <T> {
    /**
     * 初始化
     */
    void init();

    /**
     * 切点开始
     * @param domainEvent
     */
    void start(T domainEvent);

    /**
     * 切点结束
     * @return void
     */
    void end();

    /**
     * 销毁
     */
    void destroy();
}
