#!/usr/bin/env bash
set -euo pipefail

# ==================================================
# 説明：「定期解約時に普通預金へ自動振替」のテストスクリプト
# ==================================================


# =========================
# Config (環境変数で上書き可)
# =========================
SAVINGS_HOST="${SAVINGS_HOST:-http://localhost:8081}"
TD_HOST="${TD_HOST:-http://localhost:8082}"

OWNER_SAVINGS="${OWNER_SAVINGS:-Taro}"
OWNER_TD="${OWNER_TD:-Hanako}"
DEPOSIT_AMOUNT="${DEPOSIT_AMOUNT:-20000}"     # 普通預金に最初に入金
TD_PRINCIPAL="${TD_PRINCIPAL:-10000}"         # 定期に振り替える元本
TD_RATE="${TD_RATE:-0.015}"                   # 年率 (1.5%)
TD_DAYS="${TD_DAYS:-30}"                      # 期間（日）

CURL="${CURL:-curl}"
JQ="${JQ:-jq}"

# =========================
# Helpers
# =========================
red()   { printf '\033[31m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
cyan()  { printf '\033[36m%s\033[0m\n' "$*"; }
bold()  { printf '\033[1m%s\033[0m\n' "$*"; }

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || { red "Missing command: $1"; exit 1; }
}

http_post_json() {
  # $1=url, $2=json
  ${CURL} -sS -w '\n%{http_code}' -X POST "$1" \
    -H "Content-Type: application/json" \
    -d "$2"
}

http_get() {
  # $1=url
  ${CURL} -sS "$1"
}

float_equal() {
  # $1=a, $2=b, $3=eps
  awk -v a="$1" -v b="$2" -v e="${3:-0.005}" 'BEGIN{d=a-b; if (d<0) d=-d; exit (d>e)}'
}

# =========================
# Precheck
# =========================
need_cmd "${CURL}"
need_cmd "${JQ}"

bold "==> Checking services"
${CURL} -sS "${SAVINGS_HOST}/actuator/health" >/dev/null 2>&1 || true
${CURL} -sS "${TD_HOST}/actuator/health" >/dev/null 2>&1 || true
cyan "SAVINGS_HOST = ${SAVINGS_HOST}"
cyan "TD_HOST      = ${TD_HOST}"

# =========================
# 1) Create savings account
# =========================
bold "==> 1) Create savings account (owner=${OWNER_SAVINGS})"
ACC_RESP="$(http_post_json "${SAVINGS_HOST}/accounts" "{\"owner\":\"${OWNER_SAVINGS}\"}")"
ACC_STATUS="$(printf '%s\n' "${ACC_RESP}" | tail -n1)"
ACC_JSON="$(printf '%s\n' "${ACC_RESP}" | sed '$d')"

echo "${ACC_JSON}" | ${JQ} '.' || true
if [ "${ACC_STATUS}" != "200" ]; then
  red "Failed to create savings account. Status=${ACC_STATUS}"
  exit 1
fi
ACC_ID="$(echo "${ACC_JSON}" | ${JQ} -r '.id')"
[ -n "${ACC_ID}" ] || { red "Account ID not found"; exit 1; }
green "Created savings account: ${ACC_ID}"

# =========================
# 2) Deposit initial amount
# =========================
bold "==> 2) Deposit initial amount = ${DEPOSIT_AMOUNT}"
DEP_JSON="$(http_post_json "${SAVINGS_HOST}/accounts/${ACC_ID}/deposit" "{\"amount\":${DEPOSIT_AMOUNT}}")"
DEP_STATUS="$(printf '%s\n' "${DEP_JSON}" | tail -n1)"
DEP_BODY="$(printf '%s\n' "${DEP_JSON}" | sed '$d')"
echo "${DEP_BODY}" | ${JQ} '.' || true
if [ "${DEP_STATUS}" != "200" ]; then
  red "Failed to deposit. Status=${DEP_STATUS}"
  exit 1
fi

BAL_BEFORE_CLOSE="$(http_get "${SAVINGS_HOST}/accounts/${ACC_ID}" | ${JQ} -r '.balance')"
cyan "Savings balance (before close): ${BAL_BEFORE_CLOSE}"

