# ddc-client Codex 分析与优化建议

> 版本：v1.0 | 日期：2026-06-05 | 输出方：Codex

---

## 一、结论先行

`ddc-client` 的方向是正确的：它试图用 **Transactional Outbox（本地消息表）** 解决“业务数据提交”和“领域事件投递”之间的一致性问题。当前实现已经具备 AOP 标记、同连接写消息表、事务提交后分发、监听器注册与本地消费等核心骨架。

但从工程成熟度看，它更接近一个可验证思路的 Demo，而不是可直接线上复用的中间件。主要短板集中在：

| 类别 | 结论 |
|---|---|
| 数据正确性 | 存在明确代码 Bug，会写错字段或导致普通事务 NPE |
| 可靠性 | 缺少补偿扫描和完整重试闭环，服务重启可能造成消息永久滞留 |
| 接入方式 | 强依赖替换 `PlatformTransactionManager`，对现有系统侵入较强 |
| 稳定性 | 线程池、监听状态、幂等约束、错误信息长度等缺少生产级保护 |
| 工程化 | 缺少 DDL、自动配置、测试和可运行的 Maven Wrapper |

建议先修复 P0 数据正确性问题，再补齐可靠性闭环，最后再做事务感知方式和监听模型重构。

---

## 二、设计概述

### 2.1 核心模式

ddc-client 采用本地消息表模式：

1. 业务方法通过 `@EventNotify` 声明会产生领域事件。
2. 业务代码通过 `DomainEventNotifyContext.put()` 写入事件上下文，例如 `entityId`。
3. 在事务提交前，用业务事务同一个 JDBC `Connection` 写入 `ddc_event`。
4. 业务事务提交成功后，再异步执行本地通知和监听分发。
5. 监听端通过 `@EventListen` 注册处理方法，执行结果写入 `ddc_event_listen`。

这个设计的核心价值是：**只要业务事务提交成功，事件记录也应该已经在数据库中提交成功**，不需要 XA 或分布式事务。

### 2.2 当前调用流程

```text
业务方法 @EventNotify
    |
    |-- EventNotifyAspect.doAround()
    |      创建 DomainEventNotifyDTO
    |      lifecycle.start()
    |      PointcutManager 保存 PointcutInstance
    |
    |-- 业务逻辑执行
    |      DomainEventNotifyContext.put(entityId, ...)
    |
    |-- MyDataSourceTransactionManager.doBegin()
    |      获取当前事务 Connection
    |      注入 PointcutInstance
    |      PointcutInstance 状态 pending -> checked
    |
    |-- MyDataSourceTransactionManager.doCommit()
    |      createEvent()
    |      InsertEventDAOImpl 复用同一个 Connection 写入 ddc_event
    |      super.doCommit()
    |      PointcutInstance 状态 created -> submitted
    |
    |-- TransactionSynchronization.afterCompletion()
           lifecycle.end()
           finishEvent()
           DdcTaskExecutor 异步执行 notify
           LocalNotifyServiceImpl 写入 ddc_event_listen
           EventListenServiceImpl.dispatch()
           EventListenHandlerImpl 调用 @EventListen 方法
```

### 2.3 接入方式

使用方需要导入客户端配置：

```java
@ComponentScan(basePackages = {"beans"})
@EnableAspectJAutoProxy
@Import(ClientConfiguration.class)
public class TestConfiguration {
}
```

业务侧声明事件：

```java
@EventNotify(domain = "testDomain", event = "update")
@Transactional(rollbackFor = Exception.class)
public void update() {
    // 业务数据库操作
    DomainEventNotifyContext.put(DomainEventNotifyContext.ENTITY_ID, 1);
    DomainEventNotifyContext.put("a", "aaa");
}
```

监听侧声明消费逻辑：

```java
@EventListen(listenDomain = {"testDomain"}, listenEvent = {"update"})
public void listen1(@EventParam(name = DomainEventNotifyContext.ENTITY_ID) String billNo) {
    System.out.println("listen1 exec, billNo:" + billNo);
}
```

