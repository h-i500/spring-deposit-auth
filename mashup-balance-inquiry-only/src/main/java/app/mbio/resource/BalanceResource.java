// mashup-balance-inquiry-only/src/main/java/app/mbio/resource/BalanceResource.java
package app.mbio.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import app.mbio.api.SavingsApi;
import app.mbio.api.TimeApi;
import app.mbio.dto.BalanceResponse;
import app.mbio.dto.SavingsDto;
import app.mbio.dto.TimeDepositDto;

import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.List;

@Path("/balances")
@Produces(MediaType.APPLICATION_JSON)
public class BalanceResource {

    @Inject @RestClient SavingsApi savings;
    @Inject @RestClient TimeApi time;

    @GET @Path("/{owner}")
    public BalanceResponse byOwner(@PathParam("owner") String owner) {
        List<SavingsDto> s;
        try {
            s = savings.byOwner(owner);
        } catch (WebApplicationException ex) {
            if (ex.getResponse() != null && ex.getResponse().getStatus() == 404) {
                s = Collections.emptyList();
            } else {
                throw ex;
            }
        }

        List<TimeDepositDto> t;
        try {
            t = time.byOwner(owner);
        } catch (WebApplicationException ex) {
            if (ex.getResponse() != null && ex.getResponse().getStatus() == 404) {
                t = Collections.emptyList();
            } else {
                throw ex;
            }
        }

        return BalanceResponse.of(owner, s, t);
    }
}
