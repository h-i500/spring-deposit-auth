package com.example.bff.client;

import io.quarkus.oidc.token.propagation.AccessToken;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("/transfers")
@RegisterRestClient(configKey = "td")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AccessToken
public interface TimeDepositServiceClient {

    // POST /transfers  （リクエスト形は Spring 側の TransferRequest に合わせる）
    @POST
    Map<String, Object> transfer(Map<String, Object> req);

    // GET /transfers/health
    @GET
    @Path("/health")
    Map<String, String> health();
}