### 2.4 关键组件

| 组件 | 职责 |
|---|---|
| `EventNotifyAspect` | 拦截 `@EventNotify` 方法，创建事件生命周期 |
| `DomainEventNotifyContext` | 用 `ThreadLocal` 保存事件上下文 |
| `MyDataSourceTransactionManager` | 在事务提交前插入事件表，并在提交后标记状态 |
| `DomainEventNotifyInstance` | 维护切点状态机并构建 `EventDO` |
| `InsertEventDAOImpl` | 使用业务事务 `Connection` 写入 `ddc_event` |
| `EventNotifyServiceImpl` | 执行 notify 并更新 `ddc_event` 状态 |
| `LocalNotifyServiceImpl` | 本地通知实现，串联 notify 和 listen |
| `DomainEventListenSupport` | 注册并查找 `@EventListen` 监听器 |
| `EventListenServiceImpl` | 写入 `ddc_event_listen` 并分发监听器 |
| `DdcTaskExecutor` | 异步执行 notify/listener |

### 2.5 设计亮点

| 设计点 | 价值 |
|---|---|
| 同连接写事件表 | 事件写入和业务提交由同一个数据库事务保证 |
| 生命周期状态机 | 用 pending、checked、created、submitted、rolledBack 区分阶段 |
| `@EventNotify`/`@EventListen` 声明式 API | 使用方式直观，业务代码侵入较少 |
| 监听器注册表 | 启动时扫描监听器，运行时按 domain/event 快速匹配 |
| 后置处理器扩展点 | 为告警、日志、监控预留扩展口 |

---

## 三、问题清单与优化建议

### P0：数据正确性与事务安全，需立即修复

---

#### 问题 1：普通事务方法可能因 `pointcutInstance == null` 触发 NPE

**文件：** `ddc-client/src/main/java/com/transactionManager/MyDataSourceTransactionManager.java`

**问题描述：**

`ClientConfiguration` 会注册自定义 `transactionManager`，它会影响所有 `@Transactional` 方法。当前 `doCommit()` 默认认为当前事务一定来自 `@EventNotify`，直接调用：

```java
PointcutInstance pointcutInstance = PointcutManager.getPointcutInstance();
pointcutInstance.createEvent();
```

如果某个普通业务方法只有 `@Transactional`，没有 `@EventNotify`，`pointcutInstance` 为 null，提交时会直接 NPE，严重时会导致业务事务无法提交。

**修复方案：**

短期先加 null 判断，避免影响普通事务：

```java
PointcutInstance pointcutInstance = PointcutManager.getPointcutInstance();
if (pointcutInstance != null) {
    pointcutInstance.createEvent();
}

super.doCommit(status);

if (pointcutInstance != null) {
    pointcutInstance.onSubmitted();
}
```

同时 `doRollback()` 也需要做 null 判断：

```java
PointcutInstance pointcutInstance = PointcutManager.getPointcutInstance();
if (pointcutInstance != null) {
    pointcutInstance.onRolledBack();
}
```

**长期建议：**

不要通过替换全局事务管理器实现 outbox，改为基于 `TransactionSynchronizationManager` 注册同步回调。

---

#### 问题 2：`updateEventNotify` 参数顺序错误，`notify_id` 会写成事件主键

**文件：** `ddc-client/src/main/java/com/notify/dao/impl/EventDAOImpl.java`

**问题描述：**

SQL 中第三个字段是 `notify_id=?`，但参数传入的是 `event.getId()`：

```java
Object[] params = new Object[] {
    event.getState(),
    event.getNotifyType(),
    event.getId(),
    event.getNotifyResult(),
    event.getId(),
    event.getVersion()
};
```

这会导致 `notify_id` 保存错误，影响后续 trace、幂等、排查问题。

**修复方案：**

```java
Object[] params = new Object[] {
    event.getState(),
    event.getNotifyType(),
    event.getNotifyId(),
    event.getNotifyResult(),
    event.getId(),
    event.getVersion()
};
```

