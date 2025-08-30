package com.example.bff.api;

import com.example.bff.client.TimeDepositServiceClient;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import java.util.Map;

@Path("/api/time-deposits")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class TimeDepositsResource {

    @Inject
    @RestClient
    TimeDepositServiceClient td;

    // 旧: GET /api/time-deposits/products -> 実サービスに無いので削除
    // 新: POST /api/time-deposits/transfers -> /transfers に委譲

    @POST
    @Path("/transfers")
    public Response transfer(Map<String, Object> req) {
        try {
            return Response.ok(td.transfer(req)).build();
        } catch (ClientWebApplicationException e) {
            return forward(e);
        }
    }

    @GET
    @Path("/health")
    public Response health() {
        try {
            return Response.ok(td.health()).build();
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
