package app.mstd.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/time-deposits")
@Produces(MediaType.APPLICATION_JSON)
public class TimeDepositsResource {

    @GET
    public List<Object> byOwner(@QueryParam("owner") String owner) {
        if (owner == null || owner.isBlank()) {
            throw new WebApplicationException("query param 'owner' is required", 400);
        }
        return List.of();
    }
}
