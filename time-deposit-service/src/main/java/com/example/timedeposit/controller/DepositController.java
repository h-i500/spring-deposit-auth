package com.example.timedeposit.controller;

import com.example.timedeposit.client.SavingsClient;
import com.example.timedeposit.model.TimeDeposit;
import com.example.timedeposit.service.TimeDepositService;
import jakarta.validation.constraints.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/deposits")
public class DepositController {
    private final TimeDepositService service;
    private final SavingsClient savingsClient;  

    public DepositController(TimeDepositService service,
                             SavingsClient savingsClient) { 
        this.service = service;
        this.savingsClient = savingsClient;
    }

    public record CreateRequest(@NotBlank String owner,
                                @NotNull @DecimalMin("0.01") BigDecimal principal,
                                @NotNull @DecimalMin("0.0") BigDecimal annualRate,
                                @Min(1) int termDays,
                                UUID fromAccountId // ← null 可：指定がある時だけ withdraw を実施
                                ) {}

    @PostMapping
    public ResponseEntity<?> create(
            // ★ 冪等化の準備（下流に伝搬するため受け取る）
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @RequestBody CreateRequest req
    ) {
        // 1) 「普通から引落し」なら先に withdraw
        UUID from = req.fromAccountId();
        BigDecimal amount = req.principal();
        boolean withdrew = false;

        if (from != null) {
            // withdraw 実行（Idempotency-Key は :WD を付けて下流に伝搬）
            // 既存の SavingsClient#withdraw をそのまま利用
            // savingsClient.withdraw(from, amount);
            savingsClient.withdraw(from, amount, addSfx(idemKey, ":WD"));
            withdrew = true;
        }

        try {
            // 2) 定期作成
            TimeDeposit td = service.create(req.owner(), amount, req.annualRate(), req.termDays());
            return ResponseEntity.status(201).body(Map.of(
                    "id", td.getId(),
                    "owner", td.getOwner(),
                    "principal", td.getPrincipal(),
                    "annualRate", td.getAnnualRate(),
                    "termDays", td.getTermDays(),
                    "startAt", td.getStartAt(),
                    "maturityDate", td.getMaturityAt(),
                    "status", td.getStatus()
            ));
        } catch (RuntimeException e) {
            // 3) 失敗時は補償（引落し済みなら同額を deposit で戻す）
            if (withdrew && from != null) {
                try {
                    // savingsClient.deposit(from, amount);
                    savingsClient.deposit(from, amount, addSfx(idemKey, ":CP")); // 補償
                } catch (Exception ce) {
                    // 補償失敗はログだけ残して元の例外を再送出（運用で検知）
                    // log.error("compensation failed", ce);
                }
            }
            throw e;
        }
    }

    private static String addSfx(String key, String sfx) {
        return (key == null || key.isBlank()) ? null : key + sfx;
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        TimeDeposit td = service.get(id);
        return ResponseEntity.ok(Map.of(
                "id", td.getId(),
                "owner", td.getOwner(),
                "principal", td.getPrincipal(),
                "annualRate", td.getAnnualRate(),
                "termDays", td.getTermDays(),
                "startAt", td.getStartAt(),
                "maturityDate", td.getMaturityAt(),
                "status", td.getStatus()
        ));
    }

    // 解約時に普通預金へ自動振替
    @PostMapping("/{id}/close")
    public ResponseEntity<?> closeAndTransfer(@PathVariable UUID id,
                                              @RequestParam(name = "toAccountId") UUID toAccountId,
                                              @RequestParam(name = "at", required = false) String atIso,
                                              @RequestHeader(value = "Idempotency-Key", required = false) String idemKey) {
        Instant now = (atIso == null) ? Instant.now() : Instant.parse(atIso);
        BigDecimal payout = service.closeAndTransfer(id, toAccountId, now, savingsClient, addSfx(idemKey, ":CLOSE"));
        return ResponseEntity.ok(Map.of(
                "id", id,
                "status", "CLOSED",
                "payout", payout,
                "toAccountId", toAccountId
        ));
    }



    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<?> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}