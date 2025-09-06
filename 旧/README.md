# 銀行アプリ（普通預金 & 定期預金サービス）

Spring Boot / PostgreSQL / Docker Compose を利用したミニマムな銀行アプリのサンプル実装です。  
普通預金サービスと定期預金サービスをそれぞれ独立したコンテナ・DBとして構築しています。  

---

## 構成

- **savings-service**
  - 普通預金口座の管理（口座開設、入金、出金、残高照会）
  - PostgreSQL バックエンド
- **time-deposit-service**
  - 定期預金の管理（開設、満期解約）
  - 内部振替（普通 → 定期）
  - PostgreSQL バックエンド
- **docker-compose**
  - 両サービスとそれぞれのDBを立ち上げる構成

---

## 起動方法

```bash
docker compose build
docker compose up -d
````

起動後のサービス:

* 普通預金サービス: [http://localhost:8081](http://localhost:8081)
* 定期預金サービス: [http://localhost:8082](http://localhost:8082)

---

## API 一覧

### 普通預金サービス (savings-service)

| メソッド | エンドポイント                   | 説明   |
| ---- | ------------------------- | ---- |
| POST | `/accounts`               | 口座開設 |
| POST | `/accounts/{id}/deposit`  | 入金   |
| POST | `/accounts/{id}/withdraw` | 出金   |
| GET  | `/accounts/{id}`          | 残高照会 |

---

### 定期預金サービス (time-deposit-service)

| メソッド | エンドポイント                | 説明            |
| ---- | ---------------------- | ------------- |
| POST | `/deposits`            | 定期預金作成        |
| GET  | `/deposits/{id}`       | 定期預金照会        |
| POST | `/deposits/{id}/close` | 満期解約          |
| POST | `/transfers`           | 内部振替（普通 → 定期） |

---

## API サンプル

### 1. 普通預金

#### 口座開設

**リクエスト**

```bash
curl -s -X POST http://localhost:8081/accounts \
  -H "Content-Type: application/json" \
  -d '{"owner":"Taro"}'
```

**レスポンス**

```json
{
  "id": "8e0cb5fa-de02-423e-bdd5-202ce7451fc5",
  "owner": "Taro",
  "balance": 0.00,
  "createdAt": "2025-08-17T11:13:50.655089Z"
}
```

#### 入金

**リクエスト**

```bash
curl -s -X POST http://localhost:8081/accounts/{id}/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount":20000}'
```

**レスポンス**

```json
{
  "id": "8e0cb5fa-de02-423e-bdd5-202ce7451fc5",
  "owner": "Taro",
  "balance": 20000.00
}
```

---

### 2. 定期預金

#### 定期預金作成

**リクエスト**

```bash
curl -s -X POST http://localhost:8082/deposits \
  -H "Content-Type: application/json" \
  -d '{"owner":"Hanako","principal":10000,"annualRate":0.015,"termDays":30}'
```

**レスポンス**

```json
{
  "id": "3e389eae-d082-4ba4-961f-9101d5bf45ae",
  "owner": "Hanako",
  "principal": 10000.00,
  "annualRate": 0.015,
  "termDays": 30,
  "startAt": "2025-08-17T11:14:23.032658Z",
  "maturityDate": "2025-09-16T11:14:23.032658Z",
  "status": "OPEN"
}
```

#### 満期解約

**リクエスト**

```bash
curl -s -X POST http://localhost:8082/deposits/{id}/close
```

**レスポンス**

```json
{
  "id": "3e389eae-d082-4ba4-961f-9101d5bf45ae",
  "status": "CLOSED"
}
```

---

### 3. 内部振替（普通預金 → 定期預金）

#### 振替実行

**リクエスト**

```bash
curl -s -X POST http://localhost:8082/transfers \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":"8e0cb5fa-de02-423e-bdd5-202ce7451fc5","owner":"Hanako","principal":10000,"annualRate":0.015,"termDays":30}'
```

**レスポンス**

```json
{
  "fromAccountId": "8e0cb5fa-de02-423e-bdd5-202ce7451fc5",
  "timeDepositId": "3e389eae-d082-4ba4-961f-9101d5bf45ae",
  "status": "COMPLETED"
}
```

#### 確認（残高 & 定期）

```bash
curl -s http://localhost:8081/accounts/{id}
curl -s http://localhost:8082/deposits/{timeDepositId}
```

---

## ライセンス

本リポジトリ内のコードは学習目的のサンプルです。必要に応じてコピー/改変してご利用ください。

---
