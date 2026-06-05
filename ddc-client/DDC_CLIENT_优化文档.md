# ddc-client 优化文档

> 版本：v1.2 | 日期：2026-06-05 | 本版补充 Codex 分析结论

---

## 一、设计概述

### 核心模式

ddc-client 实现了 **Transactional Outbox（本地消息表）** 模式，解决业务数据变更与领域事件投递的原子性问题，无需引入分布式事务。

### 整体流程

```
@EventNotify 方法
    │
    ├─ AOP 拦截 → lifecycle.start() 创建 PointcutInstance（pending 状态）
    │
    ├─ 业务代码执行，通过 DomainEventNotifyContext.put() 写入 entityId/event 等参数
    │
    ├─ MyDataSourceTransactionManager.doBegin()
    │       → 获取当前 Connection 注入 PointcutInstance（checked 状态）
    │
    ├─ doCommit()
    │       → createEvent()：用同一 Connection 写入 ddc_event 表（created 状态）
    │       → super.doCommit()：提交业务事务（submitted 状态）
    │       ↑── 关键：消息写入与业务提交同一事务，原子性由数据库保证
    │
    └─ afterCompletion 回调（事务提交后）
           → DdcTaskExecutor 线程池异步分发
           → LocalNotifyServiceImpl → 写 ddc_event_listen → dispatch() → 调用 @EventListen 方法
```

### 关键设计亮点

| 设计点 | 描述 |
|---|---|
| 同连接原子写入 | `InsertEventDAOImpl` 复用业务事务的 `Connection`，无需 XA 协议 |
| 位图记录监听进度 | `listenResult` 初始值 `2^n-1`，每成功一个 listener 对应位清零，SQL 层原子操作 |
| 状态机流转 | `PointcutInstance` 维护 pending→checked→created→submitted/rolledBack，防止非法流转 |
| 嵌套切面防护 | `DomainEventNotifyContext` key 检测嵌套调用，直接透传避免重复触发 |
| SPI 后置扩展点 | `DomainEventPostProcessor` 支持 notify/listen 后的自定义处理（告警、日志等） |

---

## 二、问题清单与优化建议

### P0 — 数据正确性（需立即修复）

---

#### 问题 1：`updateEventNotify` SQL 参数顺序错误

**文件：** `com/notify/dao/impl/EventDAOImpl.java:29`

**问题描述：**
`notify_id=?` 位置传入的是 `event.getId()`，应为 `event.getNotifyId()`，导致 `ddc_event` 表的 `notify_id` 字段始终被错误地写成主键 id 值。

```java
// 当前错误代码
private static final String UPDATE_SQL =
    "update ddc_event set gmt_modified=now(),state=?,notify_type=?,notify_id=?,notify_result=?,version=version+1 where id=? and version=?";

Object[] params = new Object[] {
    event.getState(),
    event.getNotifyType(),
    event.getId(),           // ← BUG：notify_id 位置传了 id
    event.getNotifyResult(),
    event.getId(),
    event.getVersion()
};
```

**修复方案：**
```java
Object[] params = new Object[] {
    event.getState(),
    event.getNotifyType(),
    event.getNotifyId(),     // ← 修正为 notifyId
    event.getNotifyResult(),
    event.getId(),
    event.getVersion()
};
```

---

#### 问题 2：`updateErrorInfo` 传参类型错误

**文件：** `com/listen/dao/impl/EventListenDAOImpl.java:100`

**问题描述：**
`updateErrorInfo(Long id, ...)` 内部调用 `queryEventListen(id)`，但 `queryEventListen` 按 `event_id` 列查询，而此处传入的是 `ddc_event_listen.id`（主键），导致查不到记录或查到错误记录，错误信息无法正确拼接写回。

```java
// 当前错误代码
public boolean updateErrorInfo(Long id, String errorInfo) {
    EventListenDO eventListen = this.queryEventListen(id);  // ← 按 event_id 查，传的是主键 id
    ...
}
```

**修复方案：**
新增按主键查询方法，或在 `updateErrorInfo` 中直接更新，不依赖先查后改：

```java
// 方案一：直接更新，不做查询
public boolean updateErrorInfo(Long id, String errorInfo) {
    // 拼接时在 SQL 里做截断处理
    int result = jdbcTemplate.update(
        "update ddc_event_listen set error_info=LEFT(CONCAT(IFNULL(error_info,''),?),2000), gmt_modified=now() where id=?",
        "，" + errorInfo, id
    );
    return result > 0;
}
```

