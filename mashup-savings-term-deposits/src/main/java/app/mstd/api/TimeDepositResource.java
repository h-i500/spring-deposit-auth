
package app.mstd.api;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import app.mstd.client.TimeDepositServiceClient;

import java.util.Map;
import java.util.UUID;

import java.util.List;
import java.util.Map;

@Path("/api/deposits")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class TimeDepositResource {

    @Inject
    @RestClient
    TimeDepositServiceClient td;

    // ★ [検索用] GET /api/deposits/accounts?owner=xxx
    // @GET
    // @Path("/accounts")
    // public Response searchByOwner(@QueryParam("owner") String owner) {
    //     return Response.ok(java.util.Collections.emptyList()).build();
    // }
    
    // クラス @Path("/api/deposits") の配下に増設
    @GET
    @Path("/accounts")
    public Response list(@QueryParam("owner") String owner) {
        try {
            return Response.ok(td.listByOwner(owner)).build();
        } catch (ClientWebApplicationException e) {
            return forward(e);
        }
    }

    // 定期預金の取得: GET /api/deposits/{id}
    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") UUID id) {
        try {
            return Response.ok(td.get(id)).build();
        } catch (ClientWebApplicationException e) {
            return forward(e);
        }
    }

    // 定期預金の作成: POST /api/deposits
    @POST
    public Response create(Map<String, Object> req) {
        try {
            return Response.ok(td.create(req)).build();
        } catch (ClientWebApplicationException e) {
            return forward(e);
        }
    }

    // ★ 定期預金の解約: POST /api/deposits/{id}/close?toAccountId=...&at=...
    @POST
    @Path("/{id}/close")
    public Response close(@PathParam("id") UUID id,
                          @QueryParam("toAccountId") UUID toAccountId,
                          @QueryParam("at") String at) {
        if (toAccountId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("toAccountId is required").build();
        }
        try {
            return Response.ok(td.close(id, toAccountId, at)).build();
        } catch (ClientWebApplicationException e) {
            return forward(e);
        }
    }

    private Response forward(ClientWebApplicationException e) {
        final var resp = e.getResponse();
        Object entity;
        try {
            entity = resp.hasEntity() ? resp.readEntity(String.class) : "";
        } catch (Exception ex) {
            entity = "";
        }
        return Response.status(resp.getStatus()).entity(entity).build();
    }
}
