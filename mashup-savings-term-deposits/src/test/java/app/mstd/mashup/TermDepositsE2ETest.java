package app.mstd.mashup;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * E2E テスト（機能疎通の正常系）
 *
 * 目的:
 *  - アプリを実起動し、REST → REST Client → WireMock まで通す
 *  - 認証は test プロファイルでバイパス（JWTは使わない／送らない）
 *
 * 検証:
 *  - /api/deposits/accounts?owner=CUST001 が 200 + 最低限のJSON構造
 *  - /api/savings/accounts?owner=CUST001 が 200 + 最低限のJSON構造
 *
 * 注意:
 *  - Authorization ヘッダは付けない（401の原因になるため）
 *  - 下流は WireMockResources がスタブ化
 */
@QuarkusTest
@QuarkusTestResource(WireMockResources.class) // ※ deprecated 警告は出ますが実行は可能。移行は別途でOK。
@TestSecurity(user = "tester", roles = {"user"}) // 認可が有効でも落ちないように保険。不要なら外してもOK。
public class TermDepositsE2ETest {

    /**
     * 定期預金 API: /api/deposits/accounts
     * 期待:
     *  - 200
     *  - Content-Type が application/json (charset付き許容)
     *  - 配列が1件以上
     *  - 先頭要素: owner=CUST001, type=TERM
     */
    @Test
    void deposits_should_return_200_and_valid_body() {
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/api/deposits/accounts?owner=CUST001")
        .then()
            .statusCode(200)
            .contentType(startsWith("application/json"))
            .body("size()", greaterThan(0))
            .body("[0].owner", equalTo("CUST001"))
            .body("[0].type",  equalTo("TERM"));
    }

    /**
     * 普通預金 API: /api/savings/accounts
     * 期待:
     *  - 200
     *  - Content-Type が application/json (charset付き許容)
     *  - 配列が1件以上
     *  - 先頭要素: owner=CUST001, type=SAVINGS
     */
    @Test
    void savings_should_return_200_and_valid_body() {
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/api/savings/accounts?owner=CUST001")
        .then()
            .statusCode(200)
            .contentType(startsWith("application/json"))
            .body("size()", greaterThan(0))
            .body("[0].owner", equalTo("CUST001"))
            .body("[0].type",  equalTo("SAVINGS"));
    }
}
