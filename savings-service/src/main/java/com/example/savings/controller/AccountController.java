package com.example.savings.controller;

import com.example.savings.model.Account;
import com.example.savings.repository.AccountRepository;
import com.example.savings.service.AccountService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) { this.service = service; }

    public record CreateAccountRequest(@NotBlank String owner) {}
    public record MoneyRequest(@NotNull BigDecimal amount) {}

    // // 作成は “user” 権限
    @PreAuthorize("hasRole('user')")
    @PostMapping({"", "/"})
    public ResponseEntity<?> create(@RequestBody CreateAccountRequest req) {
        Account a = service.create(req.owner());
        return ResponseEntity.ok(Map.of(
                "id", a.getId(),
                "owner", a.getOwner(),
                "balance", a.getBalance(),
                "createdAt", a.getCreatedAt()
        ));
    }

    // 参照は “read” 権限
    @PreAuthorize("hasRole('read')")
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        Account a = service.get(id);
        return ResponseEntity.ok(Map.of(
                "id", a.getId(),
                "owner", a.getOwner(),
                "balance", a.getBalance(),
                "createdAt", a.getCreatedAt()
        ));
    }

    // 入金は “user” 権限
    @PreAuthorize("hasRole('user')")
    @PostMapping("/{id}/deposit")
    public ResponseEntity<?> deposit(@PathVariable UUID id, @RequestBody MoneyRequest req) {
        Account a = service.deposit(id, req.amount());
        return ResponseEntity.ok(Map.of(
                "id", a.getId(),
                "balance", a.getBalance()
        ));
    }

    // 出金も “user” 権限
    @PreAuthorize("hasRole('user')")
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable UUID id, @RequestBody MoneyRequest req) {
        Account a = service.withdraw(id, req.amount());
        return ResponseEntity.ok(Map.of(
                "id", a.getId(),
                "balance", a.getBalance()
        ));
    }

    // ▼ 一覧（owner 指定）— Service 経由に
    @PreAuthorize("hasRole('read')")
    @GetMapping
    public List<Account> listByOwner(@RequestParam("owner") String owner) {
        if (owner == null || owner.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query param 'owner' is required");
        }
        return service.findByOwner(owner);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<?> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

}
