package com.yandex.autotests;

import com.yandex.autotests.fakes.Network;
import com.yandex.autotests.runner.device.DeviceJUnit4Runner;
import com.yandex.autotests.runner.device.EnvironmentSettings;
import com.yandex.autotests.runner.device.ExampleRegistry;
import com.yandex.autotests.runner.device.NetworkSettings;
import com.yandex.frankenstein.annotations.TestCaseId;
import com.yandex.frankenstein.settings.UrlComponents;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(DeviceJUnit4Runner.class)
public class NetworkTest {

    private static final String BODY = "some_body";

    private final EnvironmentSettings mEnvironmentSettings = ExampleRegistry.getEnvironmentSettings();
    private final NetworkSettings mNetworkSettings = mEnvironmentSettings.getNetworkSettings();
    private final Map<String, UrlComponents> mMockHostRules = mNetworkSettings.getMockHostRules();
    private final String mSomeHost = mMockHostRules.keySet().iterator().next();
    private final UrlComponents mSomeUrlComponent = mMockHostRules.get(mSomeHost);
    private final MockWebServer mMockWebServer = new MockWebServer();

    @Before
    public void before() throws IOException {
        mMockWebServer.start(InetAddress.getByName("0.0.0.0"), mSomeUrlComponent.port);
    }

    @After
    public void after() throws IOException {
        mMockWebServer.shutdown();
    }

    @Test
    @TestCaseId(6)
    public void testGet() {
        mMockWebServer.enqueue(new MockResponse().setBody(BODY));
        final String result = Network.get("https://" + mSomeHost);
        assertEquals(1, mMockWebServer.getRequestCount());
        assertEquals(BODY, result);
    }
}
