package com.example.timedeposit.repository;

import com.example.timedeposit.model.TimeDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TimeDepositRepository extends JpaRepository<TimeDeposit, UUID> {}