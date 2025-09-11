package com.example.timedeposit.controller;

import com.example.timedeposit.model.TimeDeposit;
import com.example.timedeposit.repository.TimeDepositRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/deposits")
public class TimeDepositController {

    private final TimeDepositRepository repo;

    public TimeDepositController(TimeDepositRepository repo) {
        this.repo = repo;
    }
    
    @GetMapping
    public List<TimeDeposit> listByOwner(@RequestParam("owner") String owner) {
        return repo.findByOwner(owner);
    }
}
