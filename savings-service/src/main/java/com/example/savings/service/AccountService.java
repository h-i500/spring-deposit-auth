package com.example.savings.service;

import com.example.savings.model.Account;
import com.example.savings.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AccountService {
    private final AccountRepository repo;

    public AccountService(AccountRepository repo) {
        this.repo = repo;
    }

    public Account create(String owner) {
        Account a = new Account();
        a.setOwner(owner);
        a.setBalance(BigDecimal.ZERO);
        return repo.save(a);
    }

    public Account get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    @Transactional
    public Account deposit(UUID id, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amount must be > 0");
        Account a = get(id);
        a.setBalance(a.getBalance().add(amount));
        return a;
    }

    @Transactional
    public Account withdraw(UUID id, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amount must be > 0");
        Account a = get(id);
        if (a.getBalance().compareTo(amount) < 0) throw new IllegalStateException("insufficient funds");
        a.setBalance(a.getBalance().subtract(amount));
        return a;
    }
}