package com.example.timedeposit.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
// import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SavingsClient のクライアント層テスト。
 *
 * 目的：
 * - 3引数版のとき Idempotency-Key がヘッダに付くこと
 * - 2引数版（null 委譲）のとき Idempotency-Key ヘッダが付かないこと（＝今回の NPE 対策の要）
 * - JWT を SecurityContext から取得して Authorization: Bearer ... が送られること
 *
 * テスト構成：
 * - HTTP サーバは MockWebServer を使用し、実際の HTTP リクエストを受けてヘッダ/ボディを検証する
 * - SecurityContextHolder に JwtAuthenticationToken を詰めて Authorization の生成を再現
 */
class SavingsClientTest {

    private MockWebServer server;
    private SavingsClient client;

    @BeforeEach
    void setUp() throws IOException {
        // モックサーバ起動
        server = new MockWebServer();
        server.start();

        // RestClient.Builder を素で生成し、ベースURLにモックサーバのURLを設定
        String baseUrl = server.url("/").toString(); // 末尾 / ありでも SavingsClient 側で安全に整形される
        client = new SavingsClient(RestClient.builder(), baseUrl);

        // SecurityContext に JWT 認証をセット
        Jwt jwt = Jwt.withTokenValue("dummy-token")
                .header("alg", "none")
                .claim("sub", "tester")
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_user")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() throws IOException {
        SecurityContextHolder.clearContext();
        server.shutdown();
    }

    /**
     * 3引数版：Idempotency-Key を付けた deposit 呼び出し。
     * - Authorization と Idempotency-Key ヘッダが送られること
     * - JSON ボディに amount が含まれること
     * - パスが /accounts/{id}/deposit であること
     */
    @Test
    void deposit_withIdempotencyHeader_whenProvided() throws Exception {
        // サーバ側の擬似レスポンス（JSON）
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"ok\":true}")
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));

        UUID accountId = UUID.randomUUID();
        Map<String, Object> res = client.deposit(accountId, new BigDecimal("123.45"), "REQ-XYZ");

        assertThat(res).containsEntry("ok", true);

        var recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/accounts/" + accountId + "/deposit");

        // Authorization: Bearer dummy-token
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer dummy-token");
        // Idempotency-Key: REQ-XYZ
        assertThat(recorded.getHeader("Idempotency-Key")).isEqualTo("REQ-XYZ");

        // ボディ JSON に amount が出ている（厳密パースまでは不要：簡易検証）
        assertThat(recorded.getBody().readUtf8()).contains("\"amount\":123.45");
    }

    /**
     * 2引数版：withdraw(accountId, amount) → 3引数版へ null 委譲。
     * - Idempotency-Key ヘッダが「付かない」こと（null を送らない）
     * - Authorization は付与されること
     */
    @Test
    void withdraw_twoArgDelegatesToThreeArg_withoutIdempotencyHeader() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"ok\":true}")
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));

        UUID accountId = UUID.randomUUID();
        Map<String, Object> res = client.withdraw(accountId, new BigDecimal("10.00"));

        assertThat(res).containsEntry("ok", true);

        var recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/accounts/" + accountId + "/withdraw");

        // Authorization はある
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer dummy-token");
        // Idempotency-Key は付与されない（= ヘッダが無い）
        assertThat(recorded.getHeader("Idempotency-Key")).isNull();
    }
}
