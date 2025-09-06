package com.example.timedeposit.dto;

import com.example.timedeposit.model.TimeDeposit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TimeDepositDto(
    UUID id,
    String owner,
    BigDecimal principal,
    BigDecimal annualRate,
    int termDays,
    Instant startAt,
    Instant maturityAt,
    String status,
    BigDecimal payoutAmount,
    UUID payoutAccount,
    Instant closedAt
) {
  public static TimeDepositDto fromEntity(TimeDeposit t) {
    if (t == null) return null;
    return new TimeDepositDto(
        t.getId(),
        t.getOwner(),
        t.getPrincipal(),
        t.getAnnualRate(),
        t.getTermDays(),
        t.getStartAt(),
        t.getMaturityAt(),
        t.getStatus() != null ? t.getStatus().name() : null,
        t.getPayoutAmount(),
        t.getPayoutAccount(),
        t.getClosedAt()
    );
  }
}
