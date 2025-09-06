package com.example.timedeposit.api;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        UUID fromAccountId,     // 普通預金の口座ID（出金元）
        String owner,           // 定期預金の名義
        BigDecimal principal,   // 出金額 = 元本
        BigDecimal annualRate,  // 年率（例: 0.015）
        int termDays
) {}
