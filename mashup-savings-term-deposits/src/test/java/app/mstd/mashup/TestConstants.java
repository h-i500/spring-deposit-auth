package app.mstd.mashup;

/** プロジェクト固有の値（ここを変えるだけでテストが追従） */
public final class TestConstants {

    // TestConstants に追記（必要なものだけ true/値を入れてください）
    // public static final boolean OAUTH_REQUIRED = true; // 必須なら true
    // public static final String OAUTH_REDIRECT_URI = "http://localhost/dummy";
    // public static final String OAUTH_RESPONSE_TYPE = "code";
    // public static final String OAUTH_CLIENT_ID = "test-client";
    // public static final String OAUTH_SCOPE = "openid";


    /** マッシュアップ公開エンドポイント（暫定）※ PathProbeTest の結果で上書きしてください */
    public static final String ENDPOINT_PATH = "/api/mashup/term-deposits";

    /** クエリパラメータ（顧客IDなど）。PathProbeTestの結果に応じて要修正 */
    public static final String PARAM_CUSTOMER_ID = "customerId";
    public static final String SAMPLE_CUSTOMER_ID = "CUST001";

    /** 下流サービスのベースURLを渡す設定キー（実装のapplication.propertiesに合わせて修正） */
    public static final String PROP_SAVINGS_BASE_URL   = "downstream.savings.base-url";
    public static final String PROP_TIMEDEP_BASE_URL   = "downstream.time-deposit.base-url";

    private TestConstants() {}
}
