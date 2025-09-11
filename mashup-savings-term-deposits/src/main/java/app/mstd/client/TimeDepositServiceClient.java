package app.mstd.client;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.token.propagation.AccessToken;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.UUID;
import java.util.List;

@Path("/deposits")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AccessToken
@RegisterRestClient(configKey = "timedeposit")
public interface TimeDepositServiceClient {

    @GET @Path("/{id}")
    Map<String, Object> get(@PathParam("id") UUID id);

    @POST
    Map<String, Object> create(Map<String, Object> req);

    @POST @Path("/{id}/close")
    Map<String, Object> close(@PathParam("id") UUID id,
                              @QueryParam("toAccountId") UUID toAccountId,
                              @QueryParam("at") String at);

                              
    // // ★ 検索は POST /deposits
    // @POST
    // List<Map<String, Object>> findByOwner(Map<String, Object> req);
    // // ★ 外向け GET から POST へ変換
    // default List<Map<String, Object>> listByOwner(String owner) {
    //     // 必要に応じて "ownerKey" に変更
    //     return findByOwner(Map.of("owner", owner));
    // }
    @GET
    // @Path("/accounts")
    List<Map<String, Object>> listByOwner(@QueryParam("owner") String owner);
}
