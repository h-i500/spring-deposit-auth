#!/usr/bin/env bash

# 1) 現在の署名証明書を取得→PEM化
curl -s http://localhost:8080/realms/demo-realm/protocol/openid-connect/certs \
| jq -r '.keys[] | select(.use=="sig" and .kty=="RSA") | .x5c[0]' \
| head -n1 | awk 'BEGIN{print "-----BEGIN CERTIFICATE-----"}{gsub(".{64}","&\n");print}END{print "-----END CERTIFICATE-----"}' \
> kid.crt

# 2) Consumer 作成（既存なら 409 になるだけ）
curl -s -X POST http://localhost:8001/consumers -d "username=keycloak" >/dev/null

# 3) 既存の JWT 資格情報を削除（あれば）
# for id in $(curl -s http://localhost:8001/consumers/keycloak/jwt | jq -r '.data[].id'); do
#   curl -s -X DELETE http://localhost:8001/consumers/keycloak/jwt/$id >/dev/null
# done
resp=$(curl -s -w '\n%{http_code}' http://localhost:8001/consumers/keycloak/jwt || true)
body=${resp%$'\n'*}
code=${resp##*$'\n'}

if [ "$code" = "200" ]; then
  ids=$(jq -r '.data[]?.id // empty' <<<"$body")
  for id in $ids; do
    curl -s -X DELETE http://localhost:8001/consumers/keycloak/jwt/$id >/dev/null
  done
else
  echo "No JWT secrets (or Admin API not writeable): HTTP $code — skip deleting"
fi


# 4) 新しい RSA 公開鍵を登録（issuer=Keycloak の iss を key に）
curl -s -X POST http://localhost:8001/consumers/keycloak/jwt \
  -F 'algorithm=RS256' \
  -F 'key=http://localhost:8080/realms/demo-realm' \
  -F 'rsa_public_key=@kid.crt;type=text/plain' >/dev/null

# client_credentials でトークン取得
TOKEN=$(curl -s -X POST \
  -d 'client_id=mashup-cli' -d 'client_secret=changeit' -d 'grant_type=client_credentials' \
  http://localhost:8080/realms/demo-realm/protocol/openid-connect/token | jq -r .access_token)

# Kong 経由で集計API
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8000/balance-inquiry/demo
