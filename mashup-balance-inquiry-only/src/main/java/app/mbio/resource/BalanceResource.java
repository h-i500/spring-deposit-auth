package app.mbio.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import app.mbio.api.SavingsApi;
import app.mbio.api.TimeApi;
import app.mbio.dto.BalanceResponse;
import app.mbio.dto.SavingsAccountDto;
import app.mbio.dto.TimeDepositDto;

import java.util.List;
import java.util.Collections;

// ★ 追加：これを import
import org.jboss.resteasy.reactive.ClientWebApplicationException;

@Path("/balances")
@Produces(MediaType.APPLICATION_JSON)
public class BalanceResource {

    @Inject @RestClient SavingsApi savings;
    @Inject @RestClient TimeApi time;

    @GET @Path("/{owner}")
    public BalanceResponse byOwner(@PathParam("owner") String owner) {
        List<SavingsAccountDto> s;
        try {
            s = savings.byOwner(owner);
        } catch (ClientWebApplicationException ex) {
            int st = ex.getResponse() != null ? ex.getResponse().getStatus() : 0;
            if (st == 404 || st == 405) s = Collections.emptyList(); else throw ex;
        }

        List<TimeDepositDto> t;
        try {
            t = time.byOwner(owner);
        } catch (ClientWebApplicationException ex) {
            int st = ex.getResponse() != null ? ex.getResponse().getStatus() : 0;
            if (st == 404 || st == 405) t = Collections.emptyList(); else throw ex;
        }

        return BalanceResponse.of(owner, s, t);
    }
}