同时建议检查乐观锁更新结果：

```java
int result = jdbcTemplate.update(UPDATE_EVENT_BY_NOTIFY_SQL, params);
if (result != 1) {
    throw new IllegalStateException("updateEventNotify optimistic lock failed, eventId=" + event.getId());
}
```

---

#### 问题 3：`updateErrorInfo` 用主键 id 去查 event_id，错误信息可能写不回

**文件：** `ddc-client/src/main/java/com/listen/dao/impl/EventListenDAOImpl.java`

**问题描述：**

`updateErrorInfo(Long id, ...)` 的入参是 `ddc_event_listen.id`，但内部调用：

```java
EventListenDO eventListen = this.queryEventListen(id);
```

而 `queryEventListen()` 使用的是：

```sql
select * from ddc_event_listen where event_id = ?
```

这会导致按错字段查询，错误信息无法正确拼接，也可能误查到其他记录。

**修复方案：**

新增按主键查询方法：

```java
private static final String SELECT_BY_ID_SQL =
    "select * from ddc_event_listen where id = ?";

public EventListenDO queryEventListenById(Long id) {
    List<EventListenDO> list = jdbcTemplate.query(SELECT_BY_ID_SQL, rowMapper, id);
    return CollectionUtils.isEmpty(list) ? null : list.get(0);
}
```

或者更简单：直接 SQL 拼接并截断，不先查：

```java
private static final String UPDATE_ERROR_INFO_SQL =
    "update ddc_event_listen " +
    "set gmt_modified=now(), " +
    "error_info=left(concat(ifnull(error_info,''), ?), 2000) " +
    "where id=?";
```

---

#### 问题 4：`@EventNotify` 无事务场景会静默丢消息

**文件：** `ddc-client/src/main/java/com/notify/DomainEventNotifyLifecycle.java`

**问题描述：**

如果业务方法加了 `@EventNotify`，但没有真实开启 Spring 事务，`MyDataSourceTransactionManager.doBegin()` 不会执行，`PointcutInstance` 一直停留在 `pending`。最后 `finishEvent()` 返回 null，事件不会写库，也不会抛错。

这属于危险的静默失败。使用方以为事件已经发出，实际没有任何记录。

**修复方案：**

短期建议在 `EventNotifyAspect` 或 `DomainEventNotifyLifecycle.end()` 中做强校验：

```java
if (!TransactionSynchronizationManager.isActualTransactionActive()) {
    throw new IllegalStateException(
        "@EventNotify method must run inside a Spring transaction. " +
        "Please add @Transactional or enable DDC non-transaction mode explicitly."
    );
}
```

如果确实要支持无事务场景，应该显式提供配置项，例如：

```properties
ddc.event.allow-non-transaction=false
```

默认不允许，避免误用。

---

#### 问题 5：`InsertEventDAOImpl` 插入参数中 `gmt_create` 取错字段

**文件：** `ddc-client/src/main/java/com/notify/dao/impl/InsertEventDAOImpl.java`

**问题描述：**

插入 SQL 的第一列是 `gmt_create`，但当前参数使用的是 `event.getGmtEvent()`：

```java
return new Object[] {
    event.getGmtEvent(),
    event.getGmtModified(),
    ...
};
```

当前 `buildEventDO()` 中三个时间都用同一个 `Date`，所以暂时不明显；但字段语义已经错了，后续只要事件发生时间和创建时间不同，就会产生脏数据。

**修复方案：**

```java
return new Object[] {
    event.getGmtCreate(),
    event.getGmtModified(),
    event.getDomain(),
    ...
};
```

---

### P1：可靠性闭环，需尽快补齐

---

#### 问题 6：缺少补偿扫描，服务重启后 `pending` 消息可能永久滞留

**文件：** `ddc-client/src/main/java/com/notify/dao/AbstractEventDAO.java`

**问题描述：**

接口中声明了：

```java
List<EventDO> queryPendingEventList();
```

