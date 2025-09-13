#!/usr/bin/env bash
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
ADMIN_REALM="${ADMIN_REALM:-master}"
TARGET_REALM="${TARGET_REALM:-demo-realm}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin}"
CLIENT_ID="${CLIENT_ID:-mashup-cli-ropc}"

json() { jq -r "$1" <<<"$2"; }

# 1) 管理者トークン
TOKEN_JSON=$(curl -sS -X POST -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli" -d "username=$ADMIN_USER" -d "password=$ADMIN_PASS" -d "grant_type=password" \
  "$KEYCLOAK_URL/realms/$ADMIN_REALM/protocol/openid-connect/token")
ACCESS_TOKEN=$(json '.access_token // empty' "$TOKEN_JSON")
if [[ -z "$ACCESS_TOKEN" ]]; then
  echo "[ERROR] admin token 取得失敗"; echo "$TOKEN_JSON" | jq .; exit 1
fi

# 2) UUID 取得
LIST_JSON=$(curl -sS -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$KEYCLOAK_URL/admin/realms/$TARGET_REALM/clients?clientId=$CLIENT_ID")
CLIENT_UUID=$(json '.[0].id // empty' "$LIST_JSON")
if [[ -z "$CLIENT_UUID" ]]; then
  echo "[ERROR] クライアント未検出: clientId=$CLIENT_ID"; echo "$LIST_JSON" | jq .; exit 1
fi

# 3) 現在設定を取得
CUR_JSON=$(curl -sS -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$KEYCLOAK_URL/admin/realms/$TARGET_REALM/clients/$CLIENT_UUID")
echo "[INFO] BEFORE:"; echo "$CUR_JSON" | jq '{clientId, publicClient, clientAuthenticatorType}'

# 4) Confidential化（必要最小限のみ変更）
PATCH_JSON=$(jq '
  .publicClient=false
  | .bearerOnly=false
  | .clientAuthenticatorType="client-secret"
' <<<"$CUR_JSON")

# 5) 反映（ステータス/エラー表示）
RESP=$(curl -sS -w "\n%{http_code}" -X PUT \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" \
  -d "$PATCH_JSON" \
  "$KEYCLOAK_URL/admin/realms/$TARGET_REALM/clients/$CLIENT_UUID")
BODY=$(sed '$d' <<<"$RESP"); CODE=$(tail -n1 <<<"$RESP")
if [[ "$CODE" != "204" && "$CODE" != "201" && "$CODE" != "200" ]]; then
  echo "[ERROR] PUT 失敗 (HTTP $CODE)"; echo "$BODY"; exit 1
fi

# 6) 反映後の確認
AFTER_JSON=$(curl -sS -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$KEYCLOAK_URL/admin/realms/$TARGET_REALM/clients/$CLIENT_UUID")
echo "[INFO] AFTER:"; echo "$AFTER_JSON" | jq '{clientId, publicClient, clientAuthenticatorType}'

# 7) シークレット生成（回転）→ 取得
ROTATE_JSON=$(curl -sS -X POST -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$KEYCLOAK_URL/admin/realms/$TARGET_REALM/clients/$CLIENT_UUID/client-secret")
echo "[INFO] ROTATE RESPONSE:"; echo "$ROTATE_JSON" | jq .

SECRET_JSON=$(curl -sS -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$KEYCLOAK_URL/admin/realms/$TARGET_REALM/clients/$CLIENT_UUID/client-secret")
SECRET=$(json '.value // empty' "$SECRET_JSON")
if [[ -z "$SECRET" ]]; then
  echo "[ERROR] client-secret が空です（まだ Public の可能性）"
  echo "$SECRET_JSON" | jq .
  exit 1
fi

echo "CLIENT_SECRET=$SECRET"
