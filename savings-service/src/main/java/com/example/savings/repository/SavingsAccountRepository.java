package com.example.savings.repository;

import com.example.savings.model.Account; // ← 実体
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface SavingsAccountRepository extends JpaRepository<Account, UUID> {

  @Query("""
    select a from Account a
    where lower(a.owner) like lower(concat('%', :key, '%'))
  """)
  List<Account> searchByOwnerKey(@Param("key") String key);
}
