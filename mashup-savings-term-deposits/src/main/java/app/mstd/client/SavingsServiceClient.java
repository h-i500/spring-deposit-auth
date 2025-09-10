
package app.mstd.client;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.token.propagation.AccessToken;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.List;   // ← 追加

@Path("/accounts")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AccessToken // ログイン中ユーザのトークンを下流へ伝播
@RegisterRestClient(configKey = "savings")
public interface SavingsServiceClient {

    // GET /accounts/{id}
    @GET
    @Path("/{id}")
    Map<String, Object> get(@PathParam("id") UUID id);

    // ★ 追加: GET /accounts?owner=...
    // @GET
    // List<Map<String, Object>> findByOwner(@QueryParam("owner") String owner);
    @GET
    List<Map<String, Object>> listByOwner(@QueryParam("owner") String owner);

    // POST /accounts  { "owner": "..." }
    @POST
    Map<String, Object> create(Map<String, Object> req);

    // POST /accounts/{id}/deposit  { "amount": 123.45 }
    @POST
    @Path("/{id}/deposit")
    Map<String, Object> deposit(@PathParam("id") UUID id, Map<String, BigDecimal> req);

    // POST /accounts/{id}/withdraw  { "amount": 10 }
    @POST
    @Path("/{id}/withdraw")
    Map<String, Object> withdraw(@PathParam("id") UUID id, Map<String, BigDecimal> req);
}
