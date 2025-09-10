// mashup-balance-inquiry-only/src/main/java/app/mbio/api/TimeApi.java
package app.mbio.api;

import java.util.List;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import app.mbio.dto.TimeDepositDto;

// BFF 側の都合に合わせてどちらか：
//  A) BFF が /api/time-deposits → ここは "/time-deposits"
//  B) BFF が /api/time-deposits/accounts → ここは "/time-deposits/accounts"
@Path("/time-deposits")
@RegisterRestClient(configKey = "time-api")   // ← 重要
@Produces(MediaType.APPLICATION_JSON)
@OidcClientFilter
public interface TimeApi {
    @GET
    List<TimeDepositDto> byOwner(@QueryParam("owner") String owner);
}