---

#### 问题 3：`doCommit` 中 `pointcutInstance` 未做 null 判断导致 NPE

**文件：** `com/transactionManager/MyDataSourceTransactionManager.java:73`

**问题描述：**
当一个加了 `@Transactional` 但没有 `@EventNotify` 的方法提交事务时，`PointcutManager.getPointcutInstance()` 返回 null，代码直接调用 `pointcutInstance.createEvent()` 抛出 NPE，导致业务事务无法提交，引发线上事故。

```java
// 当前错误代码
public void doCommit(DefaultTransactionStatus status) {
    PointcutInstance pointcutInstance = PointcutManager.getPointcutInstance();
    try {
        pointcutInstance.createEvent();  // ← pointcutInstance 可能为 null
    } catch (Throwable e) {
        doRollback(status);
        throw e;
    }
    super.doCommit(status);
    pointcutInstance.onSubmitted();
}
```

**修复方案：**
```java
public void doCommit(DefaultTransactionStatus status) {
    PointcutInstance pointcutInstance = PointcutManager.getPointcutInstance();
    if (pointcutInstance != null) {
        try {
            pointcutInstance.createEvent();
        } catch (Throwable e) {
            log.error("createEvent error, will rollback", e);
            doRollback(status);
            throw e;
        }
    }
    super.doCommit(status);
    if (pointcutInstance != null) {
        pointcutInstance.onSubmitted();
    }
}
```

---

#### 问题 4（补充）：`InsertEventDAOImpl` 中 `gmt_create` 使用了 `gmtEvent` 字段

**文件：** `com/notify/dao/impl/InsertEventDAOImpl.java:62`

**问题描述：**
INSERT SQL 第一列是 `gmt_create`，但参数数组第一位传入的是 `event.getGmtEvent()`：

```java
return new Object[] {
    event.getGmtEvent(),    // ← gmt_create 列，应为 event.getGmtCreate()
    event.getGmtModified(),
    ...
};
```

当前 `buildEventDO()` 三个时间赋值相同，暂时看不出问题；一旦 event 发生时间与记录创建时间不同（如事件重放、时区差异），`gmt_create` 就会写入脏数据。

**修复方案：**
```java
return new Object[] {
    event.getGmtCreate(),   // ← 修正
    event.getGmtModified(),
    event.getDomain(),
    ...
};
```

---

### P1 — 可靠性（需尽快修复）

---

#### 问题 4：缺少补偿轮询任务，消息存在丢失风险

**文件：** 无对应实现（`AbstractEventDAO.queryPendingEventList` 已声明但未实现）

**问题描述：**
事务提交后，消息分发完全依赖内存中的线程池。以下场景会导致 `ddc_event` 中 `state=pending` 的消息永久滞留，无法被处理：

- JVM 崩溃或服务重启
- `DdcTaskExecutor` 队列满，`CallerRunsPolicy` 在极端情况下可能导致任务执行异常
- 分发过程中抛出未捕获异常

**优化方案：**

**Step 1：** 实现 `queryPendingEventList`，查询超时未处理的消息。

```java
// EventDAOImpl 中新增
private static final String QUERY_PENDING_SQL =
    "select * from ddc_event where state='pending' and gmt_event < ? limit 100";

@Override
public List<EventDO> queryPendingEventList() {
    Date threshold = new Date(System.currentTimeMillis() - 5 * 60 * 1000); // 5分钟前
    return new JdbcTemplate(dataSource).query(QUERY_PENDING_SQL, rowMapper, threshold);
}
```

**Step 2：** 新增补偿定时任务。

```java
@Component
public class EventCompensateScheduler {

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void compensate() {
        List<EventDO> pendingList = eventDAO.queryPendingEventList();
        if (CollectionUtils.isEmpty(pendingList)) return;
        log.info("EventCompensate found {} pending events", pendingList.size());
        pendingList.forEach(event ->
            DdcTaskExecutor.execute(() -> eventNotifyService.eventNotify(event))
        );
    }
}
```

**Step 3：** 多节点部署时，在补偿任务入口加分布式锁（Redis/DB 乐观锁），防止重复扫描。

---

#### 问题 5：无事务场景消息静默丢失

**文件：** `com/notify/DomainEventNotifyLifecycle.java`

