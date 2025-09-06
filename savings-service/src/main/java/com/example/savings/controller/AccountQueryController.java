package com.example.savings.controller;

import com.example.savings.dto.SavingsAccountDto;
import com.example.savings.repository.SavingsAccountRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountQueryController {

  private final SavingsAccountRepository repo;

  // Lombokなしの明示コンストラクタ（単一なら Spring が自動DI）
  public AccountQueryController(SavingsAccountRepository repo) {
    this.repo = repo;
  }

  @GetMapping("/search")
  public List<SavingsAccountDto> search(@RequestParam("ownerKey") String ownerKey) {
    return repo.searchByOwnerKey(ownerKey)
               .stream()
               .map(SavingsAccountDto::fromEntity) // ← from → fromEntity に
               .toList();
  }
}
