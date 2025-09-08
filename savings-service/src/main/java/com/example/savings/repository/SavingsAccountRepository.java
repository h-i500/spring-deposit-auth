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

  // ★ 追加: オーナー名の重複排除一覧
  @Query("select distinct a.owner from Account a where a.owner is not null order by a.owner")
  List<String> findDistinctOwners();
}