**问题描述：**
若 `@EventNotify` 方法没有 `@Transactional`，`MyDataSourceTransactionManager.doBegin` 不会被调用，`PointcutInstance` 状态永远是 `pending`，`finishEvent()` 返回 null，消息既不写库也不报错，完全静默丢失。

**优化方案：**

在 `doEndEvent` 中检测到 `pending` 状态时进行告警，并支持可选的"无事务直写"模式：

```java
private void doEndEvent() {
    DomainEventNotifyContext.clear();
    PointcutInstance<EventDO> pointcutInstance = PointcutManager.removePointcutInstance();
    PointcutManager.clear();
    if (pointcutInstance == null) return;

    EventDO eventDO = pointcutInstance.finishEvent();
    if (eventDO == null) {
        // 区分 pending（无事务）和 rolledBack（回滚）打不同日志
        String state = ((DomainEventNotifyInstance) pointcutInstance).getState();
        if (PointcutStateEnum.pending.name().equals(state)) {
            log.error("EventNotify method has no @Transactional, message is lost! " +
                      "Please add @Transactional or check transaction configuration.");
        }
        return;
    }
    DdcTaskExecutor.execute(() -> EventNotifyServiceFactory.getEventNotifyService().eventNotify(eventDO));
}
```

---

#### 问题（P1 补充 A）：`ddc_event_listen` 幂等依赖"先查后插"，缺少数据库唯一约束

**文件：** `com/listen/service/impl/EventListenServiceImpl.java:40`

**问题描述：**
当前幂等流程为先查再插：
```java
EventListenDO exist = eventListenDAO.queryEventListen(eventId);
if (exist != null) {
    return Result.fail(ErrorCode.REPEAT_RECV, ...);
}
eventListenDAO.saveEventListen(eventListenDO);
```
典型的"先查后插"竞争条件。补偿任务多节点并发时，两个线程均查不到记录，然后都插入成功，导致同一事件被消费两次。

**修复方案：**

DDL 层加唯一索引（同时也是缺少 DDL 的一部分，见下节）：
```sql
alter table ddc_event_listen add unique key uk_event_id(event_id);
```

Java 层捕获重复键异常，视为幂等成功：
```java
try {
    eventListenDAO.saveEventListen(eventListenDO);
} catch (DuplicateKeyException e) {
    log.info("saveEventListen duplicate, treat as idempotent, eventId:{}", dto.getEventId());
    return Result.fail(ErrorCode.REPEAT_RECV, "event already received");
}
```

---

#### 问题（P1 补充 B）：缺少 DDL 脚本，接入方无法准确建表

**问题描述：**
代码中硬编码了 `ddc_event`、`ddc_event_listen` 两张表及大量列名，但仓库没有任何建表脚本。接入方只能从 SQL 字符串反推字段类型、长度和索引，极易建错（如字段长度不够导致截断，缺索引导致全表扫描）。

**优化方案：**
新建 `ddc-client/src/main/resources/db/migration/V1__create_ddc_tables.sql`，至少包含：

```sql
CREATE TABLE ddc_event (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  gmt_create    DATETIME     NOT NULL,
  gmt_modified  DATETIME     NOT NULL,
  gmt_event     DATETIME     NOT NULL COMMENT '事件发生时间',
  domain        VARCHAR(64)  NOT NULL,
  entity_id     VARCHAR(64)  NOT NULL,
  event         VARCHAR(64)  NOT NULL,
  state         VARCHAR(16)  NOT NULL COMMENT 'pending/completed/failed',
  notify_type   VARCHAR(16)  DEFAULT NULL,
  gmt_notify    DATETIME     DEFAULT NULL,
  notify_id     VARCHAR(64)  DEFAULT NULL,
  notify_result VARCHAR(512) DEFAULT NULL,
  event_context TEXT         DEFAULT NULL,
  retry_times   INT          NOT NULL DEFAULT 0,
  version       INT          NOT NULL DEFAULT 1,
  local_send    TINYINT(1)   DEFAULT NULL,
  remote_send   TINYINT(1)   DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_state_gmt_event (state, gmt_event)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ddc_event_listen (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  gmt_create     DATETIME     NOT NULL,
  gmt_modified   DATETIME     NOT NULL,
  event_id       BIGINT       NOT NULL,
  domain         VARCHAR(64)  NOT NULL,
  event          VARCHAR(64)  NOT NULL,
  event_content  TEXT         DEFAULT NULL,
  msg_id         VARCHAR(64)  DEFAULT NULL,
  listen_names   VARCHAR(512) DEFAULT NULL,
  listen_result  BIGINT       NOT NULL DEFAULT 0,
  error_info     VARCHAR(2000) DEFAULT NULL,
  retry_times    INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_event_id (event_id),
  KEY idx_listen_result_gmt_modified (listen_result, gmt_modified)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### P2 — 稳定性（建议近期排期）

---

#### 问题 6：listener 执行成功但 DB 状态更新失败时，会导致重复执行且无法感知

**文件：** `com/listen/handler/EventListenHandlerImpl.java`、`com/listen/service/impl/EventListenServiceImpl.java`

**问题描述：**

`listener 执行` 和 `DB 位图更新` 是两个独立操作，之间存在不一致窗口：

```
listener 执行成功
       ↓
  [崩溃 / 超时 / 网络抖动]  ← 不一致窗口
       ↓
