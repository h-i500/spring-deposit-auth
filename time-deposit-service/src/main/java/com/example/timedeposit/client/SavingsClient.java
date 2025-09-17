package com.example.timedeposit.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class SavingsClient {

    private final RestClient rest;
    private final String baseUrl;

    // docker-compose のサービス名で到達できる URL をデフォルトにしています。
    // 例: http://savings-service:8081 （savings-service の 8081）
    public SavingsClient(RestClient.Builder builder,
                         @Value("${savings.base-url:http://savings-service:8081}") String baseUrl) {
        this.rest = builder.build();
        // 末尾スラッシュは重複させない
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * 現在のリクエストに付いてきたユーザの JWT を取り出して Bearer ヘッダ文字列を作る
     */
    private String bearer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return "Bearer " + jwtAuth.getToken().getTokenValue();
        }
        throw new IllegalStateException("No JWT available to propagate (Authorization header is required)");
    }


    // @SuppressWarnings("unchecked")
    // SavingsClient に “ヘッダ付き版” を追加（既存メソッドはそのまま）
    // 既存：3引数版（Idempotency-Key あり）
    // public Map<String, Object> deposit(UUID accountId, BigDecimal amount, String idempotencyKey) {
    //     return rest.post()
    //             .uri(this.baseUrl + "/accounts/{id}/deposit", accountId)
    //             .contentType(MediaType.APPLICATION_JSON)
    //             .header(HttpHeaders.AUTHORIZATION, bearer())
    //             .header("Idempotency-Key", idempotencyKey)
    //             .body(Map.of("amount", amount))
    //             .retrieve()
    //             .body(Map.class);
    // }
    @SuppressWarnings("unchecked")
    // SavingsClient に “ヘッダ付き版” を追加（既存メソッドはそのまま）
    // 既存：3引数版（Idempotency-Key あり）
    public Map<String, Object> deposit(UUID accountId, BigDecimal amount, String idempotencyKey) {
        RestClient.RequestBodySpec req = rest.post()
                .uri(this.baseUrl + "/accounts/{id}/deposit", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer());

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            req = req.header("Idempotency-Key", idempotencyKey);
        }

        return req
                .body(Map.of("amount", amount))
                .retrieve()
                .body(Map.class);
    }

    // ★ 復活：2引数版（互換用） → 3引数版を null で委譲
    public Map<String, Object> deposit(UUID accountId, BigDecimal amount) {
        return deposit(accountId, amount, null);
    }


    // @SuppressWarnings("unchecked")
    // SavingsClient に “ヘッダ付き版” を追加（既存メソッドはそのまま）
    // 既存：3引数版（Idempotency-Key あり）
    // public Map<String, Object> withdraw(UUID accountId, BigDecimal amount, String idempotencyKey) {
    //     return rest.post()
    //             .uri(this.baseUrl + "/accounts/{id}/withdraw", accountId)
    //             .contentType(MediaType.APPLICATION_JSON)
    //             .header(HttpHeaders.AUTHORIZATION, bearer())
    //             .header("Idempotency-Key", idempotencyKey)
    //             .body(Map.of("amount", amount))
    //             .retrieve()
    //             .body(Map.class);
    // }
    @SuppressWarnings("unchecked")
    // SavingsClient に “ヘッダ付き版” を追加（既存メソッドはそのまま）
    // 既存：3引数版（Idempotency-Key あり）
    public Map<String, Object> withdraw(UUID accountId, BigDecimal amount, String idempotencyKey) {
        RestClient.RequestBodySpec req = rest.post()
                .uri(this.baseUrl + "/accounts/{id}/withdraw", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer());

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            req = req.header("Idempotency-Key", idempotencyKey);
        }

        return req
                .body(Map.of("amount", amount))
                .retrieve()
                .body(Map.class);
    }

    // ★ 復活：2引数版（互換用） → 3引数版を null で委譲
    public Map<String, Object> withdraw(UUID accountId, BigDecimal amount) {
        return withdraw(accountId, amount, null);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAccount(UUID accountId) {
        return rest.get()
                .uri(this.baseUrl + "/accounts/{id}", accountId)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createAccount(String owner) {
        return rest.post()
                .uri(this.baseUrl + "/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .body(Map.of("owner", owner))
                .retrieve()
                .body(Map.class);
    }
}