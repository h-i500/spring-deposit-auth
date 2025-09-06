package com.example.timedeposit.repository;

import com.example.timedeposit.model.TimeDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TimeDepositAccountRepository extends JpaRepository<TimeDeposit, UUID> {

  @Query("""
    select t from TimeDeposit t
    where lower(t.owner) like lower(concat('%', :key, '%'))
  """)
  List<TimeDeposit> searchByOwnerKey(@Param("key") String key);
}
