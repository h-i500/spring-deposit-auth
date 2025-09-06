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

## docker-compose.yml

```yaml
version: "3.9"
services:
  savings-db:
    image: postgres:16
    container_name: savings-db
    environment:
      POSTGRES_DB: savings
      POSTGRES_USER: savings
      POSTGRES_PASSWORD: savings
    ports:
      - "5433:5432"  # 任意（ホストから直接叩く場合）
    volumes:
      - savings-data:/var/lib/postgresql/data

  savings-service:
    build: ./savings-service
    container_name: savings-service
    environment:
      DB_HOST: savings-db
      DB_PORT: 5432
      DB_NAME: savings
      DB_USER: savings
      DB_PASSWORD: savings
      SPRING_PROFILES_ACTIVE: docker
    depends_on:
      - savings-db
    ports:
      - "8081:8080"

  timedeposit-db:
    image: postgres:16
    container_name: timedeposit-db
    environment:
      POSTGRES_DB: timedeposit
      POSTGRES_USER: timedeposit
      POSTGRES_PASSWORD: timedeposit
    volumes:
      - timedeposit-data:/var/lib/postgresql/data

  time-deposit-service:
    build: ./time-deposit-service
    container_name: time-deposit-service
    environment:
      DB_HOST: timedeposit-db
      DB_PORT: 5432
      DB_NAME: timedeposit
      DB_USER: timedeposit
      DB_PASSWORD: timedeposit
      SPRING_PROFILES_ACTIVE: docker
    depends_on:
      - timedeposit-db
    ports:
      - "8082:8080"

volumes:
  savings-data:
  timedeposit-data:
```

---

## savings-service

### `pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>savings-service</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <properties>
    <java.version>21</java.version>
    <spring.boot.version>3.3.2</spring.boot.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring.boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <layers enabled="true"/>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

### `Dockerfile`

```dockerfile
# build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

### `src/main/resources/application.yml`

```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:savings}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate.jdbc.time_zone: UTC
  sql:
    init:
      mode: always
