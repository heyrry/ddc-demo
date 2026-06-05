package com.compensate;

/**
 * 补偿服务入口。
 *
 * 框架不内置调度器，由业务方从外部调度系统触发（XXL-Job Handler、
 * ElasticJob、Spring @Scheduled + 分布式锁 等），调用以下两个方法。
 *
 * 典型用法：
 * <pre>
 *   // XXL-Job Handler 示例
 *   {@literal @}XxlJob("ddcCompensateNotify")
 *   public ReturnT<String> compensateNotify(String param) {
 *       DomainEventApplicationContext.getInstance()
 *           .getDdcCompensateService()
 *           .compensateNotify();
 *       return ReturnT.SUCCESS;
 *   }
 * </pre>
 *
 * @author baofeng
 */
public interface DdcCompensateService {

    /**
     * 补偿 notify 侧：扫描 ddc_event 中 state=pending/failed 的超时记录并重新投递。
     */
    void compensateNotify();

    /**
     * 补偿 listen 侧：扫描 ddc_event_listen 中 listen_result > 0 的超时记录并重新分发。
     */
    void compensateListen();
}