但抽象实现里直接抛出 `UnsupportedOperationException`，实际没有补偿任务。

当前 notify 依赖事务提交后的内存线程池。如果服务在“事务已提交，异步任务未执行”之间宕机，`ddc_event` 里会留下 `pending` 事件，但没有任何后台逻辑重新捞起。

**优化方案：**

实现补偿查询：

```sql
select *
from ddc_event
where state in ('pending', 'failed')
  and retry_times < ?
  and gmt_modified < ?
order by id
limit ?
```

新增定时任务：

```java
@Scheduled(fixedDelayString = "${ddc.compensate.fixed-delay:60000}")
public void compensate() {
    List<EventDO> events = eventDAO.queryPendingEventList();
    for (EventDO event : events) {
        DdcTaskExecutor.execute(() -> eventNotifyService.eventNotify(event));
    }
}
```

多节点部署时需要抢占机制，例如：

```sql
update ddc_event
set state='processing', version=version+1
where id=? and version=? and state in ('pending','failed')
```

---

#### 问题 7：`retryTimes` 字段没有形成重试闭环

**文件：** `ddc-client/src/main/java/com/send/impl/EventNotifyServiceImpl.java`、`ddc-client/src/main/java/com/listen/service/impl/EventListenServiceImpl.java`

**问题描述：**

`EventDO`、`EventListenDO` 都有 `retryTimes` 字段，`@EventListen` 也有 `maxRetryTimes`，但当前代码没有完整地递增和重新调度：

| 位置 | 当前情况 |
|---|---|
| notify 失败 | 状态改成 `failed`，但没有递增 `retry_times` |
| listener 失败 | 只更新 `error_info`，没有递增 `retry_times` |
| 最大重试次数 | `canExecute()` 会读取，但缺少可持续触发重试的调度器 |

**优化方案：**

为 notify 和 listen 分别定义状态机：

```text
pending -> processing -> completed
                    \-> failed -> retrying -> processing
                    \-> dead
```

失败时写入：

```sql
retry_times = retry_times + 1,
next_retry_time = date_add(now(), interval ? second)
```

定时任务只扫描 `next_retry_time <= now()` 的记录。超过最大次数后进入 `dead`，触发告警。

---

#### 问题 8：`ddc_event_listen` 幂等依赖先查后插，缺少数据库唯一约束

**文件：** `ddc-client/src/main/java/com/listen/service/impl/EventListenServiceImpl.java`

**问题描述：**

当前幂等流程是：

```java
EventListenDO eventListenDO = eventListenDAO.queryEventListen(domainEventListenDTO.getEventId());
if (eventListenDO != null) {
    return Result.fail(ErrorCode.REPEAT_RECV, ...);
}
eventListenDAO.saveEventListen(eventListenDO);
```

这属于典型的“先查后插”竞争条件。并发场景或多节点重复投递时，两个线程可能都查不到，然后都插入成功。

**修复方案：**

DDL 层必须加唯一索引：

```sql
alter table ddc_event_listen
add unique key uk_event_id(event_id);
```

Java 层捕获重复键异常，将其视为幂等成功：

```java
try {
    eventListenDAO.saveEventListen(eventListenDO);
} catch (DuplicateKeyException e) {
    return Result.fail(ErrorCode.REPEAT_RECV, "event already received");
}
```

---

#### 问题 9：缺少 DDL，导致接入方无法准确创建表结构

**文件：** 当前仓库未发现 `ddc_event`、`ddc_event_listen` 建表脚本

**问题描述：**

代码中硬编码了两张表和大量列：

| 表 | 用途 |
|---|---|
| `ddc_event` | 事件 outbox 表 |
| `ddc_event_listen` | 本地监听执行记录表 |

但仓库没有标准 DDL。接入方只能从 SQL 字符串反推字段类型、索引、唯一约束和长度，容易建错。

**优化方案：**

新增：

```text
ddc-client/src/main/resources/db/migration/V1__create_ddc_tables.sql
```

