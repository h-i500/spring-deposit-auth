

# ブラウザでの承認を挟む方法

1. keycloakとkongを起動
以下コマンドで全体を起動
```
$ docker compose up -d --build
```
(参考：削除)
```
$ docker compose down -v
```

2. PUBLIC KEY生成
以下コマンドを実行。
```
$ chmod +x scripts/key-create.sh
$ scripts/key-create.sh
```
プロジェクトトップにkc.pubが生成されます。

3. kongへ設定
kc.pubの内容を、 kong/kong.yml へ以下のように記述する。
```
consumers:
  - username: keycloak
    jwt_secrets:      
      - algorithm: RS256
        key: http://keycloak:8080/realms/demo-realm
        rsa_public_key: |
          -----BEGIN PUBLIC KEY-----
          MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArkrNto9BAC/YylcoGWq3
          mIcFa4M6XFQ6xZ7DrBXLxnF7iTxyqOGBNc0oxhmzIpfxXonesXqhA0H348aukCEK
          J2OLrwE3b+VGBHjMygyi4/0N68NpVucFT6F5xh6DjlPJzg+kyzK4xXcH9vm1Jp3p
          7hawDgwu3klSSrSr3ijle5f7Jj9flWjtCTPYf/3EJ5yw62XA1B8v6kkL/zaH7syZ
          jbqO1vtPbjJf24LbySPimNY+0L6iFBuDHcJkZYaHvwFfu7Op6XMNkVqn9jczj+mY
          kXfFmfAARcZgFwH2LjXMaKvnVft6k3XX5+5CGu45+UCLoqzBesqjeBhG9TMUlF/D
          1wIDAQAB
          -----END PUBLIC KEY-----
```

以下コマンドでkongへ反映する。
$ docker compose restart kong

