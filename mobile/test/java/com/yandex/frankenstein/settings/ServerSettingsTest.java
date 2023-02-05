package com.yandex.frankenstein.settings;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerSettingsTest {

    private static final String HOST = "42.100.42.100";
    private static final long DEFAULT_TIMEOUT = 12L;
    private static final String DEFAULT_LISTEN_HOST = "0.0.0.0";

    private final ServerSettings mServerSettings = new ServerSettings(new JSONObject()
            .put(ServerSettingsKeys.HOST, HOST)
    );

    @Test
    public void testGetRemoteHost() {
        assertThat(mServerSettings.getRemoteHost()).isEqualTo(HOST);
    }

    @Test
    public void testGetListenHost() {
        assertThat(mServerSettings.getListenHost()).isEqualTo(DEFAULT_LISTEN_HOST);
    }

    @Test
    public void testGetTimeout() {
        assertThat(mServerSettings.getTimeout()).isEqualTo(DEFAULT_TIMEOUT);
    }

    @Test
    public void testGetUrl() {
        final int port = 42;
        mServerSettings.setPort(port);
        assertThat(mServerSettings.getUrl()).isEqualTo(String.format(Locale.US, "http://%s:%d", HOST, port));
    }
}