至少包含：

```sql
create table ddc_event (...);
create table ddc_event_listen (...);

create index idx_ddc_event_state_modified on ddc_event(state, gmt_modified);
create unique index uk_ddc_event_listen_event_id on ddc_event_listen(event_id);
```

---

### P2：稳定性与可维护性，建议近期排期

---

#### 问题 10：`@EventListen.order` 未实际参与排序

**文件：** `ddc-client/src/main/java/com/listen/support/DomainEventListenSupport.java`

**问题描述：**

注解里定义了：

```java
int order() default 0;
```

但注册监听器时直接按 Spring Bean 扫描顺序把 name 放入 list，没有基于 `order` 排序。最终执行顺序不可预测，也和注解语义不一致。

**优化方案：**

注册时保存监听器元信息，启动后统一排序：

```java
nameList.sort(Comparator.comparingInt(name ->
    domainEventListenMap.get(name).getEventListen().order()
));
```

更稳的做法是注册阶段先收集，所有 Bean 扫描完成后再构建 `domainEventUniqueKeyMap`。

---

#### 问题 11：`listenResult` 使用 `Long` 位图，可读性差且存在上限

**文件：** `ddc-client/src/main/java/util/ParamParserUtils.java`、`ddc-client/src/main/java/com/listen/service/impl/EventListenServiceImpl.java`

**问题描述：**

当前用 `2^n - 1` 记录 n 个 listener 的待执行状态。这个方案节省字段，但存在几个问题：

| 问题 | 说明 |
|---|---|
| 上限明显 | `Long` 最多只能表达有限 listener 数量 |
| `Math.pow` 精度风险 | double 在较大整数上会失去精度 |
| 可读性差 | DBA 或排障人员无法直观看出哪个 listener 失败 |
| 扩展困难 | 无法记录单个 listener 的耗时、错误次数、最后执行时间 |

**短期修复：**

注册时校验 listener 数量，超过上限直接启动失败：

```java
if (nameList.size() >= 53) {
    throw new IllegalStateException("Too many listeners for one domain/event");
}
```

这里建议用 53 而不是 63，因为 `Math.pow` 返回 double，超过 53 位整数精度就不可靠。

**长期方案：**

改成每个 listener 一行：

```text
ddc_event_listen
    event_id
    listener_name
    state
    retry_times
    error_info
    gmt_last_execute
```

唯一键：

```sql
unique key uk_event_listener(event_id, listener_name)
```

这种结构更适合补偿、幂等、监控和排障。

---

#### 问题 12：线程池硬编码，缺少监控和关闭逻辑

**文件：** `ddc-client/src/main/java/util/DdcTaskExecutor.java`

**问题描述：**

当前线程池固定：

```java
private static int invokerPoolSize = 10;
private static int loadFactor = 20;
private static BlockingQueue<Runnable> queue =
    new ArrayBlockingQueue<Runnable>(invokerPoolSize * loadFactor);
```

问题包括：

- 核心线程数、队列长度不可配置。
- 无线程名，不利于排查。
- 无队列积压、拒绝次数、执行耗时指标。
- 无 `shutdown()`，应用关闭时可能丢任务。
- `CallerRunsPolicy` 可能让业务线程执行 DDC 任务，影响接口响应。

**优化方案：**

抽成 Spring Bean，并支持配置：

```properties
ddc.executor.core-size=10
ddc.executor.max-size=20
ddc.executor.queue-capacity=1000
ddc.executor.rejected-policy=mark-failed
```

暴露指标：

```text
ddc_executor_queue_size
ddc_executor_active_count
ddc_executor_rejected_total
ddc_event_pending_total
ddc_event_failed_total
```

---

#### 问题 13：启动时遍历所有 Bean 并 `getBean()`，可能触发过早初始化

**文件：** `ddc-client/src/main/java/com/DomainEventApplicationContext.java`

**问题描述：**

当前启动时：