DB 状态更新失败
```

**缺陷一：`updateEventListenResult` 异常被吞掉**

```java
// EventListenCallbackImpl.java
private void updateEventListenResult(Long id, int index) {
    try {
        eventListenDAO.updateEventListenResult(id, index);
    } catch (Exception e) {
        log.error("...");
        // ← 异常消失，handle0 以为一切正常，但 DB 中位图未清零
    }
}
```

结果：补偿任务扫表后，认为该 listener 未完成，重新触发，listener **被重复执行**。

**缺陷二：`handle0` 的异常处理混用了两种失败语义**

```java
// EventListenHandlerImpl.java
try {
    DomainEventListenSupport.execute(name, dto.getParam());  // listener 执行
    callback.onHandleAfterSuccess();                         // DB 更新
} catch (Exception e) {
    callback.onHandleAfterFail(e.getMessage());              // ← 两种失败统一走这里
}
```

若 DB 更新抛出异常，会进入 catch 调用 `onHandleAfterFail`，把**已成功执行**的 listener 错误地标记为失败，后续补偿无限重试。

**根本原因：** 框架未明确声明消费语义，`listener 执行` 与 `DB 状态更新` 缺乏原子性保证。

**优化方案：**

**结论：采用 at-least-once + 幂等**（与 Kafka、RocketMQ 等主流 MQ 保持一致）。框架层明确声明语义，要求 listener 实现幂等。

**Step 1：分离两种异常，`handle0` 不能混用同一个 catch**

```java
// EventListenHandlerImpl.java
private void handle0(EventListenHandlerDTO dto) {
    boolean needHandle = DomainEventListenSupport.canExecute(dto.getName(), dto.getRetryTimes());
    if (!needHandle) {
        log.info("handle ignore,dto:{}", dto);
        return;
    }
    EventListenCallback callback = dto.getCallback();

    // Step1: 执行 listener，单独捕获
    try {
        DomainEventListenSupport.execute(dto.getName(), dto.getParam());
    } catch (Exception e) {
        log.error("listener execute error, dto:{}", dto, e);
        callback.onHandleAfterFail(e.getMessage());
        return;
    }

    // Step2: listener 已成功，更新 DB 状态，不能再调 onHandleAfterFail
    try {
        callback.onHandleAfterSuccess();
    } catch (Exception e) {
        // listener 已成功执行，DB 更新失败 —— 保持位图不变，等补偿任务重试
        // listener 需保证幂等
        log.error("listener [{}] executed successfully but DB state update failed. " +
                  "Will be retried by compensation task. " +
                  "Listener MUST be idempotent!", dto.getName(), e);
    }
}
```

**Step 2：`updateEventListenResult` 异常向上抛出，不吞掉**

```java
private void updateEventListenResult(Long id, int index) {
    boolean result = eventListenDAO.updateEventListenResult(id, index);
    if (!result) {
        throw new IllegalStateException(
            "updateEventListenResult failed, id:" + id + ", index:" + index);
    }
}
```

**Step 3：`@EventListen` 注解 Javadoc 明确声明幂等要求**

```java
/**
 * 框架保证 at-least-once 投递语义。
 * 网络抖动、JVM 重启、DB 更新失败等场景下，同一事件可能被重复投递。
 * 被标注的方法必须保证幂等性（推荐以 eventId 作为幂等键）。
 */
