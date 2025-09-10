#!/usr/bin/env bash
KC=http://localhost:8080
REALM=demo-realm

# JWKS 取得
curl -s "$KC/realms/$REALM/protocol/openid-connect/certs" > tmp/jwks.json

# 何でもよいので直近の kid を1つ選ぶ（例：先頭）
KID=$(jq -r '.keys[] | select(.use=="sig") | .kid' tmp/jwks.json | head -n1)
# 対応する x5c(証明書) → 公開鍵(PEM) に変換
CERT=$(jq -r --arg K "$KID" '.keys[] | select(.kid==$K) | .x5c[0]' tmp/jwks.json)
( echo "-----BEGIN CERTIFICATE-----"
  echo "$CERT" | fold -w 64
  echo "-----END CERTIFICATE-----" ) > tmp/kid.crt
openssl x509 -in tmp/kid.crt -pubkey -noout > tmp/kid_pubkey.pem

# この /tmp/kid_pubkey.pem の中身を kong.yml の rsa_public_key: にそのまま貼る
cat tmp/kid_pubkey.pem