```java
String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
for (String bean : beanDefinitionNames) {
    Object obj = applicationContext.getBean(bean);
    ...
}
```

这会主动实例化所有 Bean，可能导致：

- 懒加载 Bean 被提前创建。
- FactoryBean 或代理 Bean 初始化顺序被改变。
- 某些依赖尚未准备好时被提前触发。

**优化方案：**

改为使用 Spring 的 bean 后置处理器或 `MethodIntrospector` 扫描目标方法。更推荐实现 `BeanPostProcessor`：

```java
public Object postProcessAfterInitialization(Object bean, String beanName) {
    Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
    Map<Method, EventListen> methods = MethodIntrospector.selectMethods(...);
    // 注册监听方法
    return bean;
}
```

---

#### 问题 14：代理解包使用反射访问 Spring 内部字段，兼容性风险高

**文件：** `ddc-client/src/main/java/util/AnnotationUtil.java`

**问题描述：**

当前代码通过反射读取：

```java
CGLIB$CALLBACK_0
advised
h
```

这些字段属于 Spring AOP/JDK Proxy 内部实现细节，版本变化后容易失效。

**优化方案：**

优先使用 Spring 提供的工具方法：

```java
Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
```

方法匹配可以用：

```java
AopUtils.getMostSpecificMethod(method, targetClass)
BridgeMethodResolver.findBridgedMethod(method)
```

---

#### 问题 15：`notifyMethod` 定义了但没有真正生效

**文件：** `ddc-client/src/main/java/com/annotation/EventNotify.java`、`ddc-client/src/main/java/com/EventNotifyAspect.java`、`ddc-client/src/main/java/com/notify/DomainEventNotifyDTO.java`

**问题描述：**

`@EventNotify` 定义了：

```java
NotifyMethodEnum notifyMethod() default NotifyMethodEnum.local;
```

但 `DomainEventNotifyDTO` 只有 `domain` 和 `event`，`buildDomainEventNotifyDTO()` 也没有传递 `notifyMethod`。最终 `EventDO.notifyType` 也没有被正确设置。`EventNotifyServiceImpl` 中 remote notify 逻辑也是 TODO。

**优化方案：**

补齐字段传递：

```java
public class DomainEventNotifyDTO {
    private String domain;
    private String event;
    private NotifyMethodEnum notifyMethod;
}
```

构建事件时：

```java
event.setNotifyType(domainEventNotify.getNotifyMethod().name());
```

发送时明确分支：

```java
switch (notifyMethod) {
    case local:
        return localSendNotify(dto);
    case remote:
        return remoteSendNotify(dto);
    case localAndRemote:
        return sendBoth(dto);
}
```

---

### P3：架构演进，适合中长期规划

---

#### 问题 16：事务集成方式侵入性强

**文件：** `ddc-client/src/main/java/com/config/ClientConfiguration.java`

**问题描述：**

`ClientConfiguration` 直接注册：

```java
@Bean
public PlatformTransactionManager transactionManager() {
    return new MyDataSourceTransactionManager(dataSource);
}
```

这要求接入方接受 ddc-client 提供的事务管理器。对于已有动态数据源、ShardingSphere、Seata、多事务管理器、JPA 事务管理器的项目，接入成本和冲突风险都很高。

**推荐演进方案：**

使用 Spring 原生事务同步机制：

```java
TransactionSynchronizationManager.registerSynchronization(
    new TransactionSynchronizationAdapter() {
        @Override
        public void beforeCommit(boolean readOnly) {
            // 当前事务提交前写 ddc_event
        }

        @Override
        public void afterCommit() {
            // 提交后异步分发
        }

        @Override
        public void afterCompletion(int status) {
            // 清理 ThreadLocal
        }
    }
);
```

这样 ddc-client 不需要接管事务管理器，只需要要求业务方法处于 Spring 事务内。

---

#### 问题 17：静态单例和 `ThreadLocal` 使用较多，容易产生上下文污染

**文件：** `DomainEventApplicationContext`、`PointcutManager`、`DomainEventListenSupport`、`DomainEventPostProcessorManager`

