package app.mstd.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/savings/accounts")
@Produces(MediaType.APPLICATION_JSON)
public class SavingsAccountsResource {

    @GET
    public List<Object> byOwner(@QueryParam("owner") String owner) {
        if (owner == null || owner.isBlank()) {
            throw new WebApplicationException("query param 'owner' is required", 400);
        }
        // まずは空配列でOK（あとでDTO & Serviceに差し替え）
        return List.of();
    }
}
