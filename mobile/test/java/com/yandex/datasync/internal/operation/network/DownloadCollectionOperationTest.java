/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.network;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.Config;
import com.yandex.datasync.Credentials;
import com.yandex.datasync.LogLevel;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.api.Api;
import com.yandex.datasync.internal.api.ApiFactory;
import com.yandex.datasync.internal.api.HttpStatusCode;
import com.yandex.datasync.internal.api.exceptions.http.NotFoundException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.operation.Operation;
import com.yandex.datasync.util.NetworkSecurityPolicyShadow;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.yandex.datasync.asserters.RecordsDtoAsserter.assertRecords;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(shadows = NetworkSecurityPolicyShadow.class)
public class DownloadCollectionOperationTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_TOKEN = "mock_token";

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_COLLECTION_ID = "calendar";

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private MockWebServer mockWebServer;

    private final MockRawDataObserver observable = new MockRawDataObserver();

    private DatabaseManager databaseManager;

    @Mock
    private Api api;

    private String jsonString;

    @Before
    public void setUp() throws IOException {

        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        jsonString = ResourcesUtil.getTextFromFile("database_snapshot.json");

        initApi();
    }

    @Test
    public void testRun() throws IOException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(jsonString);
        mockResponse.setResponseCode(HttpStatusCode.CREATED);

        mockWebServer.enqueue(mockResponse);

        final SnapshotResponse expected =
                new Moshi.Builder().build().adapter(SnapshotResponse.class).fromJson(jsonString);

        final Operation operation = new DownloadCollectionOperation(MOCK_CONTEXT,
                                                                    MOCK_DATABASE_ID,
                                                                    MOCK_COLLECTION_ID,
                                                                    api,
                                                                    databaseManager,
                                                                    observable);
        operation.run();

        assertRecords(observable.getSnapshot().getRecords(),
                      expected.getRecords());
    }

    @Test
    public void testError() {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.NOT_FOUND);
        mockResponse.setBody("{}");
        mockWebServer.enqueue(mockResponse);

        final Operation operation = new DownloadCollectionOperation(MOCK_CONTEXT,
                                                                    MOCK_DATABASE_ID,
                                                                    MOCK_COLLECTION_ID,
                                                                    api,
                                                                    databaseManager,
                                                                    observable);
        operation.run();

        mockWebServer.enqueue(mockResponse);
        assertThat(observable.getException(), instanceOf(NotFoundException.class));
    }

    private void initApi() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        final Config.Builder builder = new Config.Builder()
                .baseUrl(mockWebServer.url("/").toString())
                .credentials(new Credentials(MOCK_USER_ID, MOCK_TOKEN))
                .logLevel(LogLevel.DEBUG);

        api = ApiFactory.create(builder.build());
    }
}