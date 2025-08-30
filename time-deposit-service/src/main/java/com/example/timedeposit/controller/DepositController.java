package com.example.timedeposit.controller;

import com.example.timedeposit.client.SavingsClient;     // 追加
import com.example.timedeposit.model.TimeDeposit;
import com.example.timedeposit.service.TimeDepositService;
import jakarta.validation.constraints.*;
// import main.java.com.example.timedeposit.controller.DepositController.CreateRequest;

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
    private final SavingsClient savingsClient;           // 追加

    // public DepositController(TimeDepositService service) { this.service = service; }
    public DepositController(TimeDepositService service,
                             SavingsClient savingsClient) { // 追加
                                this.service = service;
        this.savingsClient = savingsClient;              // 追加
    }

    public record CreateRequest(@NotBlank String owner,
                                @NotNull @DecimalMin("0.01") BigDecimal principal,
                                @NotNull @DecimalMin("0.0") BigDecimal annualRate,
                                @Min(1) int termDays) {}

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateRequest req) {
        TimeDeposit td = service.create(req.owner(), req.principal(), req.annualRate(), req.termDays());
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

    // @PostMapping("/{id}/close")
    // public ResponseEntity<?> close(@PathVariable UUID id) {
    //     var payout = service.close(id, Instant.now());
    //     return ResponseEntity.ok(Map.of("id", id, "payout", payout));
    // }
    //     @PostMapping("/{id}/close")
    //     public ResponseEntity<?> close(@PathVariable UUID id,
    //                                @RequestParam(name = "at", required = false) String atIso) {
    //     var now = (atIso == null) ? Instant.now() : Instant.parse(atIso);
    //     var payout = service.close(id, now);
    //     return ResponseEntity.ok(Map.of("id", id, "payout", payout));
    // }
    // @PostMapping("/{id}/close")
    // public ResponseEntity<?> closeAndTransfer(@PathVariable UUID id,
    //                                         @RequestParam(name = "toAccountId") UUID toAccountId,
    //                                         @RequestParam(name = "at", required = false) String atIso) {
    //     Instant now = (atIso == null) ? Instant.now() : Instant.parse(atIso);
    //     BigDecimal payout = service.closeAndTransfer(id, toAccountId, now, savingsClient);
    //     return ResponseEntity.ok(Map.of(
    //             "id", id,
    //             "status", "CLOSED",
    //             "payout", payout,
    //             "toAccountId", toAccountId
    //     ));
    // }

    // 解約時に普通預金へ自動振替
    @PostMapping("/{id}/close")
    public ResponseEntity<?> closeAndTransfer(@PathVariable UUID id,
                                              @RequestParam(name = "toAccountId") UUID toAccountId,
                                              @RequestParam(name = "at", required = false) String atIso) {
        Instant now = (atIso == null) ? Instant.now() : Instant.parse(atIso);
        BigDecimal payout = service.closeAndTransfer(id, toAccountId, now, savingsClient);
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