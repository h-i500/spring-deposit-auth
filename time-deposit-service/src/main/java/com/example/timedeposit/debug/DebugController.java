package com.example.timedeposit.debug;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.example.timedeposit.model.TimeDeposit;
import com.example.timedeposit.repository.TimeDepositRepository;
import com.example.timedeposit.repository.TimeDepositAccountRepository;

@RestController
@RequestMapping("/debug")
public class DebugController {

    private final TimeDepositRepository repo;
    private final TimeDepositAccountRepository accRepo;

    // ★ 手書きコンストラクタ（@Autowired は単一コンストラクタなら省略可）
    // public DebugController(TimeDepositRepository repo) {
    //     this.repo = repo;
    // }
    public DebugController(TimeDepositRepository repo, TimeDepositAccountRepository accRepo) {
        this.repo = repo;
        this.accRepo = accRepo;
    }

    @GetMapping("/time-deposits")
    public List<TimeDeposit> findByOwner(
            @RequestParam(name = "owner", required = false) String owner,
            @RequestParam(name = "ownerKey", required = false) String ownerKey) {
        String key = (owner != null && !owner.isBlank()) ? owner : ownerKey;
        return (key == null || key.isBlank()) ? List.of() : repo.findByOwner(key);
    }

    @GetMapping("/owners")
    public List<String> listOwners() {
        return accRepo.findDistinctOwners();
    }
}
