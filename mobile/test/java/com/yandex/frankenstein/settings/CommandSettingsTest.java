package com.yandex.frankenstein.settings;

import org.json.JSONObject;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandSettingsTest {

    private static final String HOST = "42.100.42.100";
    private static final long TIMEOUT = 42;
    private static final String CASE_PATH = "/case";
    private static final String COMMAND_PATH = "/command";
    private static final String RESULT_PATH = "/result";
    private static final String CALLBACK_PATH = "/callback";

    private final CommandSettings mCommandSettings = new CommandSettings(new JSONObject()
            .put(ServerSettingsKeys.HOST, HOST)
            .put(ServerSettingsKeys.TIMEOUT_SECONDS, TIMEOUT)
            .put("case_path", CASE_PATH)
            .put("request_path", COMMAND_PATH).put("result_path", RESULT_PATH).put("callback_path", CALLBACK_PATH));

    @Test
    public void testGetServerSettings() {
        final ServerSettings serverSettings = mCommandSettings.getServerSettings();

        assertThat(serverSettings.getRemoteHost()).isEqualTo(HOST);
        assertThat(serverSettings.getTimeout()).isEqualTo(TIMEOUT);
    }

    @Test
    public void testGetCasePath() {
        assertThat(mCommandSettings.getCasePath()).isEqualTo(CASE_PATH);
    }

    @Test
    public void testGetRequestPath() {
        assertThat(mCommandSettings.getRequestPath()).isEqualTo(COMMAND_PATH);
    }

    @Test
    public void testGetResultPath() {
        assertThat(mCommandSettings.getResultPath()).isEqualTo(RESULT_PATH);
    }

    @Test
    public void testGetCallbackPath() {
        assertThat(mCommandSettings.getCallbackPath()).isEqualTo(CALLBACK_PATH);
    }

    @Test
    public void testGetCaseUrl() {
        final ServerSettings serverSettings = mCommandSettings.getServerSettings();

        assertThat(mCommandSettings.getCaseUrl()).isEqualTo(serverSettings.getUrl() + CASE_PATH);
    }

    @Test
    public void testGetRequestUrl() {
        final ServerSettings serverSettings = mCommandSettings.getServerSettings();

        assertThat(mCommandSettings.getRequestUrl()).isEqualTo(serverSettings.getUrl() + COMMAND_PATH);
    }

    @Test
    public void testGetResultUrl() {
        final ServerSettings serverSettings = mCommandSettings.getServerSettings();

        assertThat(mCommandSettings.getResultUrl()).isEqualTo(serverSettings.getUrl() + RESULT_PATH);
    }

    @Test
    public void testGetCallbackUrl() {
        final ServerSettings serverSettings = mCommandSettings.getServerSettings();

        assertThat(mCommandSettings.getCallbackUrl()).isEqualTo(serverSettings.getUrl() + CALLBACK_PATH);
    }
}