public @interface EventListen { ... }
```

**Step 4（可选，高可靠场景）：事务耦合实现 exactly-once**

适用条件：listener 与 ddc-client 使用同一个 DataSource，且 listener 方法本身加了 `@Transactional`。

需将 `DomainEventListenSupport.execute` 从 `method.invoke`（直接反射）改为通过 Spring AOP 代理调用，Spring 事务才能生效，将 listener 执行和 DB 位图更新包在同一个事务里，任何一步失败整体回滚。这属于 P3 级别改动，需单独排期评估。

**关于异步并发位图竞争（原问题 6）：** 采用 at-least-once 方案后，`isDispatched` 判断依赖内存快照不准确的问题依然存在。修复方式是在异步回调中不依赖内存 `listenResult`，每次从 DB 读最新值：

```java
@Override
public void onHandleAfterSuccess() {
    updateEventListenResult(eventListen.getId(), index);
    // 从 DB 读最新值判断是否全部完成，而非依赖内存快照
    EventListenDO latest = eventListenDAO.queryEventListenById(eventListen.getId());
    if (latest != null && latest.getListenResult() == 0) {
        log.info("all listeners finished, eventId:{}", eventListen.getEventId());
    }
    DomainEventPostProcessorManager.postProcessAfterListen(eventListen, name, params, Result.success());
}
```

---

#### 问题 7：`DdcTaskExecutor` 线程池配置硬编码，缺少监控

**文件：** `util/DdcTaskExecutor.java`

**问题描述：**
- `corePoolSize=10`、队列容量 200 写死，无法根据业务量调整
- `CallerRunsPolicy`：队列满时阻塞业务线程，影响接口响应时间
- 无线程池监控指标，队列积压无感知

**优化方案：**

```java
public class DdcTaskExecutor {

    // 支持外部配置注入
    private static volatile ThreadPoolExecutor executor;

    public static void init(int coreSize, int maxSize, int queueCapacity) {
        executor = new ThreadPoolExecutor(
            coreSize, maxSize, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queueCapacity),
            new ThreadFactory() {
                private final AtomicInteger count = new AtomicInteger(0);
                @Override public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "ddc-executor-" + count.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            },
            (r, exec) -> {
                // 队列满时告警，不阻塞业务线程
                log.error("DdcTaskExecutor queue full! task rejected. queueSize:{}", exec.getQueue().size());
                DomainEventPostProcessorManager.onExecutorRejected();
            }
        );
    }

    // 暴露监控指标（可接入 Micrometer）
    public static int getQueueSize() { return executor.getQueue().size(); }
    public static long getCompletedTaskCount() { return executor.getCompletedTaskCount(); }
}
```

---

#### 问题 8：`errorInfo` 字段无长度保护，反复重试后可能超长

**文件：** `com/listen/dao/impl/EventListenDAOImpl.java:114`

**问题描述：**
代码注释已标注 `TODO 防止字符串过大`。每次失败都把堆栈信息追加拼接，字段长度无上限，数据库字段超长会导致更新失败，掩盖真实错误。

**优化方案：**
在 SQL 层直接处理截断（见问题 2 修复方案），或在 Java 层限制长度：

```java
private String composeErrorInfo(String oldErrorInfo, String newError) {
    String truncated = StringUtils.abbreviate(newError, 500); // 单条最多 500 字符
    String combined = StringUtils.defaultString(oldErrorInfo) + "，" + truncated;
    return StringUtils.abbreviate(combined, 2000); // 总长度不超过 2000
}
```

---

#### 问题（P2 补充 A）：`@EventListen.order` 定义了但从未参与排序

**文件：** `com/listen/support/DomainEventListenSupport.java:140`

**问题描述：**
注解中定义了 `int order() default 0`，语义是"小的先执行"，但注册监听器时直接按 Spring Bean 扫描顺序追加 name 到列表，没有基于 `order` 排序，执行顺序不可预测且与注解语义不符。

**修复方案：**
注册完所有 Bean 后，对每个 `uniKey` 对应的 listener 列表排序：

```java
// 在 registerDomainEventListen 之后、domainEventUniqueKeyMap 构建完成后统一排序
domainEventUniqueKeyMap.forEach((uniKey, nameList) ->
    nameList.sort(Comparator.comparingInt(name ->
        domainEventListenMap.get(name).getEventListen().order()
    ))
);
```

更稳的做法是在所有 Bean 扫描完成后（`ApplicationContext` refresh 结束）再建索引，避免扫描顺序影响排序结果。

---

#### 问题（P2 补充 B）：启动时遍历所有 Bean 并调 `getBean()`，可能触发过早初始化

**文件：** `com/DomainEventApplicationContext.java:63`

**问题描述：**
```java
for (String bean : beanDefinitionNames) {
    Object obj = applicationContext.getBean(bean);  // ← 主动触发所有 Bean 初始化
    ...
}
```
这会实例化所有懒加载 Bean，可能改变 Spring 的 Bean 初始化顺序，引发循环依赖或某些 Bean 依赖尚未准备好时被提前构造。

**优化方案：**
改为实现 `BeanPostProcessor`，由 Spring 在正常初始化流程中回调，而非主动触发：

```java
@Component
public class EventListenBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        Map<Method, EventListen> annotatedMethods = MethodIntrospector.selectMethods(
            targetClass,
            (MethodIntrospector.MetadataLookup<EventListen>) method ->
                AnnotationUtils.findAnnotation(method, EventListen.class)
        );
        annotatedMethods.forEach((method, annotation) -> {
            Method proxiedMethod = AopUtils.getMostSpecificMethod(method, bean.getClass());
            DomainEventListenSupport.registerDomainEventListen(annotation, proxiedMethod, bean);
        });
        return bean;
    }
}
```

---

#### 问题（P2 补充 C）：代理解包依赖 Spring 内部字段，版本升级有兼容性风险

**文件：** `util/AnnotationUtil.java`

**问题描述：**
当前代码通过反射读取 `CGLIB$CALLBACK_0`、`advised`、`h` 等字段来解包 AOP 代理，这些是 Spring AOP / JDK Proxy 的内部实现细节，Spring 版本升级后字段名或结构变化会导致静默失效甚至抛出异常。

**优化方案：**
替换为 Spring 提供的工具方法：

```java
// 获取目标类
Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);

