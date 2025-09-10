package app.mbio.dto;

import java.math.BigDecimal;

public record SavingsDto(String accountNo, String owner, BigDecimal balance) {}
