package com.example.timedeposit.service;

import com.example.timedeposit.api.TransferRequest;
import com.example.timedeposit.client.SavingsClient;
import com.example.timedeposit.model.TimeDeposit; // ← model パッケージ
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
    @Transactional(noRollbackFor = Exception.class)
    public UUID transfer(TransferRequest req) {
        // 1) 出金（普通預金）
        savingsClient.withdraw(req.fromAccountId(), req.principal());

        UUID tdId = null;
        try {
            // 2) 定期預金作成（既存サービスの create を利用）
            TimeDeposit td = timeDepositService.create(
                    req.owner(),
                    req.principal(),
                    req.annualRate(),
                    req.termDays()
            );
            tdId = td.getId();
            return tdId;

        } catch (Exception e) {
            // 3) 補償（出金を戻す）
            try {
                savingsClient.deposit(req.fromAccountId(), req.principal());
            } catch (Exception compensateEx) {
                // 補償失敗はログ対象：必要ならロギングを追加
            }
            throw e;
        }
    }
}
