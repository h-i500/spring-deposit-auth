package com.example.timedeposit.service;

import com.example.timedeposit.api.TransferRequest;
import com.example.timedeposit.client.SavingsClient;
import com.example.timedeposit.model.TimeDeposit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransferService {

    private final SavingsClient savingsClient;
    private final TimeDepositService timeDepositService;

    public TransferService(SavingsClient savingsClient,
                           TimeDepositService timeDepositService) {
        this.savingsClient = savingsClient;
        this.timeDepositService = timeDepositService;
    }

    /**
     * オーケストレーション：withdraw → create TD（失敗時は補償 deposit）
     */
    // ← ここポイント：Idempotency-Key を受け取る引数を追加
    @Transactional
    public UUID transfer(TransferRequest req, String idempotencyKey) {
        String wdKey = addSfx(idempotencyKey, ":WD");
        String cpKey = addSfx(idempotencyKey, ":CP");

        // 1) 出金（普通預金）
        if (wdKey != null) {
            savingsClient.withdraw(req.fromAccountId(), req.principal(), wdKey);
        } else {
            savingsClient.withdraw(req.fromAccountId(), req.principal()); // 従来互換
        }

        try {
            // 2) 定期預金作成（既存サービスの create を利用）
            TimeDeposit td = timeDepositService.create(
                    req.owner(),
                    req.principal(),
                    req.annualRate(),
                    req.termDays()
            );
            return td.getId();

        } catch (Exception e) {
            // 3) 補償（出金を戻す）
            try {
                if (cpKey != null) {
                    savingsClient.deposit(req.fromAccountId(), req.principal(), cpKey);
                } else {
                    savingsClient.deposit(req.fromAccountId(), req.principal()); // 従来互換
                }
            } catch (Exception compensateEx) {
                // TODO: ログ/アラート。補償失敗は要監視
            }
            throw e;
        }
    }

    private static String addSfx(String key, String sfx) {
        return (key == null || key.isBlank()) ? null : key + sfx;
    }
}

