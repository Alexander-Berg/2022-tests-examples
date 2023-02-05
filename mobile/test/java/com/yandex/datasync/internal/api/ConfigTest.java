/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.api;

import com.yandex.datasync.Config;
import com.yandex.datasync.Credentials;
import com.yandex.datasync.LogLevel;
import com.yandex.datasync.ProtocolType;
import com.yandex.datasync.internal.api.retrofit.RetrofitApiCreator;

import org.junit.Test;

import okhttp3.OkHttpClient;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ConfigTest {

    private static final String MOCK_TOKEN = "mock_token";

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_USER_AGENT = "mock_user_agent";

    private static final String MOCK_URL = "https://yandex.ru";

    private static final Credentials MOCK_CREDENTIALS = new Credentials(MOCK_USER_ID, MOCK_TOKEN);

    @Test
    public void testDefaultBaseUrl() {
        final Config config = new Config.Builder()
                .credentials(MOCK_CREDENTIALS).build();

        final RetrofitApiCreator apiCreator = new RetrofitApiCreator(config);
        apiCreator.create();

        assertNotNull(config.getBaseUrl());
    }

    @Test
    public void testCustomBaseUrl() {
        final Config config = new Config.Builder()
                .baseUrl(MOCK_URL)
                .credentials(MOCK_CREDENTIALS)
                .build();

        final RetrofitApiCreator apiCreator = new RetrofitApiCreator(config);
        apiCreator.create();

        assertThat(MOCK_URL, is(equalTo(config.getBaseUrl())));
    }

    @Test
    public void testDefaultClient() {
        final Config config = new Config.Builder()
                .credentials(MOCK_CREDENTIALS)
                .build();

        final RetrofitApiCreator apiCreator = new RetrofitApiCreator(config);
        apiCreator.create();

        assertNotNull(config.getClient());
    }

    @Test
    public void testCustomClient() {
        final OkHttpClient client = new OkHttpClient();

        final Config config = new Config.Builder()
                .credentials(MOCK_CREDENTIALS)
                .client(client)
                .build();

        final RetrofitApiCreator apiCreator = new RetrofitApiCreator(config);
        apiCreator.create();

        assertThat(client, is(equalTo(config.getClient())));
    }

    @Test
    public void testDefaultLogLevel() {
        final Config config = new Config.Builder()
                .credentials(MOCK_CREDENTIALS)
                .build();

        final RetrofitApiCreator apiCreator = new RetrofitApiCreator(config);
        apiCreator.create();

        assertNotNull(config.getLogLevel());
    }

    @Test
    public void testCustomLogLevel() {
        final LogLevel logLevel = LogLevel.DEBUG;

        final Config config = new Config.Builder()
                .credentials(MOCK_CREDENTIALS)
                .logLevel(logLevel)
                .build();

        final RetrofitApiCreator apiCreator = new RetrofitApiCreator(config);
        apiCreator.create();

        assertThat(logLevel, is(equalTo(config.getLogLevel())));
    }

    @Test
    public void testDefaultProtocolType() {
        final Config config = new Config.Builder()
                .credentials(MOCK_CREDENTIALS)
                .build();

        final RetrofitApiCreator apiCreator = new RetrofitApiCreator(config);
        apiCreator.create();

        assertNotNull(config.getProtocolType());
    }

    @Test
    public void testCustomProtocolType() {
        final ProtocolType protocolType = ProtocolType.PROTOBUF;

        final Config config = new Config.Builder()
                .protocolType(protocolType)
                .build();

        final RetrofitApiCreator apiCreator = new RetrofitApiCreator(config);
        apiCreator.create();

        assertThat(protocolType, is(equalTo(config.getProtocolType())));
    }

    @Test
    public void testDefaultUserAgent() {
        final Config config = new Config.Builder()
                .credentials(MOCK_CREDENTIALS)
                .build();

        final RetrofitApiCreator apiCreator = new RetrofitApiCreator(config);
        apiCreator.create();

        assertNotNull(config.getUserAgent());
    }

    @Test
    public void testCustomUserAgent() {

        final Config config = new Config.Builder()
                .userAgent(MOCK_USER_AGENT)
                .build();

        final RetrofitApiCreator apiCreator = new RetrofitApiCreator(config);
        apiCreator.create();

        assertThat(MOCK_USER_AGENT, is(equalTo(MOCK_USER_AGENT)));
    }
}