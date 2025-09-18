package app.mstd.mashup;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;

import com.github.tomakehurst.wiremock.WireMockServer;

/** 既存テスト互換のための薄いラッパー */
public final class WireMockStubs {

    private WireMockStubs() {}

    /** Savingsサーバ取得（nullなら IllegalStateException） */
    public static WireMockServer saving() {
        if (WireMockResources.savingsMock == null) {
            throw new IllegalStateException("WireMockResources.savingsMock is not initialized. "
                + "Annotate your test with @QuarkusTestResource(WireMockResources.class).");
        }
        return WireMockResources.savingsMock;
    }

    /** Termサーバ取得（nullなら IllegalStateException） */
    public static WireMockServer term() {
        if (WireMockResources.timeDepositMock == null) {
            throw new IllegalStateException("WireMockResources.timeDepositMock is not initialized. "
                + "Annotate your test with @QuarkusTestResource(WireMockResources.class).");
        }
        return WireMockResources.timeDepositMock;
    }

    /** 既存テスト互換: 定期預金 /deposits/accounts を 500 返しにする */
    public static void stubTimeDeposit500() {
        term().stubFor(
            get(com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo("/deposits/accounts"))
                .withQueryParam("owner", matching(".+"))
                .willReturn(aResponse().withStatus(500))
        );
    }

    /** スタブを初期状態に戻したい場合に利用 */
    public static void resetAll() {
        if (WireMockResources.savingsMock != null) {
            WireMockResources.savingsMock.resetAll();
        }
        if (WireMockResources.timeDepositMock != null) {
            WireMockResources.timeDepositMock.resetAll();
        }
        // デフォルト定義を再配置
        WireMockResources.initStubs();
    }
}
