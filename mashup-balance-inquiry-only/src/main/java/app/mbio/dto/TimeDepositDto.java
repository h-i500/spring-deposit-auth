package app.mbio.dto;

// import java.math.BigDecimal;

// public record TimeDepositDto(String contractNo, String owner, BigDecimal principal) {}
public class TimeDepositDto {
    public String id;
    public String accountNo;
    public String ownerKey;
    public java.math.BigDecimal principal;
    public java.math.BigDecimal balance; // BFF の返却に合わせて
}
