package app.mstd.client;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.token.propagation.AccessToken;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.List;

@Path("/accounts")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AccessToken
@RegisterRestClient(configKey = "savings")
public interface SavingsServiceClient {

    // GET /accounts/{id}
    @GET
    @Path("/{id}")
    Map<String, Object> get(@PathParam("id") UUID id);

    // POST /accounts …（口座作成）
    @POST
    // @Path("") // 明示（省略可） ←これを入れると"/"がついてしまいエラーになる
    Map<String, Object> create(Map<String, Object> req);

    @POST @Path("/{id}/deposit")
    Map<String, Object> deposit(@PathParam("id") UUID id, Map<String, BigDecimal> req);

    @POST @Path("/{id}/withdraw")
    Map<String, Object> withdraw(@PathParam("id") UUID id, Map<String, BigDecimal> req);


    // ownerで検索
    @GET
    List<Map<String, Object>> listByOwner(@QueryParam("owner") String owner);

    
}