// 获取最具体的方法（处理桥接方法）
Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
Method bridgedMethod  = BridgeMethodResolver.findBridgedMethod(specificMethod);
```

---

#### 问题（P2 补充 D）：`notifyMethod` 定义但从未真正生效

**文件：** `com/annotation/EventNotify.java`、`com/EventNotifyAspect.java`、`com/notify/DomainEventNotifyDTO.java`

**问题描述：**
`@EventNotify` 定义了 `NotifyMethodEnum notifyMethod()`，但 `buildDomainEventNotifyDTO()` 没有传递该字段，`DomainEventNotifyDTO` 也没有 `notifyMethod` 属性，`EventDO.notifyType` 始终为 null，`EventNotifyServiceImpl` 中的远程 notify 分支是 TODO。注解 API 与实际行为不一致，对使用方有误导性。

**优化方案（补全字段传递链）：**

```java
// DomainEventNotifyDTO 增加字段
private NotifyMethodEnum notifyMethod;

// EventNotifyAspect.buildDomainEventNotifyDTO 中传递
dto.setNotifyMethod(annotation.notifyMethod());

// DomainEventNotifyInstance.buildEventDO 中写入
event.setNotifyType(domainEventNotify.getNotifyMethod().name());

// EventNotifyServiceImpl.sendNotify 中分支
switch (NotifyMethodEnum.valueOf(event.getNotifyType())) {
    case local:         return localSendNotify(dto);
    case remote:        return remoteSendNotify(dto);  // TODO 实现远程
}
```

在远程 notify 实现完成之前，若配置了 `remote`，应在启动时或运行时明确抛出 `UnsupportedOperationException`，而非静默走本地路径。

---

### P3 — 架构优化（中长期规划）

---

#### 问题 9：`MyDataSourceTransactionManager` 侵入性强，兼容性差

**问题描述：**
接入方必须完全替换 `PlatformTransactionManager`，无法与 ShardingSphere、动态数据源、Seata 等框架共存；已有自定义事务管理器的系统迁移成本高。

**优化方案：**
改为利用 Spring 原生的 `TransactionSynchronizationManager` 注册事务同步回调，彻底解耦：

```java
// 在 AOP 的 doAround 中，替代 lifecycle.start/end 的方式
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
    @Override
    public void beforeCommit(boolean readOnly) {
        // 在事务提交前，用当前连接写入消息表
        Connection conn = DataSourceUtils.getConnection(dataSource);
        pointcutInstance.setParam(CONNECTION, conn);
        pointcutInstance.createEvent();
    }

    @Override
    public void afterCommit() {
        // 事务提交后异步分发
        DdcTaskExecutor.execute(() -> eventNotifyService.eventNotify(event));
    }

    @Override
    public void afterCompletion(int status) {
        if (status == STATUS_ROLLED_BACK) {
            pointcutInstance.onRolledBack();
        }
        PointcutManager.clear();
    }
});
```

---

#### 问题 10：`listenResult` 位图上限 63 个 listener

**问题描述：**
`Long` 类型 64 位，符号位不可用，单个 domain+event 最多支持 63 个 listener。超出时 `getInitListenResult` 溢出为负数，`isDispatched` 逻辑错乱。

**优化方案（短期）：** 注册时校验，超过 63 个直接启动失败并给出明确提示。

```java
// DomainEventListenSupport.registerDomainEventListen 中新增
if (nameList.size() >= 63) {
    throw new IllegalStateException(
        "Too many listeners for domain=" + domain + ", event=" + event +
        ". Maximum is 63, current=" + nameList.size());
}
```

**优化方案（长期）：** 将执行状态改为按 listener name 存储的 JSON 结构，消除位图限制，同时可读性更好：

```json
// listenResult 改为 JSON 字符串
{"listenerA": "success", "listenerB": "pending", "listenerC": "failed"}
```

---

#### 问题（P3 补充）：静态单例和静态 `ThreadLocal` 较多，存在上下文污染风险

**文件：** `DomainEventApplicationContext`、`PointcutManager`、`DomainEventListenSupport`、`DomainEventPostProcessorManager`

**问题描述：**

| 类 | 静态状态 |
|---|---|
| `DomainEventApplicationContext` | `static instance` |
| `PointcutManager` | `static ThreadLocal<PointcutInstance>`、`static ThreadLocal<Map>` |
| `DomainEventListenSupport` | listener 注册表、service 引用 |
| `DomainEventPostProcessorManager` | postProcessor 列表 |

风险：
- 同一 JVM 存在多个 Spring `ApplicationContext` 时互相污染（如集成测试）
- 单测之间状态残留，测试顺序不同结果不同
- Web 容器热部署时 classloader 泄漏
- `ThreadLocal` 如果 `clear()` 调用路径不完整，在线程池复用时会泄漏上下文

**优化方向：**
将 listener 注册表和 postProcessor 列表收敛为 Spring Bean，由容器管理生命周期；`ThreadLocal` 清理逻辑收归 `finally` 块，确保任何路径（正常/异常）均会清理。

---

## 三、优先级汇总

| 优先级 | # | 问题描述 | 影响范围 |
|---|---|---|---|
| **P0** | 1 | `notify_id` 字段写入错误值（`EventDAOImpl`） | 数据错误，影响追踪链路 |
| **P0** | 2 | `updateErrorInfo` 按错误字段查询（`EventListenDAOImpl`） | 错误信息无法正确记录 |
| **P0** | 3 | `doCommit` 未判 null 导致 NPE（`MyDataSourceTransactionManager`） | 线上事故，所有事务受影响 |
| **P0** | 4 | `gmt_create` 插入时使用了 `gmtEvent`（`InsertEventDAOImpl`） | 时间字段语义错误，数据脏 |
| **P1** | 5 | 缺少补偿轮询任务（`DdcCompensateService` 已预留接口） | 服务重启后消息可能永久滞留 |
| **P1** | 6 | 无事务场景消息静默丢失 | 误用场景无任何报错提示 |
| **P1** | 7 | listener 执行成功但 DB 状态更新失败时会重复执行 | at-least-once 语义未明确，幂等要求未声明 |
| **P1** | 8 | `ddc_event_listen` 先查后插缺少唯一约束，并发可重复消费 | 数据一致性风险 |
| **P1** | 9 | 缺少 DDL 脚本 | 接入方难以正确建表 |
| **P2** | 10 | `@EventListen.order` 未参与实际排序 | 执行顺序不可控，与注解语义不符 |
| **P2** | 11 | 异步 listener 并发时位图判断依赖内存快照 | 异步场景下可能重复派发 |
| **P2** | 12 | 线程池硬编码、无线程名、无监控 | 高负载下影响稳定性和可观测性 |
| **P2** | 13 | `errorInfo` 字段无长度保护 | 反复重试后字段超长写 DB 失败 |
| **P2** | 14 | 启动扫描调 `getBean()` 可能触发 Bean 过早初始化 | 可能改变 Spring 初始化顺序 |
| **P2** | 15 | 代理解包依赖 Spring 内部字段，版本升级有兼容性风险 | Spring 升级后静默失效 |
| **P2** | 16 | `notifyMethod` 定义但从未真正生效，API 与行为不一致 | 对使用方有误导性 |
| **P3** | 17 | `MyDataSourceTransactionManager` 侵入性强，兼容性差 | 多框架共存时接入困难 |
| **P3** | 18 | `listenResult` 位图上限 53/63 个 listener | 超限后溢出，大型项目有天花板 |
| **P3** | 19 | 静态单例和静态 `ThreadLocal` 较多，多上下文/单测有污染风险 | 集成测试、热部署稳定性 |

---

## 四、建议迭代计划

### 第一期（1 周内）— 先保证不出错

- 修复 P0 四个 Bug（#1 notify_id、#2 updateErrorInfo、#3 NPE、#4 gmt_create）
- `@EventNotify` 无 `@Transactional` 时启动/运行时明确报错（#6 前置防护）
- 补充最小 DDL，包含两张表 + `uk_event_id` 唯一索引（#9）
- `listenResult` 位图上限超过 53 时启动报错（#18 短期防护）

### 第二期（2～4 周）— 补齐可靠性闭环

- 业务方接入 `DdcCompensateService`，挂载到外部调度器（#5）
- 明确声明 at-least-once 语义，`handle0` 分离两段 try-catch（#7）
- `ddc_event_listen` 改为捕获 `DuplicateKeyException` 实现幂等（#8）
- 线程池配置化 + 自定义线程名 + 拒绝告警（#12）
- `errorInfo` SQL 层截断（#13）
- `@EventListen.order` 排序生效（#10）

### 第三期（1～2 个月）— 降低接入成本

- 替换启动扫描为 `BeanPostProcessor`，避免过早 `getBean()`（#14）
- 代理解包改用 `AopProxyUtils` / `AopUtils`（#15）
- `notifyMethod` 完整传递，remote 分支明确抛 `UnsupportedOperationException`（#16）
- 去掉对 `MyDataSourceTransactionManager` 的强依赖，改用 `TransactionSynchronizationManager`（#17）

### 第四期（中长期）— 提升中间件成熟度

- `listenResult` 改为每 listener 一行的表结构，彻底消除位图限制（#18）
- 静态管理器收敛为 Spring Bean，消除多上下文污染风险（#19）
- 拆分 `ddc-client-core` / `ddc-client-spring` / `ddc-client-spring-boot-starter` 模块
- 引入 Flyway/Liquibase 管理 DDL 演化
- 增加完整单元测试和基于 Testcontainers 的集成测试

---

## 五、推荐的最终接入形态

理想情况下，业务侧使用方式应收敛为：

```java
// 启动类
@EnableDdcClient
@SpringBootApplication
public class Application {}
```

```properties
# 配置
ddc.executor.core-size=10
ddc.executor.queue-capacity=1000
ddc.event.require-transaction=true
```

```java
// 发布事件
@EventNotify(domain = "order", event = "paid")
@Transactional(rollbackFor = Exception.class)
public void pay(OrderPayCommand cmd) {
    orderRepository.markPaid(cmd.getOrderId());
    DomainEventNotifyContext.put(DomainEventNotifyContext.ENTITY_ID, cmd.getOrderId());
}

// 订阅事件（必须幂等）
@EventListen(listenDomain = "order", listenEvent = "paid", order = 10)
public void onOrderPaid(@EventParam(name = "entityId") String orderId) {
    // 业务逻辑，以 orderId 为幂等键防重
}
```

框架底层保证：

| 能力 | 目标 |
|---|---|
| 事务一致性 | 业务提交和事件落库原子完成 |
| 至少一次投递 | pending/failed 事件可补偿重投 |
| 幂等消费保障 | 同一 eventId 只落 ddc_event_listen 一次（唯一约束兜底） |
| 可观测性 | 状态、重试次数、积压量、失败原因均可查 |
| 低侵入接入 | 不替换业务事务管理器 |
