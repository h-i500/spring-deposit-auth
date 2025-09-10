set -euo pipefail

KC=http://localhost:8080
REALM=demo-realm
CLIENT_ID=mashup-cli-public

# デバイスコード要求（必ず POST）
DEVICE_JSON=$(curl -sS -f -X POST \
  "$KC/realms/$REALM/protocol/openid-connect/device/auth" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d "client_id=$CLIENT_ID&scope=openid")

# ブラウザで開く URL（complete が無ければ組み立て）
VERI=$(echo "$DEVICE_JSON" | jq -r '.verification_uri_complete // empty')
if [ -z "$VERI" ]; then
  BASE=$(echo "$DEVICE_JSON" | jq -r .verification_uri)
  CODE=$(echo "$DEVICE_JSON" | jq -r .user_code)
  VERI="${BASE}?user_code=${CODE}"
fi
echo "ブラウザで開いて認可してください:"
echo "$VERI"

DEVICE_CODE=$(echo "$DEVICE_JSON" | jq -r .device_code)
INTERVAL=$(echo "$DEVICE_JSON" | jq -r '.interval // 5')

# 認可完了までポーリング（slow_down を避けるため interval を守る）
while :; do
  TOKEN_JSON=$(curl -s -X POST \
    "$KC/realms/$REALM/protocol/openid-connect/token" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d "grant_type=urn:ietf:params:oauth:grant-type:device_code&device_code=${DEVICE_CODE}&client_id=${CLIENT_ID}")
  err=$(echo "$TOKEN_JSON" | jq -r '.error // empty')
  if [ -z "$err" ]; then
    break
  fi
  sleep "$INTERVAL"
done

ACCESS_TOKEN=$(echo "$TOKEN_JSON" | jq -r .access_token)
echo "ACCESS_TOKEN 取得完了"
