#!/usr/bin/env bash
set -euo pipefail

REALM_BASE="http://localhost:8080/realms/demo-realm"
CLIENT_ID="mashup-cli-public"

# PKCE (S256)
CV=$(openssl rand -base64 96 | tr '+/' '-_' | tr -d '=[:space:]' | cut -c1-64)
CC=$(printf '%s' "$CV" | openssl dgst -binary -sha256 | base64 | tr '+/' '-_' | tr -d '=')

# device code
resp=$(curl -sS -X POST -H "Content-Type: application/x-www-form-urlencoded" \
  -d client_id="$CLIENT_ID" \
  -d scope="openid email profile" \
  -d code_challenge_method=S256 \
  -d code_challenge="$CC" \
  "$REALM_BASE/protocol/openid-connect/auth/device")

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

# 少し待ってからポーリング
# sleep "$INTERVAL"
sleep 15

TOK_JSON=$(curl -sS -X POST -H "Content-Type: application/x-www-form-urlencoded" \
  -d grant_type=urn:ietf:params:oauth:grant-type:device_code \
  -d device_code="$DEVICE_CODE" \
  -d client_id="$CLIENT_ID" \
  -d code_verifier="$CV" \
  "$REALM_BASE/protocol/openid-connect/token")

echo "$TOK_JSON" | jq .

ACCESS_TOKEN=$(jq -r '.access_token // empty' <<<"$TOK_JSON")
[[ -z "$ACCESS_TOKEN" || "$ACCESS_TOKEN" == "null" ]] && { echo "トークン未発行（承認待ち/slow_down 等）"; exit 1; }

# 保存して再利用できるように
printf '%s' "$ACCESS_TOKEN" > .access_token
echo "export ACCESS_TOKEN=$(cat .access_token)" > .env.tokens
echo "[info] wrote .access_token / .env.tokens (source .env.tokens で再利用可)"

# JWT から preferred_username を取り owner にする（OWNER 環境変数で上書き可）
# if [[ -z "${OWNER:-}" ]]; then
#   PAYLOAD=$(cut -d. -f2 <<< "$ACCESS_TOKEN")
#   # base64url デコード（パディング対応）
#   PAD=$(( (4 - ${#PAYLOAD} % 4) % 4 )); PADSTR=$(printf '=%.0s' $(seq 1 $PAD))
#   OWNER=$(printf '%s' "$PAYLOAD$PADSTR" | tr '_-' '/+' | base64 -d 2>/dev/null | jq -r .preferred_username)
# fi

OWNER_ARG="${1:-}"
if [[ -n "$OWNER_ARG" ]]; then
  OWNER="$OWNER_ARG"
elif [[ -z "${OWNER:-}" ]]; then
  PAYLOAD=$(cut -d. -f2 <<< "$ACCESS_TOKEN")
  PAD=$(( (4 - ${#PAYLOAD} % 4) % 4 )); PADSTR=$(printf '=%.0s' $(seq 1 $PAD))
  OWNER=$(printf '%s' "$PAYLOAD$PADSTR" | tr '_-' '/+' | base64 -d 2>/dev/null | jq -r .preferred_username)
fi
echo "owner = $OWNER"

echo "---- call via Kong ----"
curl -i -H "Authorization: Bearer $ACCESS_TOKEN" \
  "http://localhost:8000/balance-inquiry/$OWNER"




# echo "owner = $OWNER"

# # Kong 経由で照会
# echo "---- call via Kong ----"
# curl -i -H "Authorization: Bearer $ACCESS_TOKEN" "http://localhost:8000/balance-inquiry/$OWNER"
# # curl -i -H "Authorization: Bearer $ACCESS_TOKEN" "http://localhost:8000/balance-inquiry/demo"
