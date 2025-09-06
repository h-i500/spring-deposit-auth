package com.example.timedeposit.controller;

import com.example.timedeposit.api.TransferRequest;
import com.example.timedeposit.api.TransferResponse;
import com.example.timedeposit.service.TransferService;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    // 定期預金申込は user 権限
    // @PreAuthorize("hasRole('user')")
    @PostMapping("/deposits/from-savings")
    public ResponseEntity<?> createFromSavings(
        @RequestHeader(value = "Idempotency-Key", required = false) String key,
        @RequestBody TransferRequest req
    ) {
        UUID tdId = service.transfer(req, key);
        return ResponseEntity.status(201).body(Map.of("id", tdId));
    }


    // 疎通確認は公開
    @PermitAll
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
