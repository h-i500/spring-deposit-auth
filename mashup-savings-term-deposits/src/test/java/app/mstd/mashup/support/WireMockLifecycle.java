package app.mstd.mashup.support;

import app.mstd.mashup.TestConstants;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class WireMockLifecycle implements QuarkusTestResourceLifecycleManager {
    public static WireMockServer savingsMock;
    public static WireMockServer timeDepMock;

    @Override
    public Map<String, String> start() {
        savingsMock = new WireMockServer(options().dynamicPort());
        timeDepMock = new WireMockServer(options().dynamicPort());
        savingsMock.start();
        timeDepMock.start();

        Map<String, String> props = new HashMap<>();
        props.put(TestConstants.PROP_SAVINGS_BASE_URL, "http://localhost:" + savingsMock.port());
        props.put(TestConstants.PROP_TIMEDEP_BASE_URL, "http://localhost:" + timeDepMock.port());
        return props;
    }

    @Override
    public void stop() {
        if (savingsMock != null) savingsMock.stop();
        if (timeDepMock != null) timeDepMock.stop();
    }
}
