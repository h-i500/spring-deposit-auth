# ミニマム銀行アプリ（普通預金 / 定期預金）

Spring Boot / PostgreSQL / Docker Compose で、**普通預金（Savings）** と **定期預金（Time Deposit）** をそれぞれ独立したアプリ + DB コンテナとして動かす最小サンプルです。

- 各サービスは独立デプロイ可能（マイクロサービス風）
- 各サービスが自身の PostgreSQL に接続（DB も分離）
- シンプルな REST API
  - **Savings**: 口座作成 / 残高参照 / 入金 / 出金（楽観ロック）
  - **Time Deposit**: 申込 / 照会 / 満期解約（単利計算）

---

## プロジェクト構成

```
.
├── docker-compose.yml
├── savings-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/savings/
│       │   ├── SavingsApplication.java
│       │   ├── controller/AccountController.java
│       │   ├── service/AccountService.java
│       │   ├── model/Account.java
│       │   └── repository/AccountRepository.java
│       └── main/resources/
│           ├── application.yml
│           └── schema.sql
└── time-deposit-service/
    ├── Dockerfile
    ├── pom.xml
    └── src/
        ├── main/java/com/example/timedeposit/
        │   ├── TimeDepositApplication.java
        │   ├── controller/DepositController.java
        │   ├── service/TimeDepositService.java
        │   ├── model/TimeDeposit.java
        │   └── repository/TimeDepositRepository.java
        └── main/resources/
            ├── application.yml
            └── schema.sql
```

---

## 使い方（ローカルで Docker 動作）

1. ルート直下でビルド & 起動

```bash
docker compose up -d --build
```

2. 起動後のエンドポイント

- Savings サービス: `http://localhost:8081`
- Time Deposit サービス: `http://localhost:8082`

3. 動作確認の例（curl）

```bash
# 口座作成（Savings）
curl -s -X POST http://localhost:8081/accounts -H 'Content-Type: application/json' -d '{"owner":"Taro"}'
# -> {"id":"<UUID>", "owner":"Taro", "balance":0.00, ...}

# 入金
curl -s -X POST http://localhost:8081/accounts/<UUID>/deposit -H 'Content-Type: application/json' -d '{"amount":1000}'

# 残高
curl -s http://localhost:8081/accounts/<UUID>

# 定期預金 30日, 年率1.5%
curl -s -X POST http://localhost:8082/deposits -H 'Content-Type: application/json' -d '{"owner":"Hanako","principal":10000,"annualRate":0.015,"termDays":30}'
# -> {"id":"<UUID>", "maturityDate":"..."}

# 満期解約（maturityDate 以降）
curl -s -X POST http://localhost:8082/deposits/<UUID>/close
```

> **注意**: これは学習用の最小実装です。認証/認可、入出力バリデーション強化、監査ログ、外部送金などは含めていません。



---

## 補足

- **トランザクション管理**: Savings の入出金は `@Transactional` + 楽観ロック（`@Version`）で簡易的に保護しています。
- **スキーマ管理**: 簡潔さ優先で `schema.sql` を利用。実運用では Flyway/Liquibase 推奨。
- **金額型**: Java は `BigDecimal`、DB は `NUMERIC(19,2)` を採用。
- **金利計算**: 単利・365日基準の最小実装です（源泉徴収などは未考慮）。

---

## 次のステップ（任意）

- API バリデーション強化（Bean Validation のエラーレスポンス整形）
- OpenAPI/Swagger UI 追加
- 統合テスト（Testcontainers）
- 送金や内部振替（Savings → Time Deposit など）のオーケストレーション
- 認証/認可（Spring Security + JWT）

```
```
