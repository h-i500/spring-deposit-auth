package app.mstd.mashup;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

@QuarkusTest
class PathProbeTest {

    @Test
    void probeCandidates() {
        // よくあるパス候補（必要なら増やしてください）
        List<String> paths = List.of(
            "/api/mashup/term-deposits",
            "/api/mashup/savings-term-deposits",
            "/api/deposits/mashup/term",
            "/api/mashup/termDeposits",
            "/api/term-deposits"
        );
        // 顧客IDの候補パラメータ名
        List<String> custParams = List.of("customerId", "customerNumber", "custId", "customer_id");

        // OAuth2/OIDC っぽい必須パラメータの候補セットを段階的に試す
        List<Map<String, String>> oauthParamSets = List.of(
            Map.of(), // まずは無し
            Map.of("redirect_uri", "http://localhost/dummy"),
            Map.of("redirect_uri", "http://localhost/dummy", "response_type", "code"),
            Map.of("redirect_uri", "http://localhost/dummy", "response_type", "code", "client_id", "test-client"),
            Map.of("redirect_uri", "http://localhost/dummy", "response_type", "code", "client_id", "test-client", "scope", "openid")
        );

        for (String path : paths) {
            for (String custParam : custParams) {
                for (Map<String, String> oauth : oauthParamSets) {
                    var spec = given().accept("application/json")
                                      .log().all()
                                      .queryParam(custParam, TestConstants.SAMPLE_CUSTOMER_ID);
                    // OAuth2 系の候補を付与
                    for (var e : oauth.entrySet()) spec = spec.queryParam(e.getKey(), e.getValue());

                    var res = spec.when().get(path).then().log().ifValidationFails().extract().response();
                    int sc = res.statusCode();
                    String body = res.asString();
                    System.out.printf("PROBE path=%s custParam=%s oauth=%s -> %d %s%n",
                        path, custParam, oauth.keySet(), sc, body);
                }
            }
        }
    }
}
