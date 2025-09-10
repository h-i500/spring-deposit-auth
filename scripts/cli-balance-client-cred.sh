#!/usr/bin/env bash
set -euo pipefail
KC=${KC:-http://localhost:8080}
REALM=${REALM:-demo-realm}
CID=${CID:-mashup-cli}
SECRET=${SECRET:-changeit}
OWNER=${1:-alice}

# 1) サービスアカウントでアクセストークン取得
RESP=$(curl -s -X POST \
  -d "client_id=$CID" \
  -d "client_secret=$SECRET" \
  -d "grant_type=client_credentials" \
  "$KC/realms/$REALM/protocol/openid-connect/token")
TOKEN=$(jq -r .access_token <<<"$RESP")
[[ -z "$TOKEN" || "$TOKEN" == "null" ]] && { echo "$RESP"; exit 1; }

# 2) Kong 経由で呼ぶ（/balance-inquiry/{owner}）
curl -sS -i -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8000/balance-inquiry/$OWNER"
echo
