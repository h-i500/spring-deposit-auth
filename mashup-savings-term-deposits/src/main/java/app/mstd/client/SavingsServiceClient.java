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

    // ★ 下流の検索は POST /accounts に JSON ボディで条件を渡す
    @POST
    List<Map<String, Object>> findByOwner(Map<String, Object> req);

    // ★ 外向け GET をこのデフォルトメソッドで POST に変換
    default List<Map<String, Object>> listByOwner(String owner) {
        // 下流が "ownerKey" を期待するならキー名を ownerKey に変更してください
        return findByOwner(Map.of("owner", owner));
    }

    // POST /accounts …（口座作成）
    @POST
    @Path("") // 明示（省略可）
    Map<String, Object> create(Map<String, Object> req);

    @POST @Path("/{id}/deposit")
    Map<String, Object> deposit(@PathParam("id") UUID id, Map<String, BigDecimal> req);

    @POST @Path("/{id}/withdraw")
    Map<String, Object> withdraw(@PathParam("id") UUID id, Map<String, BigDecimal> req);
}
