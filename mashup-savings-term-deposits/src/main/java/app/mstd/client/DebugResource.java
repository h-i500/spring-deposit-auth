
package app.mstd.client;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.vertx.core.json.JsonArray;

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

    // ★ 追加: owner一覧を集約して返す
    @GET
    @Path("/owners")
    public Uni<Response> owners() {
        Uni<JsonArray> s = webClient.getAbs(SAVINGS_BASE + "/debug/owners")
                .putHeader("Authorization", bearer())
                .putHeader("Accept", MediaType.APPLICATION_JSON)
                .send()
                .onItem().transform(resp -> new JsonArray(resp.bodyAsString()))
                .onFailure().recoverWithItem(new JsonArray()); // 片方死んでても空配列に

        Uni<JsonArray> t = webClient.getAbs(TIME_DEPOSIT_BASE + "/debug/owners")
                .putHeader("Authorization", bearer())
                .putHeader("Accept", MediaType.APPLICATION_JSON)
                .send()
                .onItem().transform(resp -> new JsonArray(resp.bodyAsString()))
                .onFailure().recoverWithItem(new JsonArray());

        // 並列結合 → Setで重複排除 → ソート → JSON配列へ
        return Uni.combine().all().unis(s, t).asTuple()
                .emitOn(Infrastructure.getDefaultExecutor()) // マージ処理をワーカで
                .onItem().transform(tuple -> {
                    Set<String> set = new LinkedHashSet<>();
                    Stream.of(tuple.getItem1(), tuple.getItem2())
                            .forEach(arr -> arr.stream()
                                    .filter(Objects::nonNull)
                                    .map(Object::toString)
                                    .map(String::trim)
                                    .filter(x -> !x.isBlank())
                                    .forEach(set::add));
                    List<String> list = new ArrayList<>(set);
                    Collections.sort(list, String::compareToIgnoreCase);
                    return new JsonArray(list).encode();
                })
                .onItem().transform(json -> Response.ok(json).type(MediaType.APPLICATION_JSON).build());
    }

    // 共通プロキシ（既存メソッドの体裁に合わせて共通化）
    private Uni<Response> proxyGet(String url) {
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
