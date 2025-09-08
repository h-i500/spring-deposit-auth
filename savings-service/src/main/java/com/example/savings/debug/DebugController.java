package com.example.savings.debug;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


import com.example.savings.model.Account;
import com.example.savings.repository.SavingsAccountRepository; 

/**
 * デバッグ用検索API: /debug/savings?ownerKey=xxx
 * BFF から内部通信で呼ばれます（Keycloak 保護なしの想定）。
 */
@RestController
@RequestMapping("/debug")
public class DebugController {

    @PersistenceContext
    EntityManager em;
    private final SavingsAccountRepository repo;

    public DebugController(SavingsAccountRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/savings")
    public List<Account> findByOwner(@RequestParam("ownerKey") String ownerKey) {
        // エンティティ名=Account、プロパティ名=owner に合わせる
        return em.createQuery(
                    "select a from Account a where a.owner = :ownerKey",
                    Account.class)
                 .setParameter("ownerKey", ownerKey)
                 .getResultList();
    }

    @GetMapping("/owners")
    public List<String> listOwners() {
        return repo.findDistinctOwners();
    }
}
