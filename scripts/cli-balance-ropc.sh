#!/usr/bin/env bash
set -euo pipefail

# ---- 可変パラメータ ---------------------------------------------------------
# 既定は Docker ネットワーク内ホスト名。ホストから叩く場合は
# hosts に「127.0.0.1 keycloak」を追加するか、REALM_BASE を上書きしてください。
REALM_BASE="${REALM_BASE:-http://keycloak:8080/realms/demo-realm}"

CLIENT_ID="${CLIENT_ID:-mashup-cli-ropc}"

# 使い方: ./cli-balance-ropc.sh <username> <password> [client_secret]
USERNAME="${1:-testuser}"
PASSWORD="${2:-password}"

# 第3引数があればそれを、なければ環境変数 CLIENT_SECRET を使う。両方無ければ空。
CLIENT_SECRET="${3:-${CLIENT_SECRET:-}}"

# offline token を取得したいときは OFFLINE=1 を指定
OFFLINE="${OFFLINE:-0}"

# ---- 表示（秘密情報は出し過ぎない） ---------------------------------------
echo "[info] REALM_BASE=$REALM_BASE"
echo "[info] CLIENT_ID=$CLIENT_ID"
echo "[info] USERNAME=$USERNAME"
[[ -n "$CLIENT_SECRET" ]] && echo "[info] CLIENT_SECRET provided" || echo "[info] CLIENT_SECRET not set (public-like flow)"

# ---- 入力チェック -----------------------------------------------------------
if [[ -z "$PASSWORD" ]]; then
  echo "[error] PASSWORD が未設定です"; exit 1
fi

# ---- scope 構築 -------------------------------------------------------------
SCOPE="openid email profile"
if [[ "$OFFLINE" == "1" ]]; then
  SCOPE="$SCOPE offline_access"
fi

# ---- ROPC でトークン取得 ----------------------------------------------------
FORM=(
  -d grant_type=password
  -d client_id="$CLIENT_ID"
  -d username="$USERNAME"
  -d password="$PASSWORD"
  -d scope="$SCOPE"
)
if [[ -n "$CLIENT_SECRET" ]]; then
  FORM+=( -d client_secret="$CLIENT_SECRET" )
fi

TOK_JSON=$(curl -sS -X POST -H "Content-Type: application/x-www-form-urlencoded" \
  "${FORM[@]}" \
  "$REALM_BASE/protocol/openid-connect/token")

echo "$TOK_JSON" | jq .

ACCESS_TOKEN=$(jq -r '.access_token // empty' <<<"$TOK_JSON")
REFRESH_TOKEN=$(jq -r '.refresh_token // empty' <<<"$TOK_JSON")
if [[ -z "$ACCESS_TOKEN" || "$ACCESS_TOKEN" == "null" ]]; then
  echo "[error] トークン取得失敗"; exit 1
fi

printf '%s' "$ACCESS_TOKEN"  > .access_token
printf '%s' "$REFRESH_TOKEN" > .refresh_token 2>/dev/null || true
echo "export ACCESS_TOKEN=$(cat .access_token)" > .env.tokens
echo "[info] wrote .access_token / .env.tokens $( [[ "$OFFLINE" == "1" ]] && echo '/ .refresh_token' )"

# ---- iss チェック（issuer が keycloak になっているか） ----------------------
PAYLOAD=$(cut -d. -f2 <<< "$ACCESS_TOKEN")
PAD=$(( (4 - ${#PAYLOAD} % 4) % 4 )); PADSTR=$(printf '=%.0s' $(seq 1 $PAD))
ISS=$(printf '%s' "$PAYLOAD$PADSTR" | tr '_-' '/+' | base64 -d 2>/dev/null | jq -r .iss)
echo "[info] token.iss = $ISS"
if [[ "$ISS" != *"://keycloak:8080/realms/"* ]]; then
  echo "[warn] issuer が keycloak ではありません。Kong で検証に失敗する可能性があります。"
  echo "       REALM_BASE を http://keycloak:8080/realms/... にするか、hosts に 'keycloak' を追加してください。"
fi

# ---- owner をトークンから復元 ----------------------------------------------
OWNER=$(printf '%s' "$PAYLOAD$PADSTR" | tr '_-' '/+' | base64 -d 2>/dev/null | jq -r .preferred_username)
OWNER="${OWNER:-$USERNAME}"
echo "[info] owner = $OWNER"

# ---- 残高照会（Kong 経由） -------------------------------------------------
echo "---- call via Kong ----"
curl -i -H "Authorization: Bearer $ACCESS_TOKEN" \
  "http://localhost:8000/balance-inquiry/$OWNER"
