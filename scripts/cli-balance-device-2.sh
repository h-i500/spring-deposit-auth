REALM_BASE="http://localhost:8080/realms/demo-realm"
CLIENT_ID="mashup-cli-public"

# PKCE (S256)
CV=$(openssl rand -base64 96 | tr '+/' '-_' | tr -d '=[:space:]' | cut -c1-64)
CC=$(printf '%s' "$CV" | openssl dgst -binary -sha256 | base64 | tr '+/' '-_' | tr -d '=')

# device 発行（JSONを resp に保持）
resp=$(curl -sS -X POST -H "Content-Type: application/x-www-form-urlencoded" \
  -d client_id="$CLIENT_ID" \
  -d scope=openid \
  -d code_challenge_method=S256 \
  -d code_challenge="$CC" \
  "$REALM_BASE/protocol/openid-connect/auth/device")

# 必要フィールド取り出し
DEVICE_CODE=$(jq -r '.device_code' <<<"$resp")
USER_CODE=$(jq -r '.user_code' <<<"$resp")
VERIFY_URL=$(jq -r '.verification_uri' <<<"$resp")
VERIFY_URL_FULL=$(jq -r '.verification_uri_complete // empty' <<<"$resp")
INTERVAL=$(jq -r '.interval // 5' <<<"$resp")

echo "開いて認可してください:"
if [[ -n "$VERIFY_URL_FULL" && "$VERIFY_URL_FULL" != "null" ]]; then
  echo "  $VERIFY_URL_FULL"
else
  echo "  ${VERIFY_URL}?user_code=${USER_CODE}"
fi


# sleep $INTERVAL
sleep 10

TOKEN_URL="$REALM_BASE/protocol/openid-connect/token"

TOK_JSON=$(curl -sS -X POST -H "Content-Type: application/x-www-form-urlencoded" \
  -d grant_type=urn:ietf:params:oauth:grant-type:device_code \
  -d device_code="$DEVICE_CODE" \
  -d client_id="$CLIENT_ID" \
  -d code_verifier="$CV" \
  "$TOKEN_URL")

echo "$TOK_JSON" | jq .

ACCESS_TOKEN=$(jq -r '.access_token // empty' <<<"$TOK_JSON")
if [[ -z "$ACCESS_TOKEN" || "$ACCESS_TOKEN" == "null" ]]; then
  echo "まだ発行されていません（承認待ち/slow_down/expired など）。少し待って再実行してください。"
else
  echo "アクセストークン取得 OK"
fi

PAYLOAD=$(cut -d. -f2 <<< "$ACCESS_TOKEN")
rem=$(( ${#PAYLOAD} % 4 )); PAD=""
[[ $rem -eq 2 ]] && PAD="=="
[[ $rem -eq 3 ]] && PAD="="
[[ $rem -eq 1 ]] && PAD="==="
ISS=$(printf '%s' "$PAYLOAD$PAD" | tr '_-' '/+' | base64 -d 2>/dev/null | jq -r .iss)
echo "iss = $ISS"
