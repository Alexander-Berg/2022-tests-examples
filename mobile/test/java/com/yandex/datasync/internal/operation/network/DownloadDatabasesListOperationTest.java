/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.network;

import androidx.annotation.NonNull;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.Config;
import com.yandex.datasync.Credentials;
import com.yandex.datasync.LogLevel;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.api.Api;
import com.yandex.datasync.internal.api.ApiFactory;
import com.yandex.datasync.internal.api.HttpStatusCode;
import com.yandex.datasync.internal.api.exceptions.BaseException;
import com.yandex.datasync.internal.api.exceptions.http.NotFoundException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.DatabasesRepository;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.DatabasesResponse;
import com.yandex.datasync.util.NetworkSecurityPolicyShadow;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.yandex.datasync.asserters.DatabaseDtoListAsserter.assertDatabasesList;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(shadows = NetworkSecurityPolicyShadow.class)
public class DownloadDatabasesListOperationTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_TOKEN = "mock_token";

    private static final String MOCK_USER_ID = "mock_user_id";

    private MockWebServer mockWebServer;

    private Api api;

    private final MockCallback callback = new MockCallback();

    private DatabaseManager databaseManager;

    private SQLiteDatabaseWrapper databaseWrapper;

    private String jsonString;

    @Before
    public void setUp() throws IOException {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        databaseWrapper = databaseManager.openDatabaseWrapped(MOCK_CONTEXT);

        jsonString = ResourcesUtil.getTextFromFile("get_databases_list.json");

        initApi();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testRun() throws IOException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(jsonString);
        mockResponse.setResponseCode(HttpStatusCode.CREATED);

        mockWebServer.enqueue(mockResponse);

        final DatabasesResponse databases =
                new Moshi.Builder().build().adapter(DatabasesResponse.class).fromJson(jsonString);

        final DownloadDatabasesListOperation operation =
                new DownloadDatabasesListOperation(MOCK_CONTEXT, api, databaseManager, callback);

        operation.run();

        assertDatabasesList(callback.getDatabaseList(), databases.getDatabaseList());
    }

    @Test
    public void testSave() {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(jsonString);
        mockResponse.setResponseCode(HttpStatusCode.CREATED);

        mockWebServer.enqueue(mockResponse);

        final DownloadDatabasesListOperation operation =
                new DownloadDatabasesListOperation(MOCK_CONTEXT, api, databaseManager, callback);

        operation.run();

        final DatabasesRepository databasesRepository = new DatabasesRepository(databaseWrapper);

        assertDatabasesList(callback.getDatabaseList(),
                            databasesRepository.get().getDatabaseList());
    }

    @Test
    public void testSecondDownloading() {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(jsonString);
        mockResponse.setResponseCode(HttpStatusCode.CREATED);

        mockWebServer.enqueue(mockResponse);
        mockWebServer.enqueue(mockResponse);

        final DownloadDatabasesListOperation operation =
                new DownloadDatabasesListOperation(MOCK_CONTEXT, api, databaseManager, callback);

        operation.run();
        operation.run();

        final DatabasesRepository databasesRepository = new DatabasesRepository(databaseWrapper);

        assertDatabasesList(callback.getDatabaseList(),
                            databasesRepository.get().getDatabaseList());
    }

    @Test
    public void testError() {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.NOT_FOUND);
        mockResponse.setBody("{}");
        mockWebServer.enqueue(mockResponse);

        final DownloadDatabasesListOperation operation =
                new DownloadDatabasesListOperation(MOCK_CONTEXT, api, databaseManager, callback);

        operation.run();

        assertThat(callback.getException(), instanceOf(NotFoundException.class));
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

    private class MockCallback implements DownloadDatabasesListOperation.Callback {

        private List<DatabaseDto> databasesList;

        private BaseException exception;

        @Override
        public void onDatabaseListDownloaded(@NonNull final List<DatabaseDto> databasesList) {
            this.databasesList = databasesList;
        }

        @Override
        public void onDatabaseListFailed(@NonNull final BaseException exception) {
            this.exception = exception;
        }

        public List<DatabaseDto> getDatabaseList() {
            return databasesList;
        }

        public BaseException getException() {
            return exception;
        }
    }
}