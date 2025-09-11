package app.mstd.service;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import app.mstd.client.SavingsServiceClient;

@ApplicationScoped
public class AccountQueryService {

    @Inject @RestClient
    SavingsServiceClient savingsClient;

    public List<Map<String, Object>> listByOwner(String owner) {
        return savingsClient.listByOwner(owner); // 単純委譲
    }
}
