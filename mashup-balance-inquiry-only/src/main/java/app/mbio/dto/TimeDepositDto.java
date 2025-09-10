package app.mbio.dto;

import java.math.BigDecimal;

public record TimeDepositDto(String contractNo, String owner, BigDecimal principal) {}
