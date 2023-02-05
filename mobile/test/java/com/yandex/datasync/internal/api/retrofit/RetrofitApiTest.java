/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.api.retrofit;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.Config;
import com.yandex.datasync.Credentials;
import com.yandex.datasync.LogLevel;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.api.Api;
import com.yandex.datasync.internal.api.ApiFactory;
import com.yandex.datasync.internal.api.HttpStatusCode;
import com.yandex.datasync.internal.model.request.ChangesRequest;
import com.yandex.datasync.internal.model.request.DatabaseTitleRequest;
import com.yandex.datasync.internal.model.response.ApplyChangesResponse;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.DatabasesResponse;
import com.yandex.datasync.internal.model.response.DeltasResponse;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.yandex.datasync.asserters.ApplyChangesResponseAsserter.assertApplyChangesResponse;
import static com.yandex.datasync.asserters.DatabaseDtoAsserter.assertDatabase;
import static com.yandex.datasync.asserters.DatabaseInfoDtoAsserter.assertDatabaseInfo;
import static com.yandex.datasync.asserters.DatabasesResponseAsserter.assertDatabases;
import static com.yandex.datasync.asserters.DeltasResponseAsserter.assertDeltas;
import static com.yandex.datasync.asserters.SnapshotResponseAsserter.assertSnapshot;
import static org.junit.Assert.assertTrue;

public class RetrofitApiTest {

    private static final String MOCK_TOKEN = "mock_token";

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "mock_database_id";

    private static final int MOCK_OFFSET = 0;

    private static final int MOCK_LIMIT = 0;

    private static final int MOCK_REVISION = 10;

    private MockWebServer mockWebServer;

    private Api api;

    private Moshi moshi;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        moshi = new Moshi.Builder().build();

        final Config.Builder builder = new Config.Builder()
                .baseUrl(mockWebServer.url("/").toString())
                .credentials(new Credentials(MOCK_USER_ID, MOCK_TOKEN))
                .logLevel(LogLevel.DEBUG);

        api = ApiFactory.create(builder.build());
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void testCreateDatabase() throws Exception {

        final String jsonString = ResourcesUtil.getTextFromFile("database_create.json");

        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(jsonString);
        mockResponse.setResponseCode(HttpStatusCode.CREATED);

        mockWebServer.enqueue(mockResponse);

        final DatabaseDto actual = api.createDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final DatabaseDto expected = moshi.adapter(DatabaseDto.class).fromJson(jsonString);

        assertDatabase(actual, expected);
    }

    @Test
    public void testGetDatabaseList() throws Exception {

        final String jsonString = ResourcesUtil.getTextFromFile("get_databases_list.json");

        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(jsonString);
        mockResponse.setResponseCode(HttpStatusCode.OK);

        mockWebServer.enqueue(mockResponse);
        final DatabasesResponse actual = api.getDatabaseList(MOCK_CONTEXT, MOCK_OFFSET, MOCK_LIMIT);
        final DatabasesResponse expected =
                moshi.adapter(DatabasesResponse.class).fromJson(jsonString);

        assertDatabases(actual, expected);
    }

    @Test
    public void testGetDatabaseSnapshot() throws Exception {
        final String jsonString = ResourcesUtil.getTextFromFile("database_snapshot.json");

        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(jsonString);
        mockResponse.setResponseCode(HttpStatusCode.OK);

        mockWebServer.enqueue(mockResponse);

        final SnapshotResponse actual = api.getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotResponse expected =
                moshi.adapter(SnapshotResponse.class).fromJson(jsonString);

        assertSnapshot(actual, expected);
    }

    @Test
    public void testGetDatabaseInfo() throws Exception {
        final String jsonString = ResourcesUtil.getTextFromFile("get_database_info.json");

        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(jsonString);
        mockResponse.setResponseCode(HttpStatusCode.OK);

        mockWebServer.enqueue(mockResponse);

        final DatabaseDto actual = api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        final DatabaseDto expected = moshi.adapter(DatabaseDto.class).fromJson(jsonString);

        assertDatabaseInfo(actual, expected);
    }

    @Test
    public void testPatchDatabaseTitle() throws Exception {
        final String jsonString = ResourcesUtil.getTextFromFile("patch_database_title.json");

        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(jsonString);
        mockResponse.setResponseCode(HttpStatusCode.OK);

        mockWebServer.enqueue(mockResponse);

        final DatabaseTitleRequest databaseTitleRequest = new DatabaseTitleRequest();

        final DatabaseDto actual =
                api.patchDatabaseTitle(MOCK_CONTEXT, MOCK_DATABASE_ID, databaseTitleRequest);
        final DatabaseDto expected = moshi.adapter(DatabaseDto.class).fromJson(jsonString);

        assertDatabase(actual, expected);
    }

    @Test
    public void testPostChanges() throws Exception {

        final String jsonString = ResourcesUtil.getTextFromFile("post_changes.json");

        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(jsonString);
        mockResponse.setResponseCode(HttpStatusCode.OK);
        mockResponse.setHeader("ETag", MOCK_REVISION);

        mockWebServer.enqueue(mockResponse);
        final ChangesRequest changes = new ChangesRequest();

        final ApplyChangesResponse actual =
                api.postChanges(MOCK_CONTEXT, MOCK_DATABASE_ID, MOCK_REVISION, changes);

        final ApplyChangesResponse expected =
                moshi.adapter(ApplyChangesResponse.class).fromJson(jsonString);
        expected.setRevision(MOCK_REVISION);

        assertApplyChangesResponse(actual, expected);
    }

    @Test
    public void testGetChanges() throws Exception {

        final String jsonString = ResourcesUtil.getTextFromFile("get_changes.json");

        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(jsonString);
        mockResponse.setResponseCode(HttpStatusCode.OK);

        mockWebServer.enqueue(mockResponse);

        final DeltasResponse
                actualDeltas = api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, MOCK_REVISION);
        final DeltasResponse expectedDeltas =
                moshi.adapter(DeltasResponse.class).fromJson(jsonString);

        assertDeltas(actualDeltas, expectedDeltas);
    }

    @Test
    public void testRemoveDatabase() throws Exception {

        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.NO_CONTENT);

        mockWebServer.enqueue(mockResponse);

        final boolean removed = api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertTrue(removed);
    }
}