package com.example.savings.dto;

import com.example.savings.model.Account;  // ← 実体
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SavingsAccountDto(
    UUID id,
    String owner,
    BigDecimal balance,
    Instant createdAt,
    Long version
) {
  public static SavingsAccountDto fromEntity(Account a) {
    if (a == null) return null;
    return new SavingsAccountDto(
        a.getId(),
        a.getOwner(),
        a.getBalance(),
        a.getCreatedAt(),
        a.getVersion()
    );
  }
}
