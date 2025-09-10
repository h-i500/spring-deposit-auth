package app.mbio.api;

import java.util.List;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import app.mbio.dto.TimeDepositDto;

// @Path("/deposits/accounts")              // ← ここを /time-deposits/accounts から訂正
// @RegisterRestClient(configKey = "time-api")
// @Produces(MediaType.APPLICATION_JSON)
// @OidcClientFilter
// public interface TimeApi {
//     @GET
//     List<TimeDepositDto> byOwner(@QueryParam("owner") String owner);
// }


@Path("/deposits/accounts")
@RegisterRestClient(configKey = "time-api")  // ← ここが time-api であること
@Produces(MediaType.APPLICATION_JSON)
@OidcClientFilter
public interface TimeApi {
    @GET
    List<TimeDepositDto> byOwner(@QueryParam("owner") String owner);
}