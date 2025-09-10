package app.mbio.dto;

import java.util.List;

public record BalanceResponse(
    String owner,
    List<SavingsAccountDto> savings,
    List<TimeDepositDto> timeDeposits
) {
    public static BalanceResponse of(String owner, List<SavingsAccountDto> s, List<TimeDepositDto> t) {
        return new BalanceResponse(owner, s, t);
    }
}
