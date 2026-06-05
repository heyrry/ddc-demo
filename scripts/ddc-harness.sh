#!/usr/bin/env bash
# =============================================================================
# ddc-harness.sh
# 每次 ddc-client 提交后自动触发：构建 → 集成测试 → AI 分析 → 输出报告
# 由 .git/hooks/post-commit 在后台调用，也可手动执行。
# =============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPORT="$REPO_ROOT/ddc-test-report.txt"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

# ---------- 工具函数 ----------------------------------------------------------
section() { echo ""; echo "══════════════════════════════════════════"; echo "  $1"; echo "══════════════════════════════════════════"; }
ok()      { echo "  ✅  $1"; }
fail()    { echo "  ❌  $1"; }

# ---------- 初始化报告 --------------------------------------------------------
{
echo "DDC Harness Report"
echo "Generated : $TIMESTAMP"
echo "Commit    : $(git -C "$REPO_ROOT" log -1 --oneline 2>/dev/null || echo 'unknown')"
echo "Branch    : $(git -C "$REPO_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
} | tee "$REPORT"

# ---------- 1. 变更文件列表 ---------------------------------------------------
section "Changed Files in ddc-client" | tee -a "$REPORT"
git -C "$REPO_ROOT" diff HEAD~1 HEAD --name-only -- ddc-client/src/ 2>/dev/null \
    | tee -a "$REPORT" \
    || echo "  (首次提交，无对比基准)" | tee -a "$REPORT"

# ---------- 2. 构建 ddc-client ------------------------------------------------
section "Build: ddc-client install" | tee -a "$REPORT"
cd "$REPO_ROOT"
if mvn install -pl ddc-client -am -q 2>&1 | tee -a "$REPORT"; then
    ok "Build PASSED" | tee -a "$REPORT"
else
    fail "Build FAILED — 跳过测试，直接进入 AI 分析" | tee -a "$REPORT"
    section "AI Analysis" | tee -a "$REPORT"
    bash "$REPO_ROOT/scripts/ai-analyze.sh" "$REPORT" | tee -a "$REPORT"
    exit 1
fi

# ---------- 3. 集成测试 -------------------------------------------------------
section "Integration Tests: ddc-test" | tee -a "$REPORT"
TEST_EXIT=0
mvn test -pl ddc-test -q 2>&1 | tee -a "$REPORT" || TEST_EXIT=$?

# 追加 Surefire 文本报告（每个测试方法的详细结果）
SUREFIRE="$REPO_ROOT/ddc-test/target/surefire-reports"
if [ -d "$SUREFIRE" ]; then
    section "Surefire Detail" | tee -a "$REPORT"
    cat "$SUREFIRE"/*.txt 2>/dev/null | tee -a "$REPORT" || true
fi

if [ "$TEST_EXIT" -eq 0 ]; then
    ok "All tests PASSED" | tee -a "$REPORT"
else
    fail "Tests FAILED (exit=$TEST_EXIT)" | tee -a "$REPORT"
fi

# ---------- 4. AI 分析 --------------------------------------------------------
section "AI Analysis" | tee -a "$REPORT"
bash "$REPO_ROOT/scripts/ai-analyze.sh" "$REPORT" | tee -a "$REPORT"

# ---------- 5. 最终汇总 -------------------------------------------------------
section "Summary" | tee -a "$REPORT"
echo "  Report : $REPORT" | tee -a "$REPORT"
echo "  Result : $([ "$TEST_EXIT" -eq 0 ] && echo 'PASS ✅' || echo 'FAIL ❌')" | tee -a "$REPORT"

exit $TEST_EXIT
