// mashup-balance-inquiry-only/src/main/java/app/mbio/api/SavingsApi.java
package app.mbio.api;

import java.util.List;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import app.mbio.dto.SavingsDto;

// mashup-balance-inquiry-only/src/main/java/app/mbio/api/SavingsApi.java
@Path("/savings/accounts")
@RegisterRestClient(configKey = "savings-api")
@Produces(MediaType.APPLICATION_JSON)
@OidcClientFilter
public interface SavingsApi {
    @GET
    List<SavingsDto> byOwner(@QueryParam("owner") String owner);
}

