package app.bff.debug;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * フロントからの /api/debug/* を受けて、各サービスの /debug/* にプロキシ。
 */
@Path("/api/debug")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class DebugResource {

    // docker-compose のサービス名 & ポートに合わせる
    static final String SAVINGS_BASE = "http://savings-service:8081";
    static final String TIME_DEPOSIT_BASE = "http://time-deposit-service:8082";

    @Inject
    WebClient webClient;

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/savings")
    public Uni<Response> savings(@QueryParam("ownerKey") String ownerKey) {
        String url = SAVINGS_BASE + "/debug/savings?ownerKey=" + enc(ownerKey);
        return webClient
                .getAbs(url)
                .putHeader("Authorization", bearer())
                .putHeader("Accept", MediaType.APPLICATION_JSON)
                .send()
                .onItem().transform(resp -> Response.status(resp.statusCode())
                        .entity(resp.bodyAsString())
                        .type(MediaType.APPLICATION_JSON)
                        .build());
    }

    @GET
    @Path("/time-deposits")
    public Uni<Response> timeDeposits(@QueryParam("ownerKey") String ownerKey) {
        String url = TIME_DEPOSIT_BASE + "/debug/time-deposits?ownerKey=" + enc(ownerKey);
        return webClient
                .getAbs(url)
                .putHeader("Authorization", bearer())
                .putHeader("Accept", MediaType.APPLICATION_JSON)
                .send()
                .onItem().transform(resp -> Response.status(resp.statusCode())
                        .entity(resp.bodyAsString())
                        .type(MediaType.APPLICATION_JSON)
                        .build());
    }

    private String bearer() {
        AccessTokenCredential cred = identity.getCredential(AccessTokenCredential.class);
        if (cred == null || cred.getToken() == null || cred.getToken().isBlank()) {
            // セッションはあるがアクセストークンが無い(失効など)場合は401で返す
            throw new WebApplicationException("No access token", Response.Status.UNAUTHORIZED);
        }
        return "Bearer " + cred.getToken();
    }

    private static String enc(String s) {
        return s == null ? "" : URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
