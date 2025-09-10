
package app.mstd.api;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import app.mstd.client.SavingsServiceClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import java.util.List;
import java.util.Map;

@Path("/api/savings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class SavingsResource {

    @Inject
    @RestClient
    SavingsServiceClient savings;

    // ★ [検索用] GET /api/savings/accounts?owner=xxx
    // まずは空配列 [] を返すだけ（Aggregator は空ならそのまま合算して返せます）
    // @GET
    // @Path("/accounts")
    // public Response searchAccounts(@QueryParam("owner") String owner) {
    //     return Response.ok(java.util.Collections.emptyList()).build();
    // }
    // @GET
    // @Path("/accounts")
    // public Response listAccounts(@QueryParam("owner") String owner) {
    //     try {
    //         List<Map<String, Object>> list = savings.findByOwner(owner);
    //         return Response.ok(list).build();
    //     } catch (ClientWebApplicationException e) {
    //         return forward(e);
    //     }
    // }
    @GET
    @Path("/accounts")
    public Response list(@QueryParam("owner") String owner) {
        try {
            return Response.ok(savings.listByOwner(owner)).build();
        } catch (ClientWebApplicationException e) {
            return forward(e);
        }
    }

    @GET
    @Path("/accounts/{id}")
    public Response getAccount(@PathParam("id") UUID id) {
        try {
            return Response.ok(savings.get(id)).build();
        } catch (ClientWebApplicationException e) {
            return forward(e);
        }
    }

    @POST
    @Path("/accounts")
    public Response createAccount(Map<String, Object> req) {
        try {
            return Response.ok(savings.create(req)).build();
        } catch (ClientWebApplicationException e) {
            return forward(e);
        }
    }

    @POST
    @Path("/accounts/{id}/deposit")
    public Response deposit(@PathParam("id") UUID id, Map<String, BigDecimal> req) {
        try {
            return Response.ok(savings.deposit(id, req)).build();
        } catch (ClientWebApplicationException e) {
            return forward(e);
        }
    }

    @POST
    @Path("/accounts/{id}/withdraw")
    public Response withdraw(@PathParam("id") UUID id, Map<String, BigDecimal> req) {
        try {
            return Response.ok(savings.withdraw(id, req)).build();
        } catch (ClientWebApplicationException e) {
            return forward(e);
        }
    }

    private Response forward(ClientWebApplicationException e) {
        // 下流の HTTP ステータス/本文をそのまま転送
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
