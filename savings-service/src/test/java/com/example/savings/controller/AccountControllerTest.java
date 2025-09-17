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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    AccountService service;

    // Resource Server 用のダミー Bean（起動安定化）
    @MockBean
    JwtDecoder jwtDecoder;

    // read ロール付与（SecurityConfig の hasRole('read') に対応）
    private static RequestPostProcessor jwtRead() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_read"));
    }

    // user ロール付与（SecurityConfig の hasRole('user') に対応）
    private static RequestPostProcessor jwtUser() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_user"));
    }

    // private フィールド（id, createdAt）に値を差し込むユーティリティ
    private static void setField(Object target, String name, Object value) {
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void create_requires_user_role_and_returns_account() throws Exception {
        var a = new Account();
        // Map.of(...) が null を受け付けないため必須フィールドを埋める
        setField(a, "id", UUID.randomUUID());
        setField(a, "createdAt", Instant.now());
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

    @Test
    void deposit_requires_user_role_and_updates_balance() throws Exception {
        var id = UUID.randomUUID();
        var a = new Account();
        setField(a, "id", id); // Map.of("id", ...) 用に必須
        a.setBalance(new BigDecimal("150.00"));

        when(service.deposit(id, new BigDecimal("50.00"))).thenReturn(a);

        mvc.perform(post("/accounts/{id}/deposit", id)
                .with(jwtUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 50.00}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.balance").value(150.00));
    }

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

    @Test
    void listByOwner_requires_read_role_and_checks_param() throws Exception {
        var a = new Account();
        setField(a, "id", UUID.randomUUID());
        setField(a, "createdAt", Instant.now());
        a.setOwner("alice");
        a.setBalance(new BigDecimal("10.00"));

        when(service.findByOwner("alice")).thenReturn(List.of(a));

        // 正常ケース
        mvc.perform(get("/accounts").param("owner", "alice").with(jwtRead()))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        // owner 未指定/空白 → 400
        mvc.perform(get("/accounts").param("owner", " ").with(jwtRead()))
           .andExpect(status().isBadRequest());
    }

    @Test
    void unauthorized_without_jwt() throws Exception {
        mvc.perform(get("/accounts/{id}", UUID.randomUUID()))
           .andExpect(status().isUnauthorized());
    }
}
