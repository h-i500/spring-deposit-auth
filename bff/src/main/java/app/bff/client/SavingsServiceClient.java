package app.bff.client;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.token.propagation.AccessToken;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Path("/accounts")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AccessToken // ログイン中ユーザのトークンを下流へ伝播
@RegisterRestClient(configKey = "savings")
public interface SavingsServiceClient {

    // POST /accounts  { "owner": "..." }
    @POST
    Map<String, Object> create(Map<String, Object> req);

    // GET /accounts/{id}
    @GET
    @Path("/{id}")
    Map<String, Object> get(@PathParam("id") UUID id);

    // POST /accounts/{id}/deposit  { "amount": 123.45 }
    @POST
    @Path("/{id}/deposit")
    Map<String, Object> deposit(@PathParam("id") UUID id, Map<String, BigDecimal> req);

    // POST /accounts/{id}/withdraw  { "amount": 10 }
    @POST
    @Path("/{id}/withdraw")
    Map<String, Object> withdraw(@PathParam("id") UUID id, Map<String, BigDecimal> req);
}
