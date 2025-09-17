package com.example.timedeposit.service;

import com.example.timedeposit.api.TransferRequest;
import com.example.timedeposit.client.SavingsClient;
import com.example.timedeposit.model.TimeDeposit;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TransferService のユニットテスト。
 *
 * 目的：
 * - 正常系：Idempotency-Key あり → withdraw(:WD) → create 成功 → deposit 補償は無し
 * - 異常系：create が失敗 → withdraw 後に補償 deposit(:CP) → 例外再送出
 * - 互換系：Idempotency-Key 無し → 2引数 withdraw が使われる（ヘッダ無し）
 *
 * SavingsClient と TimeDepositService をモックし、オーケストレーションの分岐と
 * キーサフィックス付与の正しさを検証する。
 */
class TransferServiceTest {

    /**
     * 正常系（Idempotency-Key あり）：
     * - withdraw は 3引数で :WD が付与される
     * - create 成功なら deposit 補償は呼ばれない
     * - 戻り値は作成された TimeDeposit の ID
     */
    @Test
    void transfer_shouldWithdrawThenCreate_andReturnId_withIdempotencyKey() {
        // Mocks
        SavingsClient savings = mock(SavingsClient.class);
        TimeDepositService tdService = mock(TimeDepositService.class);

        // System under test
        TransferService svc = new TransferService(savings, tdService);

        // 入力はモック化（record のシグネチャ変更に強くするため）
        TransferRequest req = mock(TransferRequest.class);
        UUID from = UUID.randomUUID();
        when(req.owner()).thenReturn("alice");
        when(req.principal()).thenReturn(new BigDecimal("100.00"));
        when(req.annualRate()).thenReturn(new BigDecimal("0.05"));
        when(req.termDays()).thenReturn(30);
        when(req.fromAccountId()).thenReturn(from);

        // create の戻り値（ID は戻り値検証に使う）
        TimeDeposit td = new TimeDeposit();
        UUID tdId = UUID.randomUUID();
        ReflectionTestUtils.setField(td, "id", tdId);
        ReflectionTestUtils.setField(td, "owner", "alice");
        ReflectionTestUtils.setField(td, "principal", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(td, "annualRate", new BigDecimal("0.05"));
        ReflectionTestUtils.setField(td, "termDays", 30);
        when(tdService.create(any(), any(), any(), anyInt())).thenReturn(td);

        // 実行
        UUID result = svc.transfer(req, "IDEMPOTENT");

        // 検証
        assertThat(result).isEqualTo(tdId);
        verify(savings).withdraw(eq(from), eq(new BigDecimal("100.00")), eq("IDEMPOTENT:WD"));
        verify(savings, never()).deposit(any(UUID.class), any(BigDecimal.class));
        verify(savings, never()).deposit(any(UUID.class), any(BigDecimal.class), anyString());
    }

    /**
     * 異常系：
     * - create が失敗した場合、補償として同額の deposit(:CP) が呼ばれ、
     *   その後に例外が再送出される。
     */
    @Test
    void transfer_shouldCompensateDeposit_whenCreateFails_withIdempotencyKey() {
        SavingsClient savings = mock(SavingsClient.class);
        TimeDepositService tdService = mock(TimeDepositService.class);
        TransferService svc = new TransferService(savings, tdService);

        TransferRequest req = mock(TransferRequest.class);
        UUID from = UUID.randomUUID();
        when(req.owner()).thenReturn("alice");
        when(req.principal()).thenReturn(new BigDecimal("50.00"));
        when(req.annualRate()).thenReturn(new BigDecimal("0.02"));
        when(req.termDays()).thenReturn(10);
        when(req.fromAccountId()).thenReturn(from);

        when(tdService.create(any(), any(), any(), anyInt()))
                .thenThrow(new RuntimeException("create failed"));

        assertThatThrownBy(() -> svc.transfer(req, "KEY"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("create failed");

        verify(savings).withdraw(eq(from), eq(new BigDecimal("50.00")), eq("KEY:WD"));
        verify(savings).deposit(eq(from), eq(new BigDecimal("50.00")), eq("KEY:CP"));
    }

    /**
     * 互換系（Idempotency-Key なし）：
     * - withdraw は 2引数版が呼ばれる（= ヘッダなし）
     * - 成功時は deposit は呼ばれない
     */
    @Test
    void transfer_shouldUseLegacyTwoArg_whenNoIdempotencyKey() {
        SavingsClient savings = mock(SavingsClient.class);
        TimeDepositService tdService = mock(TimeDepositService.class);
        TransferService svc = new TransferService(savings, tdService);

        TransferRequest req = mock(TransferRequest.class);
        UUID from = UUID.randomUUID();
        when(req.owner()).thenReturn("bob");
        when(req.principal()).thenReturn(new BigDecimal("200.00"));
        when(req.annualRate()).thenReturn(new BigDecimal("0.03"));
        when(req.termDays()).thenReturn(60);
        when(req.fromAccountId()).thenReturn(from);

        TimeDeposit td = new TimeDeposit();
        ReflectionTestUtils.setField(td, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(td, "owner", "bob");
        ReflectionTestUtils.setField(td, "principal", new BigDecimal("200.00"));
        ReflectionTestUtils.setField(td, "annualRate", new BigDecimal("0.03"));
        ReflectionTestUtils.setField(td, "termDays", 60);
        when(tdService.create(any(), any(), any(), anyInt())).thenReturn(td);

        UUID id = svc.transfer(req, null);
        assertThat(id).isNotNull();

        // 2引数版が呼ばれ、3引数版は呼ばれない
        verify(savings).withdraw(eq(from), eq(new BigDecimal("200.00"))); // 2-arg
        verify(savings, never()).withdraw(any(UUID.class), any(BigDecimal.class), anyString());

        verify(savings, never()).deposit(any(UUID.class), any(BigDecimal.class));
        verify(savings, never()).deposit(any(UUID.class), any(BigDecimal.class), anyString());
    }
}
