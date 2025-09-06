package com.example.timedeposit.controller;

import com.example.timedeposit.dto.TimeDepositDto;
import com.example.timedeposit.repository.TimeDepositAccountRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/time-deposits")
public class TimeDepositQueryController {

  private final TimeDepositAccountRepository repo;

  public TimeDepositQueryController(TimeDepositAccountRepository repo) {
    this.repo = repo;
  }

  @GetMapping("/search")
  public List<TimeDepositDto> search(@RequestParam("ownerKey") String ownerKey) {
    return repo.searchByOwnerKey(ownerKey)
               .stream()
               .map(TimeDepositDto::fromEntity) // from → fromEntity に統一
               .toList();
  }
}
