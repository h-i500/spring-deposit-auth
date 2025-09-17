package com.example.timedeposit.controller;

import com.example.timedeposit.client.SavingsClient;
import com.example.timedeposit.model.TimeDeposit;
import com.example.timedeposit.service.TimeDepositService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DepositController の Web 層テスト。
 *
 * 目的：
 * - 正常系：fromAccountId が指定されたときに SavingsClient#withdraw が呼ばれ、
 *   Idempotency-Key に :WD が付いて伝搬することを確認。
 * - 正常系：fromAccountId が無いときは withdraw しないことを確認。
 * - 例外系：TimeDepositService#create が例外を投げた場合、補償として
 *   SavingsClient#deposit が :CP キーで呼ばれ、その後に例外が再送出されることを確認。
 *
 * 注意：
 * - セキュリティフィルタは無効化（@AutoConfigureMockMvc(addFilters = false)）。
 *   コントローラの振る舞いにだけ集中するため。
 * - Map.of(...) は null を許容しないため、モックで返す TimeDeposit の
 *   id/startAt/maturityAt/status を必ず埋める。
 */
@WebMvcTest(controllers = DepositController.class)
@AutoConfigureMockMvc(addFilters = false)
class DepositControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    TimeDepositService service;   // create() をモック

    @MockBean
    SavingsClient savingsClient;  // 下流呼び出しの検証用モック

    /**
     * テスト用の TimeDeposit を作成するヘルパー。
     * Map.of に渡す値が null にならないよう、最低限のフィールドを埋める。
     */
    private static TimeDeposit tdReady(String owner, BigDecimal principal, BigDecimal rate, int days) {
        var td = new TimeDeposit();
        ReflectionTestUtils.setField(td, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(td, "owner", owner);
        ReflectionTestUtils.setField(td, "principal", principal);
        ReflectionTestUtils.setField(td, "annualRate", rate);
        ReflectionTestUtils.setField(td, "termDays", days);
        ReflectionTestUtils.setField(td, "status", TimeDeposit.Status.OPEN);
        Instant start = Instant.now();
        ReflectionTestUtils.setField(td, "startAt", start);
        ReflectionTestUtils.setField(td, "maturityAt", start.plus(days, ChronoUnit.DAYS));
        return td;
    }

    /**
     * 正常系：
     * - fromAccountId が指定された場合、SavingsClient#withdraw が呼ばれること
     * - Idempotency-Key が :WD サフィックス付きで下流に伝搬すること
     * - レスポンスが 201 で ID 等が返ること
     */
    @Test
    void create_shouldWithdrawWhenFromAccountProvided_andPropagateIdemKey() throws Exception {
        var td = tdReady("alice", new BigDecimal("100.00"), new BigDecimal("0.05"), 30);
        when(service.create(any(), any(), any(), anyInt())).thenReturn(td);

        var from = UUID.randomUUID();
        mvc.perform(post("/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "REQ-1")
                .content("""
                        {
                          "owner":"alice",
                          "principal":100.0,
                          "annualRate":0.05,
                          "termDays":30,
                          "fromAccountId":"%s"
                        }
                        """.formatted(from)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.owner").value("alice"));

        // 下流呼び出しの引数を検証（Idempotency-Key に :WD が付与）
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(savingsClient).withdraw(eq(from), eq(new BigDecimal("100.0")), key.capture());
        org.assertj.core.api.Assertions.assertThat(key.getValue()).isEqualTo("REQ-1:WD");
    }

    /**
     * 正常系：
     * - fromAccountId が無い場合、SavingsClient#withdraw は呼ばれないこと
     * - レスポンスは 201 で ID が返ること
     */
    @Test
    void create_shouldNotWithdrawWhenFromAccountNull() throws Exception {
        var td = tdReady("alice", new BigDecimal("100.00"), new BigDecimal("0.05"), 30);
        when(service.create(any(), any(), any(), anyInt())).thenReturn(td);

        mvc.perform(post("/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "owner":"alice",
                          "principal":100.0,
                          "annualRate":0.05,
                          "termDays":30
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        // withdraw は呼ばれない
        verifyNoInteractions(savingsClient);
    }

    /**
     * 例外系（補償）：
     * - create() が例外を投げた場合、補償として SavingsClient#deposit が
     *   :CP サフィックス付きキーで呼ばれ、その後に例外が再送出される。
     * - MockMvc は例外を 500 で返さず、テストに例外が伝播するため、
     *   ステータスではなく「例外が起きたこと」を検証する。
     */
    @Test
    void create_shouldCompensateOnFailure() {
        when(service.create(any(), any(), any(), anyInt()))
                .thenThrow(new RuntimeException("boom"));

        var from = UUID.randomUUID();

        assertThatThrownBy(() ->
                mvc.perform(post("/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "REQ-2")
                        .content("""
                                {
                                  "owner":"alice",
                                  "principal":100.0,
                                  "annualRate":0.05,
                                  "termDays":30,
                                  "fromAccountId":"%s"
                                }
                                """.formatted(from))
                ).andReturn()
        ).hasRootCauseInstanceOf(RuntimeException.class);

        // 補償 deposit が :CP キーで呼ばれることを検証
        verify(savingsClient).deposit(eq(from), eq(new BigDecimal("100.0")), eq("REQ-2:CP"));
    }
}