```

### `src/main/resources/schema.sql`

```sql
CREATE TABLE IF NOT EXISTS accounts (
  id UUID PRIMARY KEY,
  owner TEXT NOT NULL,
  balance NUMERIC(19,2) NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  version BIGINT NOT NULL DEFAULT 0
);
```

### `src/main/java/com/example/savings/SavingsApplication.java`

```java
package com.example.savings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SavingsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SavingsApplication.class, args);
    }
}
```

### `src/main/java/com/example/savings/model/Account.java`

```java
package com.example.savings.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (balance == null) balance = BigDecimal.ZERO;
    }

    public UUID getId() { return id; }
    public String getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }

    public void setOwner(String owner) { this.owner = owner; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
```

### `src/main/java/com/example/savings/repository/AccountRepository.java`

```java
package com.example.savings.repository;

import com.example.savings.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {}
```

### `src/main/java/com/example/savings/service/AccountService.java`

```java
package com.example.savings.service;

import com.example.savings.model.Account;
import com.example.savings.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AccountService {
    private final AccountRepository repo;

    public AccountService(AccountRepository repo) {
        this.repo = repo;
    }

    public Account create(String owner) {
        Account a = new Account();
        a.setOwner(owner);
        a.setBalance(BigDecimal.ZERO);
        return repo.save(a);
    }

    public Account get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    @Transactional
    public Account deposit(UUID id, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amount must be > 0");
        Account a = get(id);
        a.setBalance(a.getBalance().add(amount));
        return a;
    }

    @Transactional
    public Account withdraw(UUID id, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amount must be > 0");
        Account a = get(id);
        if (a.getBalance().compareTo(amount) < 0) throw new IllegalStateException("insufficient funds");
        a.setBalance(a.getBalance().subtract(amount));
        return a;
    }
}
```

### `src/main/java/com/example/savings/controller/AccountController.java`

```java
package com.example.savings.controller;

import com.example.savings.model.Account;
import com.example.savings.service.AccountService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) { this.service = service; }

    public record CreateAccountRequest(@NotBlank String owner) {}
    public record MoneyRequest(@NotNull BigDecimal amount) {}

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateAccountRequest req) {
        Account a = service.create(req.owner());
        return ResponseEntity.ok(Map.of(
                "id", a.getId(),
                "owner", a.getOwner(),
                "balance", a.getBalance(),
                "createdAt", a.getCreatedAt()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        Account a = service.get(id);
        return ResponseEntity.ok(Map.of(
                "id", a.getId(),
                "owner", a.getOwner(),
                "balance", a.getBalance(),
                "createdAt", a.getCreatedAt()
        ));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<?> deposit(@PathVariable UUID id, @RequestBody MoneyRequest req) {
        Account a = service.deposit(id, req.amount());
        return ResponseEntity.ok(Map.of(
                "id", a.getId(),
                "balance", a.getBalance()
        ));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable UUID id, @RequestBody MoneyRequest req) {
        Account a = service.withdraw(id, req.amount());
        return ResponseEntity.ok(Map.of(
                "id", a.getId(),
                "balance", a.getBalance()
        ));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<?> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```

---

## time-deposit-service

### `pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>time-deposit-service</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <properties>
    <java.version>21</java.version>
    <spring.boot.version>3.3.2</spring.boot.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring.boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <layers enabled="true"/>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

### `Dockerfile`

```dockerfile
# build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

### `src/main/resources/application.yml`

```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:timedeposit}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate.jdbc.time_zone: UTC
  sql:
    init:
      mode: always
```

### `src/main/resources/schema.sql`

```sql
CREATE TYPE IF NOT EXISTS deposit_status AS ENUM ('OPEN', 'CLOSED');

CREATE TABLE IF NOT EXISTS time_deposits (
  id UUID PRIMARY KEY,
  owner TEXT NOT NULL,
  principal NUMERIC(19,2) NOT NULL,
  annual_rate NUMERIC(9,6) NOT NULL,
  term_days INT NOT NULL,
  start_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  maturity_at TIMESTAMPTZ NOT NULL,
  status deposit_status NOT NULL DEFAULT 'OPEN'
);
```

### `src/main/java/com/example/timedeposit/TimeDepositApplication.java`

```java
package com.example.timedeposit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TimeDepositApplication {
    public static void main(String[] args) {
        SpringApplication.run(TimeDepositApplication.class, args);
    }
}
```

### `src/main/java/com/example/timedeposit/model/TimeDeposit.java`

```java
package com.example.timedeposit.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "time_deposits")
public class TimeDeposit {
    public enum Status { OPEN, CLOSED }

    @Id
    private UUID id;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principal;

    @Column(name = "annual_rate", nullable = false, precision = 9, scale = 6)
    private BigDecimal annualRate; // 0.015 = 1.5%

    @Column(name = "term_days", nullable = false)
    private int termDays;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "maturity_at", nullable = false)
    private Instant maturityAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (startAt == null) startAt = Instant.now();
        if (maturityAt == null) maturityAt = startAt.plus(termDays, ChronoUnit.DAYS);
    }

    public UUID getId() { return id; }
    public String getOwner() { return owner; }
    public BigDecimal getPrincipal() { return principal; }
    public BigDecimal getAnnualRate() { return annualRate; }
    public int getTermDays() { return termDays; }
    public Instant getStartAt() { return startAt; }
    public Instant getMaturityAt() { return maturityAt; }
    public Status getStatus() { return status; }

    public void setOwner(String owner) { this.owner = owner; }
    public void setPrincipal(BigDecimal principal) { this.principal = principal; }
    public void setAnnualRate(BigDecimal annualRate) { this.annualRate = annualRate; }
    public void setTermDays(int termDays) { this.termDays = termDays; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public void setMaturityAt(Instant maturityAt) { this.maturityAt = maturityAt; }
    public void setStatus(Status status) { this.status = status; }
}
```

### `src/main/java/com/example/timedeposit/repository/TimeDepositRepository.java`

```java
package com.example.timedeposit.repository;

import com.example.timedeposit.model.TimeDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TimeDepositRepository extends JpaRepository<TimeDeposit, UUID> {}
```

### `src/main/java/com/example/timedeposit/service/TimeDepositService.java`

```java
package com.example.timedeposit.service;

import com.example.timedeposit.model.TimeDeposit;
import com.example.timedeposit.repository.TimeDepositRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class TimeDepositService {
    private final TimeDepositRepository repo;

    public TimeDepositService(TimeDepositRepository repo) {
        this.repo = repo;
    }

    public TimeDeposit create(String owner, BigDecimal principal, BigDecimal annualRate, int termDays) {
        if (principal == null || principal.signum() <= 0) throw new IllegalArgumentException("principal must be > 0");
        if (annualRate == null || annualRate.signum() < 0) throw new IllegalArgumentException("annualRate must be >= 0");
        if (termDays <= 0) throw new IllegalArgumentException("termDays must be > 0");
        TimeDeposit td = new TimeDeposit();
        td.setOwner(owner);
        td.setPrincipal(principal.setScale(2, RoundingMode.HALF_UP));
        td.setAnnualRate(annualRate);
        td.setTermDays(termDays);
        return repo.save(td);
    }

    public TimeDeposit get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("deposit not found"));
    }

    /** 単利: principal * (1 + annualRate * termDays/365) */
    public BigDecimal calculatePayout(TimeDeposit td) {
        BigDecimal days = new BigDecimal(td.getTermDays());
        BigDecimal factor = BigDecimal.ONE.add(td.getAnnualRate().multiply(days).divide(new BigDecimal("365"), MathContext.DECIMAL64));
        return td.getPrincipal().multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public BigDecimal close(UUID id, Instant now) {
        TimeDeposit td = get(id);
        if (td.getStatus() == TimeDeposit.Status.CLOSED) throw new IllegalStateException("already closed");
        if (now.isBefore(td.getMaturityAt())) throw new IllegalStateException("not matured yet");
        BigDecimal payout = calculatePayout(td);
        td.setStatus(TimeDeposit.Status.CLOSED);
        return payout;
    }
}
```

### `src/main/java/com/example/timedeposit/controller/DepositController.java`

```java
package com.example.timedeposit.controller;

