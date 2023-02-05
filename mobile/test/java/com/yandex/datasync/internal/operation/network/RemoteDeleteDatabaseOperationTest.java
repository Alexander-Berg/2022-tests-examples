/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.network;

import android.database.Cursor;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.Config;
import com.yandex.datasync.Credentials;
import com.yandex.datasync.LogLevel;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.api.Api;
import com.yandex.datasync.internal.api.ApiFactory;
import com.yandex.datasync.internal.api.HttpStatusCode;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.DatabasesRepository;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.response.DatabasesResponse;
import com.yandex.datasync.internal.operation.Operation;
import com.yandex.datasync.internal.operation.local.LocalDeleteDatabaseOperation;
import com.yandex.datasync.util.NetworkSecurityPolicyShadow;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.yandex.datasync.asserters.DatabaseDtoListAsserter.assertDatabasesList;
import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Databases.Rows.SYNCED;
import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Databases.TABLE_NAME;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(shadows = NetworkSecurityPolicyShadow.class)
public class RemoteDeleteDatabaseOperationTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private final MockRawDataObserver observable = new MockRawDataObserver();

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_TOKEN = "mock_token";

    private static final String MOCK_USER_ID = "mock_user_id";

    private DatabaseManager databaseManager;

    private SQLiteDatabaseWrapper databaseWrapper;

    private MockWebServer mockWebServer;

    private Api api;

    @Before
    public void setUp() throws IOException {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        databaseWrapper = databaseManager.openDatabaseWrapped(MOCK_CONTEXT);

        initApi();
    }

    @Test
    public void testRun() throws IOException {
        final String jsonString = ResourcesUtil.getTextFromFile("get_databases_list.json");

        final DatabasesResponse databases =
                new Moshi.Builder().build().adapter(DatabasesResponse.class).fromJson(jsonString);

        final DatabasesRepository repository = new DatabasesRepository(databaseWrapper);
        repository.save(databases);

        final Operation localOperation = new LocalDeleteDatabaseOperation(MOCK_CONTEXT,
                                                                          MOCK_DATABASE_ID,
                                                                          databaseManager);
        localOperation.run();

        final Operation apiOperation = new RemoteDeleteDatabaseOperation(MOCK_CONTEXT,
                                                                         api,
                                                                         databaseManager,
                                                                         observable);
        apiOperation.run();

        databases.getDatabaseList().remove(0);

        final DatabasesResponse databasesList = repository.get();

        assertDatabasesList(databasesList.getDatabaseList(), databases.getDatabaseList());

        final String selection = SYNCED + " is not null";

        try (final Cursor cursor = databaseWrapper.query(TABLE_NAME, selection, new String[0])) {
            assertThat(cursor.getCount(), is(0));
        }
    }

    private void initApi() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        final Config.Builder builder = new Config.Builder()
                .baseUrl(mockWebServer.url("/").toString())
                .credentials(new Credentials(MOCK_USER_ID, MOCK_TOKEN))
                .logLevel(LogLevel.DEBUG);

        api = ApiFactory.create(builder.build());

        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.NO_CONTENT);

        mockWebServer.enqueue(mockResponse);
    }
}