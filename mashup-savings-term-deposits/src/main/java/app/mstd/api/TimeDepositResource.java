
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


    // ★ === ここから、検索用 ===
    private static final String TIME_DEPOSIT_BASE = "http://time-deposit-service:8082";

    @Inject WebClient webClient;
    @Inject SecurityIdentity identity;

    public static record OwnerReq(String owner) {}
    public static record OwnerKeyReq(String ownerKey) {}

    @GET
    @Path("/accounts")
    public Uni<Response> accounts(OwnerKeyReq req) {
        String owner = req == null ? "" : req.ownerKey(); // 下流が owner を期待
        JsonObject body = new JsonObject().put("owner", owner);

        return webClient.postAbs(TIME_DEPOSIT_BASE + "/accounts")
            .putHeader("Authorization", bearer())
            .putHeader("Accept", MediaType.APPLICATION_JSON)
            .putHeader("Content-Type", MediaType.APPLICATION_JSON)
            .sendJsonObject(body)
            .onItem().transform(resp -> Response.status(resp.statusCode())
                .entity(resp.bodyAsString())
                .type(MediaType.APPLICATION_JSON)
                .build());
    }

    private String bearer() {
        AccessTokenCredential cred = identity.getCredential(AccessTokenCredential.class);
        if (cred == null || cred.getToken() == null || cred.getToken().isBlank()) {
            throw new WebApplicationException("No access token", Response.Status.UNAUTHORIZED);
        }
        return "Bearer " + cred.getToken();
    }



}
