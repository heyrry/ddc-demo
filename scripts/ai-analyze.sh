#!/usr/bin/env bash
# =============================================================================
# ai-analyze.sh  <report-file>
# 读取构建 + 测试报告，调用 Claude API 输出中文分析结论。
#
# 依赖：
#   - python3（标准库即可，用于 JSON 拼装和解析）
#   - curl
#   - 环境变量 ANTHROPIC_API_KEY
# =============================================================================
REPORT_FILE="${1:?Usage: ai-analyze.sh <report-file>}"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# ---------- 前置检查 ----------------------------------------------------------
if [ -z "${ANTHROPIC_API_KEY:-}" ]; then
    echo "  ⚠️  ANTHROPIC_API_KEY 未设置，跳过 AI 分析"
    echo "     设置方法: export ANTHROPIC_API_KEY=sk-ant-..."
    exit 0
fi

if ! command -v python3 &>/dev/null; then
    echo "  ⚠️  python3 未找到，跳过 AI 分析"
    exit 0
fi

# ---------- 收集上下文 --------------------------------------------------------
DIFF_FILE=$(mktemp)
PAYLOAD_FILE=$(mktemp)
RESPONSE_FILE=$(mktemp)
trap 'rm -f "$DIFF_FILE" "$PAYLOAD_FILE" "$RESPONSE_FILE"' EXIT

# git diff：本次提交涉及 ddc-client/src 的变更（最多 400 行）
git -C "$REPO_ROOT" diff HEAD~1 HEAD -- ddc-client/src/ 2>/dev/null \
    | head -400 > "$DIFF_FILE" \
    || true
# 首次提交时 diff 为空，改用 show
if [ ! -s "$DIFF_FILE" ]; then
    git -C "$REPO_ROOT" show HEAD -- ddc-client/src/ 2>/dev/null \
        | head -400 > "$DIFF_FILE" || true
fi

# ---------- 构建 JSON Payload（Python 负责转义） --------------------------------
python3 - "$DIFF_FILE" "$REPORT_FILE" "$PAYLOAD_FILE" <<'PYEOF'
import json, sys

diff_text    = open(sys.argv[1], encoding='utf-8', errors='replace').read()
report_text  = open(sys.argv[2], encoding='utf-8', errors='replace').read()
# 取报告最后 8000 字符，避免 token 超限
report_tail  = report_text[-8000:]

system_prompt = """\
你是 ddc-client 的测试分析专家。
ddc-client 是基于 Transactional Outbox 模式的本地消息表中间件，核心链路：
  @EventNotify + @Transactional
    → MyDataSourceTransactionManager.doCommit() 用同一 Connection 写 ddc_event
    → 事务提交后 DdcTaskExecutor 异步分发
    → LocalNotifyServiceImpl 写 ddc_event_listen
    → @EventListen 方法被调用

测试用例覆盖：
  TC-01 正常流程   TC-02 事务回滚   TC-03 多监听器位图
  TC-04 监听器异常  TC-05 幂等防重   TC-06 补偿任务\
"""

user_content = (
    f"## 本次代码变更\n```diff\n{diff_text}\n```\n\n"
    f"## 构建与测试结果\n```\n{report_tail}\n```\n\n"
    "请用中文输出以下四项分析（每项 2-4 句话）：\n\n"
    "### 1. 变更覆盖率\n"
    "本次变更的代码路径是否被测试用例覆盖？哪些路径未被覆盖？\n\n"
    "### 2. 失败分析\n"
    "若有测试失败，分析根本原因和修复方向。若全部通过，说明'无失败'。\n\n"
    "### 3. 遗漏场景\n"
    "基于本次变更，还有哪些边界条件或并发场景未被测试覆盖？\n\n"
    "### 4. 总体评估\n"
    "一行结论：PASS ✅ / WARN ⚠️ / FAIL ❌，并说明判断依据。"
)

payload = {
    "model": "claude-opus-4-8",
    "max_tokens": 1500,
    "system": system_prompt,
    "messages": [{"role": "user", "content": user_content}]
}

with open(sys.argv[3], 'w', encoding='utf-8') as f:
    json.dump(payload, f, ensure_ascii=False)
PYEOF

# ---------- 调用 Anthropic API ------------------------------------------------
HTTP_CODE=$(curl -s -o "$RESPONSE_FILE" -w "%{http_code}" \
    "https://api.anthropic.com/v1/messages" \
    -H "x-api-key: $ANTHROPIC_API_KEY" \
    -H "anthropic-version: 2023-06-01" \
    -H "content-type: application/json; charset=utf-8" \
    -d @"$PAYLOAD_FILE")

if [ "$HTTP_CODE" != "200" ]; then
    echo "  ❌  API 调用失败 (HTTP $HTTP_CODE)"
    cat "$RESPONSE_FILE"
    exit 1
fi

# ---------- 解析并输出结果 ----------------------------------------------------
python3 - "$RESPONSE_FILE" <<'PYEOF'
import json, sys
with open(sys.argv[1], encoding='utf-8') as f:
    r = json.load(f)
if 'content' in r:
    print(r['content'][0]['text'])
elif 'error' in r:
    print("  ❌  API Error:", r['error'].get('message', r))
else:
    print("  ❌  Unknown response:", json.dumps(r, ensure_ascii=False))
PYEOF
