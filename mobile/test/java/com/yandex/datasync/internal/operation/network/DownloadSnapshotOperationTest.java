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
import com.yandex.datasync.internal.database.repository.SnapshotRepository;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.util.NetworkSecurityPolicyShadow;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
public class DownloadSnapshotOperationTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_TOKEN = "mock_token";

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private MockWebServer mockWebServer;

    private Api api;

    private final MockCallback callback = new MockCallback();

    private DatabaseManager databaseManager;

    private String jsonString;

    @Before
    public void setUp() throws IOException {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        fillDatabaseInfo();

        jsonString = ResourcesUtil.getTextFromFile("database_snapshot.json");

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

        final SnapshotResponse expected =
                new Moshi.Builder().build().adapter(SnapshotResponse.class)
                        .fromJson(jsonString);

        final DownloadSnapshotOperation operation = new DownloadSnapshotOperation(MOCK_CONTEXT,
                                                                                  MOCK_DATABASE_ID,
                                                                                  api,
                                                                                  databaseManager,
                                                                                  callback);

        operation.run();

        assertRecords(callback.getSnapshot().getRecords(),
                      expected.getRecords());
    }

    @Test
    public void testSave() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(jsonString);
        mockResponse.setResponseCode(HttpStatusCode.CREATED);

        mockWebServer.enqueue(mockResponse);

        final DownloadSnapshotOperation operation = new DownloadSnapshotOperation(MOCK_CONTEXT,
                                                                                  MOCK_DATABASE_ID,
                                                                                  api,
                                                                                  databaseManager,
                                                                                  callback);

        operation.run();

        final SnapshotRepository snapshotRepository = new SnapshotRepository(databaseManager,
                                                                             MOCK_CONTEXT,
                                                                             MOCK_DATABASE_ID);

        assertRecords(callback.getSnapshot().getRecords(),
                      snapshotRepository.get().getRecords());
    }

    @Test
    public void testError() {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.NOT_FOUND);
        mockResponse.setBody("{}");
        mockWebServer.enqueue(mockResponse);

        final DownloadSnapshotOperation operation = new DownloadSnapshotOperation(MOCK_CONTEXT,
                                                                                  MOCK_DATABASE_ID,
                                                                                  api,
                                                                                  databaseManager,
                                                                                  callback);

        operation.run();

        mockWebServer.enqueue(mockResponse);
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

    private void fillDatabaseInfo() throws IOException {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(MOCK_CONTEXT);
        final DatabasesRepository changesRepository = new DatabasesRepository(databaseWrapper);
        final String databaseInfo = ResourcesUtil.getTextFromFile("get_database_info.json");
        final DatabaseDto databaseDto =
                new Moshi.Builder().build().adapter(DatabaseDto.class)
                        .fromJson(databaseInfo);
        changesRepository.save(databaseDto);
    }

    private class MockCallback implements DownloadSnapshotOperation.Callback {

        private SnapshotResponse snapshot;

        private BaseException exception;

        @Override
        public void onSnapshotDownloaded(@NonNull final SnapshotResponse snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public void onSnapshotLoadFailed(@NonNull final BaseException exception) {
            this.exception = exception;
        }

        public SnapshotResponse getSnapshot() {
            return snapshot;
        }

        public BaseException getException() {
            return exception;
        }
    }
}