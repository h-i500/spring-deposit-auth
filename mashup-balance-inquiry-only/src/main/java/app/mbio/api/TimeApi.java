package app.mbio.api;

import java.util.List;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import app.mbio.dto.SavingsAccountDto;
import app.mbio.dto.TimeDepositDto;

// mashup-balance-inquiry-only/src/main/java/app/mbio/api/TimeApi.java
@Path("/deposits/accounts")
@RegisterRestClient(configKey = "time-api")
@Produces(MediaType.APPLICATION_JSON)
@OidcClientFilter
public interface TimeApi {
    // @GET
    // // List<TimeDepositDto> byOwner(@QueryParam("owner") String owner);
    // List<TimeDepositDto> byOwner(@QueryParam("ownerKey") String ownerKey);
    
    // @POST
    @GET
    // @Path("/deposits")
    List<TimeDepositDto> byOwner(String owner);
}