import com.example.timedeposit.model.TimeDeposit;
import com.example.timedeposit.service.TimeDepositService;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/deposits")
public class DepositController {
    private final TimeDepositService service;

    public DepositController(TimeDepositService service) { this.service = service; }

    public record CreateRequest(@NotBlank String owner,
                                @NotNull @DecimalMin("0.01") BigDecimal principal,
                                @NotNull @DecimalMin("0.0") BigDecimal annualRate,
                                @Min(1) int termDays) {}

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateRequest req) {
        TimeDeposit td = service.create(req.owner(), req.principal(), req.annualRate(), req.termDays());
        return ResponseEntity.ok(Map.of(
                "id", td.getId(),
                "owner", td.getOwner(),
                "principal", td.getPrincipal(),
                "annualRate", td.getAnnualRate(),
                "termDays", td.getTermDays(),
                "startAt", td.getStartAt(),
                "maturityDate", td.getMaturityAt(),
                "status", td.getStatus()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        TimeDeposit td = service.get(id);
        return ResponseEntity.ok(Map.of(
                "id", td.getId(),
                "owner", td.getOwner(),
                "principal", td.getPrincipal(),
                "annualRate", td.getAnnualRate(),
                "termDays", td.getTermDays(),
                "startAt", td.getStartAt(),
                "maturityDate", td.getMaturityAt(),
                "status", td.getStatus()
        ));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> close(@PathVariable UUID id) {
        var payout = service.close(id, Instant.now());
        return ResponseEntity.ok(Map.of("id", id, "payout", payout));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<?> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```

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
