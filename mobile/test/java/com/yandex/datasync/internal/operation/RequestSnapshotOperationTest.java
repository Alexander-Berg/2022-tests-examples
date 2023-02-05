/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation;

import androidx.annotation.NonNull;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.Config;
import com.yandex.datasync.Credentials;
import com.yandex.datasync.DataSyncManager;
import com.yandex.datasync.Datatype;
import com.yandex.datasync.LogLevel;
import com.yandex.datasync.WrappersObserverWithResult;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.api.HttpStatusCode;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.model.ChangesDto;
import com.yandex.datasync.internal.model.ValueDto;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.DeltaItemDto;
import com.yandex.datasync.internal.model.response.DeltasResponse;
import com.yandex.datasync.internal.model.response.FieldDto;
import com.yandex.datasync.internal.model.response.RecordDto;
import com.yandex.datasync.internal.model.response.RecordsDto;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.util.NetworkSecurityPolicyShadow;
import com.yandex.datasync.util.ResourcesUtil;
import com.yandex.datasync.wrappedModels.Collection;
import com.yandex.datasync.wrappedModels.Database;
import com.yandex.datasync.wrappedModels.Record;
import com.yandex.datasync.wrappedModels.Snapshot;
import com.yandex.datasync.wrappedModels.Value;
import com.yandex.datasync.wrappedModels.ValuesList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.yandex.datasync.asserters.DatabaseAsserter.assertDatabase;
import static com.yandex.datasync.asserters.SnapshotAsserter.assertSnapshot;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(shadows = NetworkSecurityPolicyShadow.class)
public class RequestSnapshotOperationTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_CONTEXT_STRING = MOCK_CONTEXT.nameLowerCase();

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String DATABASE_INFO = "get_database_info.json";

    private static final String DATABASE_INFO_UPDATED = "get_database_info_updated.json";

    private static final String MOCK_COLLECTION_ID = "sport";

    private static final String MOCK_TOKEN = "mock_token";

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final long MOCK_REVISION = 1L;

    private DataSyncManager dataSyncManager;

    private OperationProcessor operationProcessor;

    private WrappersObserverWithResult wrappersObserver;

    private static final Credentials MOCK_CREDENTIALS = new Credentials(MOCK_USER_ID, MOCK_TOKEN);

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws IOException {
        wrappersObserver = new WrappersObserverWithResult();
        operationProcessor = new ImmediatelyOperationProcessor();

        dataSyncManager = new DataSyncManager(RuntimeEnvironment.application);

        initDataSyncManager();

        dataSyncManager.addObserver(wrappersObserver);
    }

    @Test
    public void testFirstCall() throws IOException, InterruptedException {
        final DatabaseDto databaseInfo = enqueueDatabaseInfoRequest(DATABASE_INFO);
        enqueueSnapshotRequest();

        dataSyncManager.requestDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final RecordedRequest databaseInfoRequest = mockWebServer.takeRequest(1, SECONDS);
        final RecordedRequest snapshotRequest = mockWebServer.takeRequest(1, SECONDS);
        final RecordedRequest changesRequest = mockWebServer.takeRequest(1, SECONDS);

        assertNotNull(databaseInfoRequest);
        assertNotNull(snapshotRequest);
        assertNull(changesRequest);

        final String expectedPath = "/v1/data/" + MOCK_CONTEXT_STRING
                                    + "/databases/" + MOCK_DATABASE_ID + "/snapshot";

        assertThat(snapshotRequest.getPath(), is(expectedPath));
        assertThat(snapshotRequest.getMethod(), is("GET"));
        assertThat(snapshotRequest.getBodySize(), is(0L));

        assertNull(wrappersObserver.getError());
        assertNotNull(wrappersObserver.getSnapshot());

        databaseInfo.setFullSnapshotSynced(true);
        final Database expectedDatabase = new Database(dataSyncManager, MOCK_CONTEXT, databaseInfo);
        assertDatabase(wrappersObserver.getDatabase(), expectedDatabase);
        assertThat(wrappersObserver.getDatabaseRevision(), is(databaseInfo.getRevision()));

        final String changesString = ResourcesUtil.getTextFromFile("database_snapshot.json");
        final SnapshotResponse snapshotResponse =
                new Moshi.Builder().build().adapter(SnapshotResponse.class)
                        .fromJson(changesString);

        assertThat(wrappersObserver.getDatabaseRevision(), is(snapshotResponse.getRevision()));
    }

    @Test
    public void testSecondCall() throws IOException, InterruptedException {
        enqueueDatabaseInfoRequest(DATABASE_INFO);
        enqueueSnapshotRequest();
        final DatabaseDto databaseInfoUpdated = enqueueDatabaseInfoRequest(DATABASE_INFO_UPDATED);
        databaseInfoUpdated.setFullSnapshotSynced(true);
        enqueueChangesRequest();

        dataSyncManager.requestDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        dataSyncManager.requestDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final RecordedRequest databaseInfoRequest = mockWebServer.takeRequest(1, SECONDS);
        final RecordedRequest snapshotRequest = mockWebServer.takeRequest(1, SECONDS);
        final RecordedRequest databaseInfoRequest2 = mockWebServer.takeRequest(1, SECONDS);
        final RecordedRequest changesRequest = mockWebServer.takeRequest(1, SECONDS);

        assertNotNull(snapshotRequest);
        assertNotNull(databaseInfoRequest);
        assertNotNull(changesRequest);
        assertNotNull(databaseInfoRequest2);

        final String expectedPath = "/v1/data/" + MOCK_CONTEXT_STRING
                                    + "/databases/" + MOCK_DATABASE_ID + "/deltas"
                                    + "?base_revision=" + MOCK_REVISION;

        assertThat(changesRequest.getPath(), is(expectedPath));
        assertThat(changesRequest.getMethod(), is("GET"));
        assertThat(changesRequest.getBodySize(), is(0L));

        assertNull(wrappersObserver.getError());
        assertNotNull(wrappersObserver.getSnapshot());
        assertNotNull(wrappersObserver.getDatabase());

        final Database expectedDatabase = new Database(dataSyncManager,
                                                       MOCK_CONTEXT,
                                                       databaseInfoUpdated);
        assertDatabase(wrappersObserver.getDatabase(), expectedDatabase);
        assertThat(wrappersObserver.getDatabaseRevision(), is(databaseInfoUpdated.getRevision()));

        final String changesString = ResourcesUtil.getTextFromFile("get_changes.json");
        final DeltasResponse deltasResponse =
                new Moshi.Builder().build().adapter(DeltasResponse.class)
                        .fromJson(changesString);

        assertThat(wrappersObserver.getDatabaseRevision(), is(deltasResponse.getRevision()));
    }

    @Test
    public void testFirstCallForCollection() throws IOException, InterruptedException {
        final DatabaseDto databaseInfo = enqueueDatabaseInfoRequest(DATABASE_INFO);
        enqueueSnapshotRequest();

        dataSyncManager.requestCollection(MOCK_CONTEXT, MOCK_DATABASE_ID, MOCK_COLLECTION_ID);

        final RecordedRequest databaseInfoRequest = mockWebServer.takeRequest(1, SECONDS);
        final RecordedRequest snapshotRequest = mockWebServer.takeRequest(1, SECONDS);
        final RecordedRequest changesRequest = mockWebServer.takeRequest(1, SECONDS);

        assertNotNull(databaseInfoRequest);
        assertNotNull(snapshotRequest);
        assertNull(changesRequest);

        final String expectedPath = "/v1/data/" + MOCK_CONTEXT_STRING
                                    + "/databases/" + MOCK_DATABASE_ID + "/snapshot"
                                    + "?collection_id=" + MOCK_COLLECTION_ID;

        assertThat(snapshotRequest.getPath(), is(expectedPath));
        assertThat(snapshotRequest.getMethod(), is("GET"));
        assertThat(snapshotRequest.getBodySize(), is(0L));

        assertNull(wrappersObserver.getError());
        assertNotNull(wrappersObserver.getCollection());
        assertNotNull(wrappersObserver.getDatabase());

        databaseInfo.setFullSnapshotSynced(false);
        final Database expectedDatabase = new Database(dataSyncManager, MOCK_CONTEXT, databaseInfo);
        assertDatabase(wrappersObserver.getDatabase(), expectedDatabase);
        assertThat(wrappersObserver.getDatabaseRevision(), is(databaseInfo.getRevision()));

        final String changesString = ResourcesUtil.getTextFromFile("database_snapshot.json");
        final SnapshotResponse snapshotResponse =
                new Moshi.Builder().build().adapter(SnapshotResponse.class)
                        .fromJson(changesString);

        assertThat(wrappersObserver.getDatabaseRevision(), is(snapshotResponse.getRevision()));
    }

    @Test
    public void testSecondCallForCollection() throws IOException, InterruptedException {
        enqueueDatabaseInfoRequest(DATABASE_INFO);
        enqueueSnapshotRequest();
        final DatabaseDto databaseInfoUpdated = enqueueDatabaseInfoRequest(DATABASE_INFO_UPDATED);
        enqueueChangesRequest();

        dataSyncManager.requestCollection(MOCK_CONTEXT, MOCK_DATABASE_ID, MOCK_COLLECTION_ID);

        dataSyncManager.requestCollection(MOCK_CONTEXT, MOCK_DATABASE_ID, MOCK_COLLECTION_ID);

        final RecordedRequest databaseInfoRequest = mockWebServer.takeRequest(1, SECONDS);
        final RecordedRequest snapshotRequest = mockWebServer.takeRequest(1, SECONDS);

        final RecordedRequest databaseInfo2 = mockWebServer.takeRequest(1, SECONDS);
        final RecordedRequest changesRequest = mockWebServer.takeRequest(1, SECONDS);

        assertNotNull(databaseInfoRequest);
        assertNotNull(snapshotRequest);
        assertNotNull(databaseInfo2);
        assertNotNull(changesRequest);

        final String expectedPath = "/v1/data/" + MOCK_CONTEXT_STRING
                                    + "/databases/" + MOCK_DATABASE_ID + "/deltas"
                                    + "?base_revision=" + MOCK_REVISION;

        assertThat(changesRequest.getPath(), is(expectedPath));
        assertThat(changesRequest.getMethod(), is("GET"));
        assertThat(changesRequest.getBodySize(), is(0L));

        assertNull(wrappersObserver.getError());
        assertNotNull(wrappersObserver.getCollection());

        databaseInfoUpdated.setFullSnapshotSynced(false);
        final Database expectedDatabase = new Database(dataSyncManager,
                                                       MOCK_CONTEXT,
                                                       databaseInfoUpdated);
        assertThat(wrappersObserver.getDatabaseRevision(), is(databaseInfoUpdated.getRevision()));
        assertDatabase(wrappersObserver.getDatabase(), expectedDatabase);

        final String changesString = ResourcesUtil.getTextFromFile("get_changes.json");
        final DeltasResponse deltasResponse =
                new Moshi.Builder().build().adapter(DeltasResponse.class)
                        .fromJson(changesString);

        assertThat(wrappersObserver.getDatabaseRevision(), is(deltasResponse.getRevision()));
    }

    @Test
    public void testNoNetworkFirstCallSnapshot() throws IOException, InterruptedException {
        mockWebServer.shutdown();

        dataSyncManager.requestDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertNotNull(wrappersObserver.getError());
        assertNull(wrappersObserver.getSnapshot());
        assertNull(wrappersObserver.getDatabase());
    }

    @Test
    public void testNoNetworkFirstCallCollection() throws IOException, InterruptedException {
        mockWebServer.shutdown();

        dataSyncManager.requestDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertNotNull(wrappersObserver.getError());
        assertNull(wrappersObserver.getCollection());
        assertNull(wrappersObserver.getDatabase());
    }

    @Test
    public void testNoNetworkSecondCallCollection() throws IOException, InterruptedException {
        final DatabaseDto databaseInfo = enqueueDatabaseInfoRequest(DATABASE_INFO);
        enqueueSnapshotRequest();

        dataSyncManager.requestCollection(MOCK_CONTEXT, MOCK_DATABASE_ID, MOCK_COLLECTION_ID);

        mockWebServer.shutdown();

        dataSyncManager.requestCollection(MOCK_CONTEXT, MOCK_DATABASE_ID, MOCK_COLLECTION_ID);

        assertNotNull(wrappersObserver.getError());
        assertNotNull(wrappersObserver.getCollection());
        assertThat(wrappersObserver.getDatabaseRevision(), is(databaseInfo.getRevision()));
    }

    @Test
    public void testNoNetworkSecondCallSnapshot() throws IOException, InterruptedException {
        final DatabaseDto databaseInfo = enqueueDatabaseInfoRequest(DATABASE_INFO);
        enqueueSnapshotRequest();

        dataSyncManager.requestDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        mockWebServer.shutdown();

        dataSyncManager.requestDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertNotNull(wrappersObserver.getError());
        assertNotNull(wrappersObserver.getSnapshot());
        assertThat(wrappersObserver.getDatabaseRevision(), is(databaseInfo.getRevision()));
    }

    @Test
    public void testCollectionGone() throws IOException, InterruptedException {
        enqueueDatabaseInfoRequest(DATABASE_INFO);
        enqueueSnapshotRequest();
        final DatabaseDto databaseInfoUpdated = enqueueDatabaseInfoRequest(DATABASE_INFO_UPDATED);
        enqueueGoneRequest();
        enqueueSnapshotRequest();

        dataSyncManager.requestCollection(MOCK_CONTEXT, MOCK_DATABASE_ID, MOCK_COLLECTION_ID);
        dataSyncManager.requestCollection(MOCK_CONTEXT, MOCK_DATABASE_ID, MOCK_COLLECTION_ID);

        final RecordedRequest databaseInfoRequest = mockWebServer.takeRequest(1, SECONDS);
        final RecordedRequest snapshotRequest = mockWebServer.takeRequest(1, SECONDS);
        final RecordedRequest databaseInfo2 = mockWebServer.takeRequest(1, SECONDS);
        final RecordedRequest goneRequest = mockWebServer.takeRequest(1, SECONDS);
        final RecordedRequest snapshotRequest2 = mockWebServer.takeRequest(1, SECONDS);

        assertNotNull(databaseInfoRequest);
        assertNotNull(snapshotRequest);
        assertNotNull(databaseInfo2);
        assertNotNull(goneRequest);
        assertNotNull(snapshotRequest2);

        databaseInfoUpdated.setFullSnapshotSynced(false);
        final Database expectedDatabase = new Database(dataSyncManager,
                                                       MOCK_CONTEXT,
                                                       databaseInfoUpdated);
        assertDatabase(wrappersObserver.getDatabase(), expectedDatabase);
        assertThat(wrappersObserver.getDatabaseRevision(), is(databaseInfoUpdated.getRevision()));

        final String expectedPath = "/v1/data/" + MOCK_CONTEXT_STRING
                                    + "/databases/" + MOCK_DATABASE_ID + "/snapshot?collection_id=";

        assertThat(snapshotRequest2.getPath(), startsWith(expectedPath));
        assertThat(snapshotRequest2.getMethod(), is("GET"));
        assertThat(snapshotRequest2.getBodySize(), is(0L));

        assertNull(wrappersObserver.getError());
        assertNotNull(wrappersObserver.getCollection());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testRevisionFirstCall() throws Exception {
        enqueueDatabaseInfoRequest(DATABASE_INFO);
        final SnapshotResponse expectedSnapshot = enqueueSnapshotRequest();

        dataSyncManager.requestDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertNotNull(wrappersObserver.getSnapshot());
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        final Snapshot snapshot = new Snapshot(databaseManager,
                                               MOCK_CONTEXT,
                                               MOCK_DATABASE_ID,
                                               operationProcessor,
                                               expectedSnapshot);

        assertSnapshot(wrappersObserver.getSnapshot(), snapshot);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testRevisionSecondCall() throws Exception {
        enqueueDatabaseInfoRequest(DATABASE_INFO);
        final SnapshotResponse expectedSnapshot = enqueueSnapshotRequest();
        enqueueDatabaseInfoRequest(DATABASE_INFO_UPDATED);
        final DeltasResponse deltasResponse = enqueueChangesRequest();

        dataSyncManager.requestDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        dataSyncManager.requestDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertNotNull(wrappersObserver.getSnapshot());

        applyChangesToSnapshot(expectedSnapshot, deltasResponse);

        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        final Snapshot snapshot = new Snapshot(databaseManager,
                                               MOCK_CONTEXT,
                                               MOCK_DATABASE_ID,
                                               operationProcessor,
                                               expectedSnapshot);

        assertSnapshot(wrappersObserver.getSnapshot(), snapshot);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testListInListDelta() throws Exception {
        enqueueDatabaseInfoRequest(DATABASE_INFO);
        enqueueSnapshotRequest("database_snapshot_list_in_list.json");
        enqueueDatabaseInfoRequest(DATABASE_INFO_UPDATED);
        enqueueChangesRequest("changes_list_in_list.json");

        dataSyncManager.requestDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        dataSyncManager.requestDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertNotNull(wrappersObserver.getSnapshot());
        final Snapshot snapshot = wrappersObserver.getSnapshot();
        final Collection notes = snapshot.getCollection("notes");
        final Record note = notes.getRecord("note_id");
        final Value tags = note.getValue("tags");
        final ValuesList listValue = tags.getListValue();
        assertEquals(2, listValue.size());
        assertEquals(2, listValue.getValue(0).getListValue().size());
    }

    @SuppressWarnings("ConstantConditions")
    private void applyChangesToSnapshot(@NonNull final SnapshotResponse snapshotResponse,
                                        @NonNull final DeltasResponse deltasResponse) {
        final ValueDto value0 = new ValueDto();
        value0.setType(Datatype.STRING);
        value0.setStringValue("3 p.m.");

        final ValueDto value1 = new ValueDto();
        value1.setType(Datatype.STRING);
        value1.setStringValue("4 p.m.");

        final FieldDto field0 = new FieldDto();
        field0.setFieldId("starts");
        field0.setValue(value0);

        final FieldDto field1 = new FieldDto();
        field1.setFieldId("finishes");
        field1.setValue(value1);

        final List<FieldDto> fieldList = new ArrayList<>();
        fieldList.add(field0);
        fieldList.add(field1);

        final RecordDto record = new RecordDto();
        record.setFields(fieldList);

        record.setCollectionId("sport");
        record.setRecordId("meeting");

        final List<RecordDto> recordList = snapshotResponse.getRecords().getItems();
        recordList.add(record);

        updateRevisions(snapshotResponse, deltasResponse);
    }

    @SuppressWarnings("ConstantConditions")
    private void updateRevisions(@NonNull final SnapshotResponse snapshotResponse,
                                 @NonNull final DeltasResponse deltas) {
        snapshotResponse.setRevision(deltas.getRevision());

        for (final DeltaItemDto deltaItem : deltas.getItems()) {
            final long revision = deltaItem.getRevision();
            for (final ChangesDto changes : deltaItem.getChanges()) {
                final String collectionId = changes.getCollectionId();
                final String recordId = changes.getRecordId();
                updateRevision(snapshotResponse.getRecords(), collectionId, recordId, revision);
            }
        }
    }

    private void updateRevision(@NonNull final RecordsDto recordsDto,
                                @NonNull final String collectionId,
                                @NonNull final String recordId,
                                final long revision) {
        for (final RecordDto record : recordsDto.getItems()) {
            if (collectionId.equals(record.getCollectionId())
                && recordId.equals(record.getRecordId())) {
                record.setRevision(revision);
            }
        }
    }

    private void initDataSyncManager() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        final Config.Builder builder = new Config.Builder()
                .baseUrl(mockWebServer.url("/").toString())
                .credentials(MOCK_CREDENTIALS)
                .operationProcessor(operationProcessor)
                .logLevel(LogLevel.DEBUG);

        dataSyncManager.init(builder.build());
    }

    private DeltasResponse enqueueChangesRequest() throws IOException {
        return enqueueChangesRequest("get_changes.json");
    }

    private DeltasResponse enqueueChangesRequest(String fileName) throws IOException {
        final String changesString = ResourcesUtil.getTextFromFile(fileName);

        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(changesString);
        mockResponse.setResponseCode(HttpStatusCode.OK);

        mockWebServer.enqueue(mockResponse);
        return new Moshi.Builder().build().adapter(DeltasResponse.class)
                .fromJson(changesString);
    }

    private DatabaseDto enqueueDatabaseInfoRequest(@NonNull final String fileName)
            throws IOException {
        final String databaseInfoString = ResourcesUtil.getTextFromFile(fileName);

        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(databaseInfoString);
        mockResponse.setResponseCode(HttpStatusCode.OK);

        mockWebServer.enqueue(mockResponse);

        return new Moshi.Builder().build().adapter(DatabaseDto.class)
                .fromJson(databaseInfoString);
    }

    private SnapshotResponse enqueueSnapshotRequest() throws IOException {
        return enqueueSnapshotRequest("database_snapshot.json");
    }

    private SnapshotResponse enqueueSnapshotRequest(String fileName) throws IOException {
        final String snapshotString = ResourcesUtil.getTextFromFile(fileName);

        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(snapshotString);
        mockResponse.setResponseCode(HttpStatusCode.OK);

        mockWebServer.enqueue(mockResponse);
        return new Moshi.Builder().build().adapter(SnapshotResponse.class)
                .fromJson(snapshotString);
    }

    private void enqueueGoneRequest() throws IOException {
        final String errorString = ResourcesUtil.getTextFromFile("error.json");

        final MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(errorString);
        mockResponse.setResponseCode(HttpStatusCode.GONE);

        mockWebServer.enqueue(mockResponse);
    }
}