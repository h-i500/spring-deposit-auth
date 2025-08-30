package com.example.timedeposit.api;

import java.util.UUID;

public record TransferResponse(
        UUID fromAccountId,
        UUID timeDepositId,
        String status // "COMPLETED"
) {}
