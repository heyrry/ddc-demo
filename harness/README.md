# Harness CI 接入指南

## 文件说明

| 文件 | 用途 |
|---|---|
| `pipeline.yaml` | CI Pipeline 定义（构建 + Test Intelligence）|
| `trigger.yaml` | Push Trigger（ddc-client/src/** 变更时自动触发）|

---

## 接入步骤

### 前置条件

1. 代码已推送到 GitHub / GitLab
2. 已在 [app.harness.io](https://app.harness.io) 注册账号并创建 Project

### Step 1：创建 Connector

在 Harness UI → **Project Settings → Connectors** 中创建两个 Connector：

**① 代码仓库 Connector（GitHub 示例）**
- Type: `GitHub`
- 填写 GitHub 账号和 Personal Access Token（需要 `repo` 权限）
- 记下 Connector ID，填入 yaml 的 `<YOUR_REPO_CONNECTOR>`

**② DockerHub Connector**
- Type: `Docker Registry`
- URL: `https://index.docker.io/v2/`
- 填写 DockerHub 账号（拉取 `maven:3.8.6-openjdk-8-slim` 镜像）
- 记下 Connector ID，填入 yaml 的 `<YOUR_DOCKER_CONNECTOR>`

### Step 2：替换 YAML 占位符

编辑 `pipeline.yaml` 和 `trigger.yaml`，替换以下四个占位符：

```
<YOUR_ORG>              → Harness 组织 ID（Settings → Organization）
<YOUR_PROJECT>          → Harness 项目 ID（项目设置）
<YOUR_REPO_CONNECTOR>   → Step 1 创建的代码仓库 Connector ID
<YOUR_DOCKER_CONNECTOR> → Step 1 创建的 DockerHub Connector ID
```

### Step 3：导入 Pipeline

在 Harness UI → **Pipelines → + Create Pipeline → Import from Git** 或直接 **+ Create Pipeline → YAML** 粘贴 `pipeline.yaml` 内容。

### Step 4：导入 Trigger

在 Pipeline 详情页 → **Triggers → + New Trigger → Webhook → GitHub Push**，或在 Trigger 配置页选择 **YAML** 模式粘贴 `trigger.yaml`。

### Step 5：验证

```bash
# 修改任意 ddc-client/src/ 文件并推送
git add ddc-client/src/main/java/com/notify/dao/impl/EventDAOImpl.java
git commit -m "test: trigger harness ci"
git push origin master
```

推送后 30 秒内在 Harness UI → **Builds** 可看到新的 Pipeline 执行。

---

## Test Intelligence 工作原理

```
第 1 次运行：全量执行 6 个测试用例，建立基线
               ↓
提交 #2：仅改动 EventDAOImpl.java
  → Harness 分析 diff：只有 TC-01、TC-03 的执行路径受影响
  → 只运行 TC-01、TC-03（跳过 TC-02/04/05/06）
  → 节省约 66% 测试时间
               ↓
提交 #3：改动 DomainEventNotifyLifecycle.java
  → 影响路径更广 → 运行更多用例
               ↓
随着提交积累，TI 模型越来越准确
```

**查看 TI 分析报告**：Build 详情 → Tests → Test Intelligence 面板，可看到：
- 选中/跳过的用例列表
- 每个用例与代码变更的关联关系
- 历史趋势图（成功率、执行时间）

---

## 常见问题

**Q: 首次运行报错 `Cannot find ddc-client artifact`**
A: 第一步 `Build ddc-client` 会执行 `mvn install -pl ddc-client`，把 jar 安装到容器内的 `~/.m2`。由于启用了 Cache Intelligence，第二次运行开始会直接使用缓存。

**Q: Test Intelligence 第一次不会减少用例**
A: 正常。TI 需要至少一次全量运行来建立调用关系图（call graph）。

**Q: 如何在 PR 上也触发 CI**
A: 复制 `trigger.yaml`，把 `type: Push` 改为 `type: PullRequest`，`targetBranch` 改为目标分支（如 `master`），保存为 `trigger-pr.yaml`。
