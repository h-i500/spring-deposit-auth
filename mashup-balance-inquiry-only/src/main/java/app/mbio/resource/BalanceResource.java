package app.mbio.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import app.mbio.api.SavingsApi;
import app.mbio.api.TimeApi;
import app.mbio.dto.BalanceResponse;  // ← dto
import app.mbio.dto.SavingsDto;      // ← dto（型参照するなら）
import app.mbio.dto.TimeDepositDto;  // ← dto（型参照するなら）

// @Path("/v1/balances")
@Path("/balances")
@Produces(MediaType.APPLICATION_JSON)
public class BalanceResource {

    @Inject @RestClient SavingsApi savings;
    @Inject @RestClient TimeApi time;

    @GET @Path("/{owner}")
    public BalanceResponse byOwner(@PathParam("owner") String owner) {
        var s = savings.byOwner(owner);
        var t = time.byOwner(owner);
        return BalanceResponse.of(owner, s, t);
    }
}
