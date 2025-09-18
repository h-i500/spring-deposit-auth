package app.mstd.mashup;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Quarkusテスト用のWireMockリソース。
 * - Savings/TermDeposits で2台起動
 * - Rest Clientの接続先をbaseUrlに差し替え
 * - /savings, /savings/accounts, 互換の /accounts も用意
 * - /deposits, /deposits/accounts も用意
 * - JSONは配列で返す。type は SAVINGS / TERM で統一
 */
public class WireMockResources implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = Logger.getLogger(WireMockResources.class);

    public static WireMockServer savingsMock;
    public static WireMockServer timeDepositMock;

    @Override
    public Map<String, String> start() {
        savingsMock = new WireMockServer(wireMockConfig().dynamicPort());
        timeDepositMock = new WireMockServer(wireMockConfig().dynamicPort());
        savingsMock.start();
        timeDepositMock.start();

        LOG.infov("Savings WireMock started on {0}", savingsMock.baseUrl());
        LOG.infov("TimeDeposit WireMock started on {0}", timeDepositMock.baseUrl());

        initStubs();

        Map<String, String> props = new HashMap<>();
        props.put("quarkus.rest-client.\"app.mstd.client.SavingsServiceClient\".url", savingsMock.baseUrl());
        props.put("quarkus.rest-client.\"app.mstd.client.TimeDepositServiceClient\".url", timeDepositMock.baseUrl());
        return props;
    }

    @Override
    public void stop() {
        if (savingsMock != null) {
            savingsMock.stop();
            savingsMock = null;
        }
        if (timeDepositMock != null) {
            timeDepositMock.stop();
            timeDepositMock = null;
        }
    }

    /** 起動時のデフォルトスタブ定義 */
    public static void initStubs() {
        if (savingsMock == null || timeDepositMock == null) return;

        // ===== Savings =====
        // GET /savings?owner=...
        savingsMock.stubFor(
            get(com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo("/savings"))
                .withQueryParam("owner", matching(".+"))
                .atPriority(1)
                .willReturn(okJson("[" +
                    "  {" +
                    "    \"type\":\"SAVINGS\"," +
                    "    \"accountId\":\"SV-001\"," +
                    "    \"owner\":\"CUST001\"," +
                    "    \"balance\":50000," +
                    "    \"currency\":\"JPY\"" +
                    "  }" +
                    "]").withHeader("Content-Type", "application/json"))
        );

        // GET /savings/accounts?owner=...
        savingsMock.stubFor(
            get(com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo("/savings/accounts"))
                .withQueryParam("owner", matching(".+"))
                .atPriority(1)
                .willReturn(okJson("[" +
                    "  {" +
                    "    \"type\":\"SAVINGS\"," +
                    "    \"accountId\":\"SV-001\"," +
                    "    \"owner\":\"CUST001\"," +
                    "    \"balance\":50000," +
                    "    \"currency\":\"JPY\"" +
                    "  }" +
                    "]").withHeader("Content-Type", "application/json"))
        );

        // ★互換: GET /accounts?owner=... （クライアントが /accounts を叩くケースに対応）
        savingsMock.stubFor(
            get(com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo("/accounts"))
                .withQueryParam("owner", matching(".+"))
                .atPriority(1)
                .willReturn(okJson("[" +
                    "  {" +
                    "    \"type\":\"SAVINGS\"," +
                    "    \"accountId\":\"SV-001\"," +
                    "    \"owner\":\"CUST001\"," +
                    "    \"balance\":50000," +
                    "    \"currency\":\"JPY\"" +
                    "  }" +
                    "]").withHeader("Content-Type", "application/json"))
        );

        // ===== Term Deposits =====
        // GET /deposits?owner=...
        timeDepositMock.stubFor(
            get(com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo("/deposits"))
                .withQueryParam("owner", matching(".+"))
                .atPriority(1)
                .willReturn(okJson("[" +
                    "  {" +
                    "    \"type\":\"TERM\"," +
                    "    \"accountId\":\"TD-001\"," +
                    "    \"owner\":\"CUST001\"," +
                    "    \"principal\":300000," +
                    "    \"currency\":\"JPY\"," +
                    "    \"maturityDate\":\"2030-12-31\"" +
                    "  }" +
                    "]").withHeader("Content-Type", "application/json"))
        );

        // GET /deposits/accounts?owner=...
        timeDepositMock.stubFor(
            get(com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo("/deposits/accounts"))
                .withQueryParam("owner", matching(".+"))
                .atPriority(1)
                .willReturn(okJson("[" +
                    "  {" +
                    "    \"type\":\"TERM\"," +
                    "    \"accountId\":\"TD-001\"," +
                    "    \"owner\":\"CUST001\"," +
                    "    \"principal\":300000," +
                    "    \"currency\":\"JPY\"," +
                    "    \"maturityDate\":\"2030-12-31\"" +
                    "  }" +
                    "]").withHeader("Content-Type", "application/json"))
        );

        LOG.info("WireMock stubs initialized");
    }

    /** Fallbackテストで500を仕込みたい時に使うヘルパ（互換用） */
    public static void stubTermAccounts500Once() {
        if (timeDepositMock == null) return;
        timeDepositMock.stubFor(
            get(com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo("/deposits/accounts"))
                .withQueryParam("owner", matching(".+"))
                .willReturn(aResponse().withStatus(500))
        );
    }
}