4. シェル実行
以下シェルを実行する。認証情報を得ながら、残高照会をします。
```
$ chmod +x scripts/cli-balance-device.sh
$ scripts/cli-balance-device.sh testuser
開いて認可してください:
  http://keycloak:8080/realms/demo-realm/device?user_code=XGJV-MGMY
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJPZTVHckNfa0g0SnlMZXZXdlNtSi02dmpFMnN3NEYxTjZnQzRTbUM0VjhVIn0.eyJleHAiOjE3NTc1OTIzNjMsImlhdCI6MTc1NzU5MTQ2MywianRpIjoiZDFiNzVmN2MtODRiMS00ZTEyLTgzMDktNmZiM2VjNjM3YWY1IiwiaXNzIjoiaHR0cDovL2tleWNsb2FrOjgwODAvcmVhbG1zL2RlbW8tcmVhbG0iLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJtYXNodXAtY2xpLXB1YmxpYyIsInNpZCI6ImQyMWFhOWJhLWI3NmUtNDU4Ny05M2Y5LTU2MGZiN2UyY2M4NSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJyZWFkIiwidXNlciJdfSwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJuYW1lIjoiRGVtbyBVc2VyIiwicHJlZmVycmVkX3VzZXJuYW1lIjoidGVzdHVzZXIiLCJnaXZlbl9uYW1lIjoiRGVtbyIsImZhbWlseV9uYW1lIjoiVXNlciIsImVtYWlsIjoidXNlckBleGFtcGxlLmNvbSJ9.L4GoqigxY3GY67-PhSz-YedcNJEDHHaMDMnHNOdkwmG2s_52YSTC3KjN4oVRUL7S_obvw9GsZ3QMbV8b70Rq2BIVsv6rW_jJw3FiJzn-okDo-zkm4KuFkL8V9fFhEot7qzlIaXPsWcPtDOUIb5eJS1W_pHuLsr4En0L2YzyePoWSU57ONhqCRoKKnVgzlBoP_X8fSgONooB3TqqzZcdhLkMBm5fG1GUz1SU5FVjeIUdBgCdle6R7kA3KJKwYKxTYJTp_0esGMEcOsjW8KTsWUbqi3CDu8-GB1OI4FJyZYDBF7ECm67-lEoYN6D2p6HgjErePSKNIEYZjXzAlfQQOrA",
  "expires_in": 900,
  "refresh_expires_in": 3600,
  "refresh_token": "eyJhbGciOiJIUzUxMiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI0OGFlOThlNC1jYTgzLTQ3ZjUtOThjOC01MjA3NzM5ZmZmYzQifQ.eyJleHAiOjE3NTc1OTUwNjMsImlhdCI6MTc1NzU5MTQ2MywianRpIjoiZTQ1YTBhNDQtNjQwZS00MGJlLThhNDYtYjcyMmEyMDRlYjYzIiwiaXNzIjoiaHR0cDovL2tleWNsb2FrOjgwODAvcmVhbG1zL2RlbW8tcmVhbG0iLCJhdWQiOiJodHRwOi8va2V5Y2xvYWs6ODA4MC9yZWFsbXMvZGVtby1yZWFsbSIsInR5cCI6IlJlZnJlc2giLCJhenAiOiJtYXNodXAtY2xpLXB1YmxpYyIsInNpZCI6ImQyMWFhOWJhLWI3NmUtNDU4Ny05M2Y5LTU2MGZiN2UyY2M4NSIsInNjb3BlIjoib3BlbmlkIGVtYWlsIHJvbGVzIHByb2ZpbGUifQ.p9hCDxTx7lr9tk5uQEf68Pb5xwUxEASrOgiIBsde1sKWHenQf8lJl4NTPaiCIykDCTd6DoY1WhXMQXugNxFCsg",
  "token_type": "Bearer",
  "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJPZTVHckNfa0g0SnlMZXZXdlNtSi02dmpFMnN3NEYxTjZnQzRTbUM0VjhVIn0.eyJleHAiOjE3NTc1OTIzNjMsImlhdCI6MTc1NzU5MTQ2MywianRpIjoiM2I4NDk4MDgtMjUzNC00M2M0LTlkZDEtY2M5ZmIzMDU0NmE3IiwiaXNzIjoiaHR0cDovL2tleWNsb2FrOjgwODAvcmVhbG1zL2RlbW8tcmVhbG0iLCJhdWQiOiJtYXNodXAtY2xpLXB1YmxpYyIsInN1YiI6IjFiNmQ1MDliLWYxNmEtNGVjYy04ZjY2LTYxNmVkNzM4OWNkMyIsInR5cCI6IklEIiwiYXpwIjoibWFzaHVwLWNsaS1wdWJsaWMiLCJzaWQiOiJkMjFhYTliYS1iNzZlLTQ1ODctOTNmOS01NjBmYjdlMmNjODUiLCJhdF9oYXNoIjoiSGRNeEFXMi1kLVFMV0NnX0oyX0pXZyIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsicmVhZCIsInVzZXIiXX0sIm5hbWUiOiJEZW1vIFVzZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0dXNlciIsImdpdmVuX25hbWUiOiJEZW1vIiwiZmFtaWx5X25hbWUiOiJVc2VyIiwiZW1haWwiOiJ1c2VyQGV4YW1wbGUuY29tIn0.HZU5cVRnONMA0rkjq-pzZcBEYg65Y2VJnyOvVfbh6iENgpsiSQKKOTuQ9x53v_NmocO9H0w8HzKiGq3gdiqB2c_nSDW_O3J8xIMZHu80Ng6UzImcOmcv6wwnKr4H9fS7RdJwRSFcXih5gOFqRauMkhlggu_R_Oo5Q4i3VFLMvOYjMFKf0ktrrRWvHGoS1woELfbQGztaf1dN-aR5akAy6uhUQ-H23sFOAFawsfei7fUrg3bz_ra2ma16FyLIkMV1KSN6mFt3wxU09Z3zDtm8mW0zkQ9SXupa1Qh02bNIQ8CC8g_pO1CKUCgdCZFyt4F5kpLP9oRO4uy7u0tBLwXHMg",
  "not-before-policy": 0,
  "session_state": "d21aa9ba-b76e-4587-93f9-560fb7e2cc85",
  "scope": "openid email profile"
}
[info] wrote .access_token / .env.tokens (source .env.tokens で再利用可)
owner = testuser
---- call via Kong ----
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
Content-Length: 262
Connection: keep-alive
X-Kong-Upstream-Latency: 691
X-Kong-Proxy-Latency: 3
Via: kong/3.6.1
X-Kong-Request-Id: 0528b7507de9ab311b20c853f6f15f11

{"owner":"testuser","savings":[{"id":"de9a7fcb-6fe3-424b-b45a-e86081229842","accountNo":null,"ownerKey":null,"balance":9989000.0}],"timeDeposits":[{"id":"14df1061-7eae-4eb7-8a5a-4b5cf8317de1","accountNo":null,"ownerKey":null,"principal":10000.0,"balance":null}]}
```

上記では、途中で、
http://keycloak:8080/realms/demo-realm/device?user_code=XGJV-MGMY
へブラウザへアクセスすることで、認証することができます。
なお、以下のtestuserは、検索対象のownerキーです。
```
$ scripts/cli-balance-device.sh testuser
```


# 承認手続きも含めてCLIで実施する方法

1. オンライン専用
ACCESS_TOKEN の有効期限（既定 900 秒）内に都度取得して使う形。
以下コマンドを実行する。
```
$ chmod +x scripts/cli-balance-ropc.sh
$ scripts/cli-balance-ropc.sh testuser password ppXevOmtsc7nqFq0THeurHflbLdUsq0N
```

2. 長期無人運用したい（オフライン token も取得）
すでに案内した手順Bのとおり、mashup-cli-ropc クライアントに offline_access クライアントスコープを関連付けたうえで、以下を実行
```
$ OFFLINE=1 scripts/cli-balance-ropc.sh testuser password <CLIENT_SECRET>
```


