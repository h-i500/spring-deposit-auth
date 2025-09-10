CERT=$(
  curl -s "http://localhost:8080/realms/demo-realm/protocol/openid-connect/certs" \
  | jq -r '.keys[] | select(.alg=="RS256") | .x5c[0]' \
  | head -n1
)

# 取れたか確認（空なら Keycloak がまだ起動中 or URL/realm ミス）
echo "len(CERT) = ${#CERT}"

# 証明書 → 公開鍵 (BEGIN PUBLIC KEY …)
{ echo "-----BEGIN CERTIFICATE-----"
  echo "$CERT" | fold -w 64
  echo "-----END CERTIFICATE-----"
} | openssl x509 -pubkey -noout > kc.pub

echo "==== kc.pub ===="
sed -n '1,5p' kc.pub
