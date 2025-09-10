#!/usr/bin/env bash
set -euo pipefail
KC=${KC:-http://localhost:8080}
REALM=${REALM:-demo-realm}
CID=${CID:-mashup-cli-public}   # Public client（Device Grant 有効）
OWNER=${1:-alice}

# 1) デバイスコード要求
RESP=$(curl -s -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CID" \
  "$KC/realms/$REALM/protocol/openid-connect/auth/device")

VERIFY_URL=$(jq -r .verification_uri_complete <<<"$RESP")
DEVICE_CODE=$(jq -r .device_code <<<"$RESP")
# INTERVAL=$(jq -r '.interval // 2' <<<"$RESP")
INTERVAL=$(jq -r '.interval // 5' <<<"$RESP")

echo "ブラウザで開いて承認してください: $VERIFY_URL"
sleep "$INTERVAL"

# 2) トークン取得（承認までポーリング）
# while :; do
#   T=$(curl -s -X POST \
#     -d "grant_type=urn:ietf:params:oauth:grant-type:device_code" \
#     -d "device_code=$DEVICE_CODE" \
#     -d "client_id=$CID" \
#     "$KC/realms/$REALM/protocol/openid-connect/token")
#   ERR=$(jq -r .error <<<"$T" 2>/dev/null || echo)
#   if [[ -z "$ERR" || "$ERR" == "null" ]]; then TOKEN=$(jq -r .access_token <<<"$T"); break; fi
#   [[ "$ERR" == "authorization_pending" ]] && { sleep "$INTERVAL"; continue; }
#   echo "$T"; exit 1
# done

while :; do
  T=$(curl -s -X POST \
    -d "grant_type=urn:ietf:params:oauth:grant-type:device_code" \
    -d "device_code=$DEVICE_CODE" \
    -d "client_id=$CID" \
    "$KC/realms/$REALM/protocol/openid-connect/token")
  ERR=$(jq -r .error <<<"$T" 2>/dev/null || echo)
  case "$ERR" in
    null|"") TOKEN=$(jq -r .access_token <<<"$T"); break;;
    authorization_pending) sleep "$INTERVAL";;
    slow_down) INTERVAL=$((INTERVAL+5)); sleep "$INTERVAL";;
    access_denied|expired_token) echo "$T"; exit 1;;
    *) echo "$T"; exit 1;;
  esac
done

# 3) 呼び出し
curl -sS -i -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8000/balance-inquiry/$OWNER"
echo
