package com.example.savings.repository;

import com.example.savings.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository;

import java.util.List;   // ← 追加
import java.util.UUID;  // ← UUID を使っているなら追加

// public interface AccountRepository extends JpaRepository<Account, UUID> {}
public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByOwner(String owner);
}