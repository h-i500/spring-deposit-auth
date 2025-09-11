package app.mbio.api;

import java.util.List;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import app.mbio.dto.TimeDepositDto;

@Path("/api/deposits")
@RegisterRestClient(configKey = "mstd")   // ← SavingsApi と同じ MSTD へ
@Produces(MediaType.APPLICATION_JSON)
@OidcClientFilter
public interface TimeApi {
    @GET @Path("/accounts")
    List<TimeDepositDto> byOwner(@QueryParam("owner") String owner);
}
