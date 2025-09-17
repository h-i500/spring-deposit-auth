// 修正版: src/test/java/com/example/savings/service/AccountServiceTest.java
package com.example.savings.service;

import com.example.savings.model.Account;
import com.example.savings.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private AccountRepository repo;
    private AccountService service;

    @BeforeEach
    void setUp() {
        repo = mock(AccountRepository.class);
        service = new AccountService(repo);
    }

    @Test
    @DisplayName("create: owner を設定し残高0で保存する")
    void create_ok() {
        var saved = new Account();
        // saved の id は null でも可（テストで id を検証していない）
        saved.setOwner("alice");
        saved.setBalance(BigDecimal.ZERO);

        when(repo.save(any(Account.class))).thenReturn(saved);

        var result = service.create("alice");
        assertThat(result.getOwner()).isEqualTo("alice");
        assertThat(result.getBalance()).isEqualByComparingTo("0");
        verify(repo).save(any(Account.class));
    }

    @Test
    @DisplayName("get: 存在しなければ IllegalArgumentException")
    void get_notFound() {
        var id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    @DisplayName("deposit: 正の金額で加算される")
    void deposit_ok() {
        var id = UUID.randomUUID();
        var a = new Account();
        a.setOwner("alice");
        a.setBalance(new BigDecimal("100"));

        when(repo.findById(id)).thenReturn(Optional.of(a));

        var res = service.deposit(id, new BigDecimal("25.50"));
        assertThat(res.getBalance()).isEqualByComparingTo("125.50");
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("deposit: 0以下は IllegalArgumentException")
    void deposit_ng_amount() {
        var id = UUID.randomUUID();
        var a = new Account(); a.setBalance(BigDecimal.TEN);
        when(repo.findById(id)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.deposit(id, new BigDecimal("0")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.deposit(id, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("withdraw: 残高以内なら減算される")
    void withdraw_ok() {
        var id = UUID.randomUUID();
        var a = new Account(); a.setBalance(new BigDecimal("100.00"));
        when(repo.findById(id)).thenReturn(Optional.of(a));

        var res = service.withdraw(id, new BigDecimal("40.00"));
        assertThat(res.getBalance()).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("withdraw: 残高不足は IllegalStateException")
    void withdraw_insufficient() {
        var id = UUID.randomUUID();
        var a = new Account(); a.setBalance(new BigDecimal("30"));
        when(repo.findById(id)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.withdraw(id, new BigDecimal("31")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insufficient funds");
    }

    @Test
    @DisplayName("findByOwner: repository 経由で検索する")
    void findByOwner() {
        when(repo.findByOwner("alice")).thenReturn(List.of(new Account()));
        assertThat(service.findByOwner("alice")).hasSize(1);
        verify(repo).findByOwner("alice");
    }
}
