package com.example.timedeposit.service;

import com.example.timedeposit.client.SavingsClient;
import com.example.timedeposit.model.TimeDeposit;
import com.example.timedeposit.repository.TimeDepositRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TimeDepositService のユニットテスト。
 *
 * 目的：
 * - create() の入力バリデーションと金額の丸め（HALF_UP）を確認
 * - close() の成熟前/二重クローズのエラーを確認
 * - closeAndTransfer() での計算（単利）・状態遷移・下流 deposit 呼び出し
 * - closeAndTransfer() の冪等性（CLOSED 済みなら下流呼び出しなし）
 *
 * リポジトリはモック化し、サービス単体の振る舞いに集中する。
 */
class TimeDepositServiceTest {

    private static TimeDepositRepository repoMock() {
        return mock(TimeDepositRepository.class);
    }

    /**
     * テスト用の TimeDeposit を作成するヘルパー。
     * 満期判定や Map.of を安全にするため、必要フィールドを埋める。
     */
    private static TimeDeposit tdNew(String owner, String principal, String rate, int days) {
        var td = new TimeDeposit();
        ReflectionTestUtils.setField(td, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(td, "owner", owner);
        ReflectionTestUtils.setField(td, "principal", new BigDecimal(principal));
        ReflectionTestUtils.setField(td, "annualRate", new BigDecimal(rate));
        ReflectionTestUtils.setField(td, "termDays", days);
        ReflectionTestUtils.setField(td, "status", TimeDeposit.Status.OPEN);
        Instant start = Instant.now();
        ReflectionTestUtils.setField(td, "startAt", start);
        ReflectionTestUtils.setField(td, "maturityAt", start.plus(days, ChronoUnit.DAYS));
        return td;
    }

    /**
     * create(): 金額の丸め（HALF_UP）と設定値の反映を確認。
     */
    @Test
    void create_shouldValidateAndRound() {
        TimeDepositRepository repo = repoMock();
        TimeDepositService service = new TimeDepositService(repo);

        // save の戻り値は引数そのまま返す簡易スタブ（ID 採番の有無は本テストでは不要）
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var saved = service.create("alice", new BigDecimal("100.005"), new BigDecimal("0.05"), 30);
        assertThat(saved.getPrincipal()).isEqualTo(new BigDecimal("100.01")); // HALF_UP
        assertThat(saved.getAnnualRate()).isEqualTo(new BigDecimal("0.05"));
        assertThat(saved.getTermDays()).isEqualTo(30);
    }

    /**
     * create(): 不正入力（principal<=0, annualRate<0, termDays<=0）は例外。
     */
    @Test
    void create_shouldRejectInvalid() {
        TimeDepositService service = new TimeDepositService(repoMock());
        assertThatThrownBy(() -> service.create("a", new BigDecimal("0.00"), BigDecimal.ZERO, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create("a", new BigDecimal("1"), new BigDecimal("-0.01"), 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create("a", new BigDecimal("1"), BigDecimal.ZERO, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * close(): 満期前はエラー、CLOSED 済みもエラー。
     */
    @Test
    void close_shouldFailIfNotMaturedOrAlreadyClosed() {
        TimeDepositRepository repo = repoMock();
        TimeDepositService service = new TimeDepositService(repo);

        var id = UUID.randomUUID();
        var td = tdNew("alice", "1000.00", "0.05", 10);
        when(repo.findById(id)).thenReturn(Optional.of(td));

        // 満期前は IllegalStateException
        assertThatThrownBy(() -> service.close(id, td.getStartAt().plus(1, ChronoUnit.DAYS)))
                .isInstanceOf(IllegalStateException.class);

        // 既に CLOSED なら IllegalStateException
        ReflectionTestUtils.setField(td, "status", TimeDeposit.Status.CLOSED);
        assertThatThrownBy(() -> service.close(id, Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * closeAndTransfer(): 満期後に deposit が呼ばれ、:CLOSE キーが付与される。
     * 状態が CLOSED になり、支払額・口座・日時がセットされる。
     */
    @Test
    void closeAndTransfer_shouldDepositThenFinalize_withIdempotencyKey() {
        TimeDepositRepository repo = repoMock();
        TimeDepositService service = new TimeDepositService(repo);

        var id = UUID.randomUUID();
        var to = UUID.randomUUID();
        var td = tdNew("alice", "1000.00", "0.10", 365);
        when(repo.findById(id)).thenReturn(Optional.of(td));

        SavingsClient savings = mock(SavingsClient.class);

        var payout = service.closeAndTransfer(id, to, td.getMaturityAt(), savings, "IDEMPOTENT-KEY");
        // 単利: 1000 * (1 + 0.1 * 365/365) = 1100.00
        assertThat(payout).isEqualTo(new BigDecimal("1100.00"));

        // 下流呼び出しは :CLOSE キー付き
        verify(savings).deposit(eq(to), eq(new BigDecimal("1100.00")), eq("IDEMPOTENT-KEY:CLOSE"));

        // 状態遷移と出力フィールド
        assertThat(td.getStatus()).isEqualTo(TimeDeposit.Status.CLOSED);
        assertThat(td.getPayoutAmount()).isEqualByComparingTo("1100.00");
        assertThat(td.getPayoutAccount()).isEqualTo(to);
        assertThat(td.getClosedAt()).isNotNull();
    }

    /**
     * closeAndTransfer(): 既に CLOSED なら冪等的に同額を返し、下流呼び出しは発生しない。
     */
    @Test
    void closeAndTransfer_shouldBeIdempotentWhenAlreadyClosed() {
        TimeDepositRepository repo = repoMock();
        TimeDepositService service = new TimeDepositService(repo);

        var id = UUID.randomUUID();
        var to = UUID.randomUUID();
        var td = tdNew("alice", "1000.00", "0.10", 365);
        ReflectionTestUtils.setField(td, "status", TimeDeposit.Status.CLOSED);
        ReflectionTestUtils.setField(td, "payoutAmount", new BigDecimal("1100.00"));
        when(repo.findById(id)).thenReturn(Optional.of(td));

        SavingsClient savings = mock(SavingsClient.class);

        var payout = service.closeAndTransfer(id, to, Instant.now(), savings, "K");
        assertThat(payout).isEqualByComparingTo("1100.00");
        verifyNoInteractions(savings); // 冪等：下流を呼ばない
    }
}
