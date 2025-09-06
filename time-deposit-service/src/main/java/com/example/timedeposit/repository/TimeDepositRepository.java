// package com.example.timedeposit.repository;

// import com.example.timedeposit.model.TimeDeposit;
// import org.springframework.data.jpa.repository.JpaRepository;

// import java.util.UUID;

// public interface TimeDepositRepository extends JpaRepository<TimeDeposit, UUID> {}

package com.example.timedeposit.repository;


import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.timedeposit.model.TimeDeposit;

public interface TimeDepositRepository extends JpaRepository<TimeDeposit, UUID> {
    List<TimeDeposit> findByOwner(String owner);
}
