// src/test/java/com/example/savings/controller/AccountControllerTest.java
package com.example.savings.controller;

import com.example.savings.model.Account;
import com.example.savings.security.SecurityConfig;
import com.example.savings.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder; // Resource Server の起動に必要（テストではモック）
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;  // ← 本プロジェクトの Account.createdAt は Instant
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web 層スライステスト。
 *
 * 目的:
 * - Controller のリクエスト/レスポンスとメソッドレベル認可(@PreAuthorize)の動作を検証する。
 * - 例外ハンドリング(@ExceptionHandler)での HTTP ステータス/ボディを検証する。
 *
 * 特徴:
 * - @WebMvcTest で Web 層のみを起動。Service/Repository はロードしない。
 * - AccountService は @MockBean でスタブ化（DB に依存しない）。
 * - SecurityConfig を @Import して hasRole(...) を実際に通す。
 * - 認可は jwt().authorities(ROLE_xxx) でロールを直接付与（クレーム→権限変換はここでは検証しない）。
 * - Controller が Map.of(...) でレスポンスを構築するため、null を避ける目的で
 *   id / createdAt をリフレクションで埋めるユーティリティを用意。
 */
@WebMvcTest(controllers = AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    AccountService service;

    // Resource Server 用の Bean。テストでは実処理不要なのでダミー化。
    @MockBean
    JwtDecoder jwtDecoder;

    /** read ロールを直接付与（hasRole('read') を満たす） */
    private static RequestPostProcessor jwtRead() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_read"));
    }

    /** user ロールを直接付与（hasRole('user') を満たす） */
    private static RequestPostProcessor jwtUser() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_user"));
    }

    /** setter が無い private フィールドに値を入れるテスト用ユーティリティ */
    private static void setField(Object target, String name, Object value) {
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * シナリオ: ROLE_user で口座作成 API を叩くと 200 になり、owner/balance が返る。
     * ポイント:
     * - Controller は Map.of(...) を使うため、id/createdAt を null にできない。
     *   → リフレクションで事前に埋めておく。
     */
    @Test
    void create_requires_user_role_and_returns_account() throws Exception {
        var a = new Account();
        setField(a, "id", UUID.randomUUID());
        setField(a, "createdAt", Instant.now()); // Account の createdAt は Instant
        a.setOwner("alice");
        a.setBalance(BigDecimal.ZERO);

        when(service.create("alice")).thenReturn(a);

        mvc.perform(post("/accounts")
                .with(jwtUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"owner\":\"alice\"}"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.owner").value("alice"))
           .andExpect(jsonPath("$.balance").value(0));
    }

    /**
     * シナリオ: ROLE_read で残高取得 API を叩くと 200 と口座情報が返る。
     * ポイント: id/createdAt を埋めて Map.of の NPE を回避。
     */
    @Test
    void get_requires_read_role() throws Exception {
        var id = UUID.randomUUID();
        var a = new Account();
        setField(a, "id", id);
        setField(a, "createdAt", Instant.now());
        a.setOwner("bob");
        a.setBalance(new BigDecimal("123.45"));

        when(service.get(id)).thenReturn(a);

        mvc.perform(get("/accounts/{id}", id).with(jwtRead()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.owner").value("bob"))
           .andExpect(jsonPath("$.balance").value(123.45));
    }

    /**
     * シナリオ: ROLE_user で入金 API を叩くと 200 と更新後残高が返る。
     * ポイント: レスポンスに id が含まれるため、id をリフレクションで必ず埋める。
     */
    @Test
    void deposit_requires_user_role_and_updates_balance() throws Exception {
        var id = UUID.randomUUID();
        var a = new Account();
        setField(a, "id", id); // Map.of("id", ...) で必須
        a.setBalance(new BigDecimal("150.00"));

        when(service.deposit(id, new BigDecimal("50.00"))).thenReturn(a);

        mvc.perform(post("/accounts/{id}/deposit", id)
                .with(jwtUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 50.00}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.balance").value(150.00));
    }

    /**
     * シナリオ: 出金 API でサービスが IllegalStateException を投げたら
     * Controller の @ExceptionHandler により 400 と {"error": "..."} が返る。
     */
    @Test
    void withdraw_returns_400_when_service_throws_illegalstate_or_illegalargument() throws Exception {
        var id = UUID.randomUUID();
        when(service.withdraw(eq(id), any())).thenThrow(new IllegalStateException("insufficient funds"));

        mvc.perform(post("/accounts/{id}/withdraw", id)
                .with(jwtUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 99999}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error").value("insufficient funds"));
    }

    /**
     * シナリオ: ROLE_read で一覧 API を叩くと 200、
     *          owner パラメータが空白だと 400（ResponseStatusException）。
     */
    @Test
    void listByOwner_requires_read_role_and_checks_param() throws Exception {
        var a = new Account();
        setField(a, "id", UUID.randomUUID());
        setField(a, "createdAt", Instant.now());
        a.setOwner("alice");
        a.setBalance(new BigDecimal("10.00"));

        when(service.findByOwner("alice")).thenReturn(List.of(a));

        // 正常
        mvc.perform(get("/accounts").param("owner", "alice").with(jwtRead()))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        // パラメータ空白 → 400
        mvc.perform(get("/accounts").param("owner", " ").with(jwtRead()))
           .andExpect(status().isBadRequest());
    }

    /**
     * シナリオ: 未認証でアクセスすると 401（BearerTokenAuthenticationEntryPoint）。
     */
    @Test
    void unauthorized_without_jwt() throws Exception {
        mvc.perform(get("/accounts/{id}", UUID.randomUUID()))
           .andExpect(status().isUnauthorized());
    }
}
