package com.example.timedeposit.service;

import com.example.timedeposit.api.TransferRequest;          // あれば
import com.example.timedeposit.client.SavingsClient;          // ← これが正
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

    @Transactional
    public BigDecimal closeAndTransfer(UUID id, UUID toAccountId, Instant now, SavingsClient savingsClient) {
    TimeDeposit td = get(id);

    if (td.getStatus() == TimeDeposit.Status.CLOSED) {
        // 冪等：既に閉鎖済みなら記録値を返す
        return td.getPayoutAmount();
    }

    if (now.isBefore(td.getMaturityAt())) {
        throw new IllegalStateException("not matured yet");
    }

    // まず CLOSING にして同時実行をブロック（この変更はトランザクション内で確定）
    td.setStatus(TimeDeposit.Status.CLOSING);

    // 払戻額を計算
    BigDecimal payout = calculatePayout(td);

    // 外部呼び出し：Savings に入金（ネットワークなのでトランザクション外部）
    // 失敗したら例外となり、このトランザクションはロールバック → 状態は OPEN のままに戻る
    savingsClient.deposit(toAccountId, payout);

    // 入金成功 → 閉鎖確定
    td.setStatus(TimeDeposit.Status.CLOSED);
    td.setPayoutAmount(payout);
    td.setPayoutAccount(toAccountId);
    td.setClosedAt(now);

    return payout;
    }
}