package app.mbio.api;

// import java.util.List;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
// import io.quarkus.oidc.client.filter.OidcClientFilter;
import app.mbio.dto.SavingsAccountDto;
// import io.quarkus.oidc.token.propagation.AccessToken;
import io.quarkus.oidc.token.propagation.common.AccessToken;


@AccessToken
@RegisterRestClient(configKey = "savings-api") // ← ここを savings-api に
@Path("/api/savings")
@Produces(MediaType.APPLICATION_JSON)
public interface SavingsApi {
    @GET
    @Path("/accounts")
    List<SavingsAccountDto> byOwner(@QueryParam("owner") String owner);
}
