package app.mbio.api;

import java.util.List;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import app.mbio.dto.SavingsAccountDto;

@Path("/api/savings")
@RegisterRestClient(configKey = "mstd")   // ← MSTD に向ける
@Produces(MediaType.APPLICATION_JSON)
@OidcClientFilter
public interface SavingsApi {
    @GET @Path("/accounts")
    List<SavingsAccountDto> byOwner(@QueryParam("owner") String owner);
}
