package app.mbio.dto;

import java.util.List;

public record BalanceResponse(
    String owner,
    List<SavingsDto> savings,
    List<TimeDepositDto> timeDeposits
) {
    public static BalanceResponse of(String owner, List<SavingsDto> s, List<TimeDepositDto> t) {
        return new BalanceResponse(owner, s, t);
    }
}