# =========================
# 3) Open time deposit
# =========================
bold "==> 3) Open time deposit (principal=${TD_PRINCIPAL}, rate=${TD_RATE}, days=${TD_DAYS})"
TD_RESP="$(http_post_json "${TD_HOST}/deposits" "{\"owner\":\"${OWNER_TD}\",\"principal\":${TD_PRINCIPAL},\"annualRate\":${TD_RATE},\"termDays\":${TD_DAYS}}")"
TD_STATUS="$(printf '%s\n' "${TD_RESP}" | tail -n1)"
TD_JSON="$(printf '%s\n' "${TD_RESP}" | sed '$d')"
echo "${TD_JSON}" | ${JQ} '.' || true
if [ "${TD_STATUS}" != "200" ]; then
  red "Failed to open time deposit. Status=${TD_STATUS}"
  exit 1
fi
TD_ID="$(echo "${TD_JSON}" | ${JQ} -r '.id')"
TD_MATURITY="$(echo "${TD_JSON}" | ${JQ} -r '.maturityDate')"
[ -n "${TD_ID}" ] || { red "Time deposit ID not found"; exit 1; }
[ -n "${TD_MATURITY}" ] || { red "Maturity date not found"; exit 1; }
green "Opened time deposit: ${TD_ID}"
cyan  "Maturity: ${TD_MATURITY}"

# =========================
# 4) Close TD with auto-transfer to savings
# =========================
bold "==> 4) Close time deposit (auto-transfer to savings)"
CLOSE_URL="${TD_HOST}/deposits/${TD_ID}/close?toAccountId=${ACC_ID}&at=${TD_MATURITY}"
CLOSE_RESP="$( ${CURL} -sS -w '\n%{http_code}' -X POST "${CLOSE_URL}" )"
CLOSE_STATUS="$(printf '%s\n' "${CLOSE_RESP}" | tail -n1)"
CLOSE_JSON="$(printf '%s\n' "${CLOSE_RESP}" | sed '$d')"
echo "${CLOSE_JSON}" | ${JQ} '.' || true
if [ "${CLOSE_STATUS}" != "200" ]; then
  red "Failed to close time deposit. Status=${CLOSE_STATUS}"
  exit 1
fi
PAYOUT="$(echo "${CLOSE_JSON}" | ${JQ} -r '.payout')"
[ -n "${PAYOUT}" ] || { red "Payout not found"; exit 1; }
green "Closed TD. Payout=${PAYOUT}"

# =========================
# 5) Verify results
# =========================
bold "==> 5) Verify balances and TD state"

BAL_AFTER_CLOSE="$(http_get "${SAVINGS_HOST}/accounts/${ACC_ID}" | ${JQ} -r '.balance')"
TD_STATE_JSON="$(http_get "${TD_HOST}/deposits/${TD_ID}")"
TD_STATUS_VALUE="$(echo "${TD_STATE_JSON}" | ${JQ} -r '.status')"

cyan "Savings balance (after close):  ${BAL_AFTER_CLOSE}"
cyan "TD status: ${TD_STATUS_VALUE}"

# (balance_after - balance_before) ~= payout を確認（誤差 0.01 以内）
DELTA="$(awk -v a="${BAL_AFTER_CLOSE}" -v b="${BAL_BEFORE_CLOSE}" 'BEGIN{printf "%.2f", (a-b)}')"
if float_equal "${DELTA}" "${PAYOUT}" 0.02; then
  green "OK: (after - before) == payout (${DELTA} ~= ${PAYOUT})"
else
  red "NG: (after - before) != payout (delta=${DELTA}, payout=${PAYOUT})"
  exit 1
fi

if [ "${TD_STATUS_VALUE}" != "CLOSED" ]; then
  red "NG: TD status should be CLOSED but was ${TD_STATUS_VALUE}"
  exit 1
fi

bold "==> SUCCESS 🎉  All checks passed."
echo "Account: ${ACC_ID}"
echo "TD:      ${TD_ID}"
echo "Payout:  ${PAYOUT}"
