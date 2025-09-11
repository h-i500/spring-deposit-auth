// TimeApi.java
package app.mbio.api;

import java.util.List;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
// ↓ これに変更（SavingsApi と同じパッケージに合わせる）
import io.quarkus.oidc.token.propagation.common.AccessToken;
// import io.quarkus.oidc.client.filter.OidcClientFilter;  // ← 削除
import app.mbio.dto.TimeDepositDto;

@Path("/api/deposits")
@RegisterRestClient(configKey = "mstd")
@Produces(MediaType.APPLICATION_JSON)
@AccessToken   // ← ここを付与
public interface TimeApi {
    @GET @Path("/accounts")
    List<TimeDepositDto> byOwner(@QueryParam("owner") String owner);
}
