package com.example.timedeposit.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class SavingsClient {

    private final RestClient client;

    public SavingsClient(@Value("${savings.base-url}") String baseUrl) {
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public void withdraw(UUID accountId, BigDecimal amount) {
        client.post()
              .uri("/accounts/{id}/withdraw", accountId)
              .contentType(MediaType.APPLICATION_JSON)
              .body(Map.of("amount", amount))
              .retrieve()
              .toBodilessEntity();
    }

    public void deposit(UUID accountId, BigDecimal amount) {
        client.post()
              .uri("/accounts/{id}/deposit", accountId)
              .contentType(MediaType.APPLICATION_JSON)
              .body(Map.of("amount", amount))
              .retrieve()
              .toBodilessEntity();
    }

    public Map<?,?> getAccount(UUID accountId) {
        return client.get()
                .uri("/accounts/{id}", accountId)
                .retrieve()
                .body(Map.class);
    }
}
