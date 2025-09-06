package com.example.timedeposit.service;

import com.example.timedeposit.api.TransferRequest;
import com.example.timedeposit.client.SavingsClient;
import com.example.timedeposit.model.TimeDeposit;
import com.example.timedeposit.repository.TimeDepositRepository;

// import main.java.com.example.timedeposit.client.SavingsClient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class TimeDepositService {
    private final TimeDepositRepository repo;

    public TimeDepositService(TimeDepositRepository repo) {
        this.repo = repo;
    }

    public TimeDeposit create(String owner, BigDecimal principal, BigDecimal annualRate, int termDays) {
        if (principal == null || principal.signum() <= 0) throw new IllegalArgumentException("principal must be > 0");
        if (annualRate == null || annualRate.signum() < 0) throw new IllegalArgumentException("annualRate must be >= 0");
        if (termDays <= 0) throw new IllegalArgumentException("termDays must be > 0");
        TimeDeposit td = new TimeDeposit();
        td.setOwner(owner);
        td.setPrincipal(principal.setScale(2, RoundingMode.HALF_UP));
        td.setAnnualRate(annualRate);
        td.setTermDays(termDays);
        return repo.save(td);
    }

    public TimeDeposit get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("deposit not found"));
    }

    /** 単利: principal * (1 + annualRate * termDays/365) */
    public BigDecimal calculatePayout(TimeDeposit td) {
        BigDecimal days = new BigDecimal(td.getTermDays());
        BigDecimal factor = BigDecimal.ONE.add(td.getAnnualRate().multiply(days).divide(new BigDecimal("365"), MathContext.DECIMAL64));
        return td.getPrincipal().multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public BigDecimal close(UUID id, Instant now) {
        TimeDeposit td = get(id);
        if (td.getStatus() == TimeDeposit.Status.CLOSED) throw new IllegalStateException("already closed");
        if (now.isBefore(td.getMaturityAt())) throw new IllegalStateException("not matured yet");
        BigDecimal payout = calculatePayout(td);
        td.setStatus(TimeDeposit.Status.CLOSED);
        return payout;
    }

    // @Tran
    
    @Transactional
public BigDecimal closeAndTransfer(UUID id, UUID toAccountId, Instant now,
                                   SavingsClient savingsClient, String idempotencyKey) {
    TimeDeposit td = get(id);
    if (td.getStatus() == TimeDeposit.Status.CLOSED) {
        return td.getPayoutAmount(); // 冪等
    }
    if (now.isBefore(td.getMaturityAt())) {
        throw new IllegalStateException("not matured yet");
    }
    td.setStatus(TimeDeposit.Status.CLOSING);

    BigDecimal payout = calculatePayout(td);

    String closeKey = (idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey + ":CLOSE";
    if (closeKey != null) {
        savingsClient.deposit(toAccountId, payout, closeKey);
    } else {
        savingsClient.deposit(toAccountId, payout);
    }

    td.setStatus(TimeDeposit.Status.CLOSED);
    td.setPayoutAmount(payout);
    td.setPayoutAccount(toAccountId);
    td.setClosedAt(now);
    return payout;
}

}