**问题描述：**

当前多处使用 static 保存运行时状态：

| 类 | 静态状态 |
|---|---|
| `DomainEventApplicationContext` | `instance` |
| `PointcutManager` | `ThreadLocal<PointcutInstance>`、`ThreadLocal<Map>` |
| `DomainEventListenSupport` | listener 注册表、service |
| `DomainEventPostProcessorManager` | postProcessor 列表 |

风险：

- 多 Spring ApplicationContext 时互相污染。
- 单测之间状态残留。
- Web 容器热部署时 classloader 泄漏。
- 异步线程无法自动继承上下文。

**优化方案：**

将这些组件收敛为 Spring Bean，由容器管理生命周期。`ThreadLocal` 必须在 finally 中清理，并尽量只保存一次请求内的最小上下文。

---

#### 问题 18：工程化交付不完整

**文件：** 仓库根目录、`ddc-client/pom.xml`

**问题描述：**

当前存在几个交付问题：

- 仓库有 `mvnw`，但缺少 `.mvn/wrapper`，Wrapper 无法运行。
- 缺少 ddc-client 的单元测试和集成测试。
- 缺少标准配置文档。
- `ddc-client` 直接依赖 MySQL Driver，作为中间件不应强绑定具体数据库驱动。
- Spring、AspectJ、MySQL 依赖版本较旧且分散在父子 POM 中。

**优化方案：**

1. 补齐 Maven Wrapper：

```text
.mvn/wrapper/maven-wrapper.jar
.mvn/wrapper/maven-wrapper.properties
```

2. 将 `ddc-client` 拆成：

```text
ddc-client-core
ddc-client-spring
ddc-client-spring-boot-starter
```

3. 数据库驱动由业务项目提供，client 只依赖 JDBC/Spring JDBC。

4. 增加测试：

| 测试 | 覆盖点 |
|---|---|
| 事务提交测试 | 业务提交后事件落库 |
| 事务回滚测试 | 回滚后事件不落库 |
| 普通事务测试 | 无 `@EventNotify` 不受影响 |
| 无事务误用测试 | 明确报错 |
| 补偿测试 | pending 事件能被重新分发 |
| 幂等测试 | 重复 eventId 只消费一次 |

---

## 四、优先级汇总

| 优先级 | 问题编号 | 问题描述 | 影响 |
|---|---|---|---|
| P0 | 问题 1 | 普通事务可能因 `pointcutInstance == null` NPE | 影响所有事务方法，可能线上事故 |
| P0 | 问题 2 | `notify_id` 写错字段 | 追踪链路和通知结果错误 |
| P0 | 问题 3 | `updateErrorInfo` 按错字段查询 | listener 失败信息记录不准确 |
| P0 | 问题 4 | 无事务场景静默丢消息 | 使用方误以为消息已投递 |
| P0 | 问题 5 | `gmt_create` 插入参数语义错误 | 数据字段不准确 |
| P1 | 问题 6 | 缺少补偿扫描 | 服务重启后消息可能永久滞留 |
| P1 | 问题 7 | 重试字段没有闭环 | 失败消息无法稳定恢复 |
| P1 | 问题 8 | 监听幂等缺少唯一约束 | 并发或多节点可能重复消费 |
| P1 | 问题 9 | 缺少 DDL | 接入方容易建错表 |
| P2 | 问题 10 | `order` 未生效 | listener 执行顺序不可控 |
| P2 | 问题 11 | 位图方案可读性差且有上限 | 排障和扩展困难 |
| P2 | 问题 12 | 线程池硬编码、无监控 | 高负载下不可观测 |
| P2 | 问题 13 | 启动扫描过早实例化 Bean | 可能改变 Spring 初始化行为 |
| P2 | 问题 14 | 代理解包依赖内部字段 | Spring 版本升级风险 |
| P2 | 问题 15 | `notifyMethod` 未生效 | API 语义和行为不一致 |
| P3 | 问题 16 | 事务管理器侵入性强 | 难以接入复杂系统 |
| P3 | 问题 17 | 静态状态较多 | 多上下文、单测、热部署风险 |
| P3 | 问题 18 | 工程化交付不完整 | 难以作为中间件发布 |

