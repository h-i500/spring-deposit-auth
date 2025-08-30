#!/usr/bin/env bash
set -euo pipefail

BASE="http://localhost:8000"
CJ="${1:-.cookies.txt}"            # cookie jar path (default: .cookies.txt)
USER="${USERNAME:-testuser}"       # override by env USERNAME
PASS="${PASSWORD:-password}"       # override by env PASSWORD

# keycloak:8080 は Docker の内部ホスト名なので、ホスト側からは --resolve で到達させる
RESOLVE="--resolve keycloak:8080:127.0.0.1"

rm -f "$CJ"

# 1) /secure にアクセスして 302 Location を拾う
AUTH_URL=$(
  curl -s -i -c "$CJ" -H 'Accept: text/html' "$BASE/secure" \
  | tr -d '\r' | awk 'tolower($1)=="location:"{print $2; exit}'
)
[[ -n "$AUTH_URL" ]] || { echo "failed: AUTH_URL not found"; exit 1; }

# 2) ログインフォームの action を抽出
LOGIN_HTML=$(curl -s -L -c "$CJ" -b "$CJ" $RESOLVE "$AUTH_URL")
ACTION_URL=$(
  echo "$LOGIN_HTML" | tr '\n' ' ' \
  | sed -n 's/.*id="kc-form-login"[^>]*action="\([^"]*\)".*/\1/p'
)
[[ -n "$ACTION_URL" ]] || { echo "failed: ACTION_URL not found"; exit 1; }

# 3) 認証 POST（credentialId は空でOK）
curl -s -i -L -c "$CJ" -b "$CJ" $RESOLVE \
  -X POST \
  -d "username=$USER" \
  -d "password=$PASS" \
  -d "credentialId=" \
  "$ACTION_URL" > /dev/null

# 4) ログイン後の保護エンドポイントを確認
echo "== /secure/me =="
curl -i -b "$CJ" "$BASE/secure/me"
