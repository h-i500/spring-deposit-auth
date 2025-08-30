package com.example.bff.api;

import com.example.bff.client.SavingsServiceClient;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Path("/api/savings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class SavingsResource {

    @Inject
    @RestClient
    SavingsServiceClient savings;

    // 変更点: "一覧" は存在しないため、BFF では create と get を明示 API に。
    // 既存 UI が GET /api/savings/accounts を叩いていたなら、UI も合わせてください。

    @POST
    @Path("/accounts")
    public Response createAccount(Map<String, Object> req) {
        try {
            return Response.ok(savings.create(req)).build();
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
        // 下流の HTTP ステータス/本文をそのまま転送（BFF で 500 にしない）
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
