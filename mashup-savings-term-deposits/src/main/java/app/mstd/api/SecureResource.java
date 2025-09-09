
package app.mstd.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import java.net.URI;
// import java.net.URLEncoder;
// import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("/secure")
public class SecureResource {

    // 未ログインで来たら OIDC が 302 で Keycloak へ。
    // 認証完了後にこのメソッドが実行され、/app/ にリダイレクト。

    @GET
    @Path("/login")
    @Produces(MediaType.TEXT_HTML) // 406 回避
    public Response login(@Context UriInfo uriInfo) {
        // 保護された /secure/me へ 303。そこで OIDC が 302→Keycloak へリダイレクトする。
        URI target = uriInfo.getBaseUriBuilder().path("secure").path("me").build();
        return Response.seeOther(target).build();
    }

    // 「ログイン状態の判定＋現在のユーザー情報取得」のための標準フック
    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    public Response me(@Context SecurityIdentity identity) {
        String principal = identity != null ? identity.getPrincipal().getName() : null;
        String preferred = getClaim(identity, "preferred_username"); // あなたの実装に合わせて
        String email     = getClaim(identity, "email");

        Map<String, Object> out = new LinkedHashMap<>();
        if (principal != null) out.put("principal", principal);
        if (preferred != null) out.put("preferred_username", preferred);
        if (email != null)     out.put("email", email);
        return Response.ok(out).build();
    }

    private String getClaim(SecurityIdentity id, String name) {
        if (id == null) return null;
        Object v = id.getAttribute(name); // 取得方法は実装に合わせて（例）
        return v != null ? v.toString() : null;
    }

    @GET
    @Path("/callback")
    @Produces(MediaType.TEXT_HTML) // 406 回避
    public Response callback() {
        // Quarkus OIDC が通常は先にフックして token 交換を完了させる。
        // 何らかの理由でここまで落ちてきたら /app/ へ戻す。
        return Response.seeOther(URI.create("/app/")).build();
}

}