---

## 五、建议迭代计划

### 第一期：1 周内，先保证不出错

- 修复 `MyDataSourceTransactionManager` 的 null 判断。
- 修复 `updateEventNotify` 参数错误。
- 修复 `updateErrorInfo` 查询字段错误。
- 修复 `InsertEventDAOImpl` 的 `gmt_create` 参数。
- 对 `@EventNotify` 无事务场景直接抛出明确异常。
- 补充最小 DDL，至少包含两张表和唯一索引。

### 第二期：2 到 4 周，补齐可靠性闭环

- 实现 `queryPendingEventList()`。
- 新增补偿定时任务。
- 增加 `retry_times`、`next_retry_time`、最大重试和死信状态。
- 线程池配置化，增加线程名、拒绝告警、基础监控。
- 给 `ddc_event_listen.event_id` 加唯一索引，并处理重复键。

### 第三期：1 到 2 个月，降低接入成本

- 去掉对 `MyDataSourceTransactionManager` 的强依赖。
- 改为基于 `TransactionSynchronizationManager` 感知事务。
- 将静态管理器改为 Spring Bean。
- 优化监听器注册方式，避免启动时主动 `getBean()`。
- 让 `notifyMethod` 真正生效，明确 local/remote/localAndRemote 行为。

### 第四期：中长期，提升中间件成熟度

- 将 listener 执行记录从位图改为每 listener 一行。
- 拆分 core、spring、spring-boot-starter 模块。
- 引入 Flyway/Liquibase 脚本。
- 提供 starter 自动配置、配置元数据和样例项目。
- 增加完整单元测试和基于 Testcontainers 的集成测试。

---

## 六、推荐的最终形态

理想情况下，ddc-client 对业务方的使用方式应该收敛为：

```java
@EnableDdcClient
@SpringBootApplication
public class Application {
}
```

配置：

```properties
ddc.enabled=true
ddc.executor.core-size=10
ddc.executor.queue-capacity=1000
ddc.compensate.enabled=true
ddc.compensate.fixed-delay=60000
ddc.event.require-transaction=true
```

业务代码：

```java
@EventNotify(domain = "order", event = "paid")
@Transactional(rollbackFor = Exception.class)
public void pay(OrderPayCommand command) {
    orderRepository.markPaid(command.getOrderId());
    DomainEventNotifyContext.put(DomainEventNotifyContext.ENTITY_ID, command.getOrderId());
}
```

监听代码：

```java
@EventListen(listenDomain = "order", listenEvent = "paid", order = 10)
public void onOrderPaid(@EventParam(name = "entityId") String orderId) {
    // send coupon, update projection, notify user, etc.
}
```

底层由 ddc-client 保证：

| 能力 | 目标 |
|---|---|
| 事务一致性 | 业务提交和事件落库原子完成 |
| 至少一次投递 | pending/failed 事件可补偿 |
| 幂等消费 | 同一 eventId/listenerName 只成功一次 |
| 可观测性 | 有状态、重试、积压、失败原因 |
| 低侵入接入 | 不替换业务事务管理器 |

---

## 七、Codex 版总结

这个项目最有价值的地方是思路清楚：通过 AOP 和事务提交前同连接写表，搭出了本地消息表的核心路径。真正要补的是“中间件化”的后半段：不要影响普通事务，不要丢 pending 消息，不要让失败只停留在日志里，不要让接入方靠猜来建表和配置。

优先级建议非常明确：

1. 先修 P0，避免数据写错和事务 NPE。
2. 再补 P1，让消息失败后能被重新捞起。
3. 然后处理 P2，让运行时稳定、可观测、可排障。
4. 最后做 P3，把它从 Demo 型实现升级成真正可复用的客户端中间件。
