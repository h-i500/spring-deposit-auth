#!/usr/bin/env bash
set -euo pipefail

REALM_BASE="http://localhost:8080/realms/demo-realm"
CLIENT_ID="mashup-cli-public"
SCOPE="openid"

AUTH_DEVICE_URL="${REALM_BASE}/protocol/openid-connect/auth/device"
TOKEN_URL="${REALM_BASE}/protocol/openid-connect/token"

# --- PKCE (S256) : Git Bash でも安定 ---
CODE_VERIFIER=$(openssl rand -base64 96 | tr '+/' '-_' | tr -d '=[:space:]' | cut -c1-64)
CODE_CHALLENGE=$(printf '%s' "$CODE_VERIFIER" \
  | openssl dgst -binary -sha256 \
  | base64 | tr '+/' '-_' | tr -d '=')

# 1) デバイスコード発行
tmp_body=$(mktemp)
http_code=$(
  curl -sS -o "$tmp_body" -w '%{http_code}' -X POST \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "client_id=${CLIENT_ID}" \
    -d "scope=${SCOPE}" \
    -d "code_challenge_method=S256" \
    -d "code_challenge=${CODE_CHALLENGE}" \
    "${AUTH_DEVICE_URL}"
)
resp="$(cat "$tmp_body")"
rm -f "$tmp_body"

# デバッグ表示（失敗時に状況が見えるように）
echo "[device auth] HTTP $http_code"
[[ -n "$resp" ]] && echo "$resp" | jq . || echo "(empty body)"

# エラーチェック
if [[ "$http_code" != "200" ]]; then
  echo "device authorization failed"
  exit 1
fi

device_code=$(echo "$resp" | jq -r '.device_code')
user_code=$(echo "$resp" | jq -r '.user_code')
verification_uri=$(echo "$resp" | jq -r '.verification_uri')
verification_uri_complete=$(echo "$resp" | jq -r '.verification_uri_complete // empty')
interval=$(echo "$resp" | jq -r '.interval // 5')

echo "ブラウザで開いて認可してください:"
if [[ -n "$verification_uri_complete" && "$verification_uri_complete" != "null" ]]; then
  echo "  $verification_uri_complete"
else
  echo "  ${verification_uri}?user_code=${user_code}"
fi

# 2) トークン取得
sleep "${interval}"
while :; do
  tmp_tok=$(mktemp)
  http_code=$(
    curl -sS -o "$tmp_tok" -w '%{http_code}' -X POST \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "grant_type=urn:ietf:params:oauth:grant-type:device_code" \
      -d "device_code=${device_code}" \
      -d "client_id=${CLIENT_ID}" \
      -d "code_verifier=${CODE_VERIFIER}" \
      "${TOKEN_URL}"
  )
  tok="$(cat "$tmp_tok")"
  rm -f "$tmp_tok"

  err=$(echo "$tok" | jq -r '.error // empty')
  if [[ "$http_code" == "200" && ( -z "$err" || "$err" == "null" ) ]]; then
    ACCESS_TOKEN=$(echo "$tok" | jq -r '.access_token')
    export ACCESS_TOKEN
    echo "アクセストークン取得 OK"
    break
  fi
  case "$err" in
    authorization_pending) sleep "${interval}" ;;
    slow_down)             sleep $((interval + 5)) ;;
    access_denied)         echo "認可が拒否されました"; exit 2 ;;
    expired_token)         echo "デバイスコードが失効しました"; exit 3 ;;
    *)                     echo "[token poll] HTTP $http_code"; echo "$tok" | jq .; exit 4 ;;
  esac
done

# 3) iss を確認（Kong の iss マッチ用）
PAYLOAD=$(cut -d. -f2 <<< "$ACCESS_TOKEN")
rem=$(( ${#PAYLOAD} % 4 )); PAD=""
[[ $rem -eq 2 ]] && PAD="=="
[[ $rem -eq 3 ]] && PAD="="
[[ $rem -eq 1 ]] && PAD="==="
ISS=$(printf '%s' "$PAYLOAD$PAD" | tr '_-' '/+' | base64 -d 2>/dev/null | jq -r .iss)
echo "iss = $ISS"

# 4) Kong 経由で呼ぶ
curl -i -H "Authorization: Bearer $ACCESS_TOKEN" \
  "http://localhost:8000/balance-inquiry/demo"
