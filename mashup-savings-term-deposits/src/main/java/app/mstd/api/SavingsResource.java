
package app.mstd.api;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import app.mstd.client.SavingsServiceClient;
import app.mstd.service.AccountQueryService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import java.util.List;
import java.util.Map;

import app.mstd.client.SavingsServiceClient;

@Path("/api/savings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class SavingsResource {

    @Inject
    @RestClient
    SavingsServiceClient savings;

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



    // ★ === ここから、検索用 ===
    @Inject
    AccountQueryService accountQuery; // ← Service 経由（直で RestClient でもOK）

    @GET
    @Path("/accounts")
    public List<Map<String, Object>> accounts(@QueryParam("owner") String owner) {
        if (owner == null || owner.isBlank()) {
            throw new WebApplicationException("query param 'owner' is required", 400);
        }
        return accountQuery.listByOwner(owner);
    }
}
