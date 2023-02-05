/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.network;

import android.content.ContentValues;
import androidx.annotation.NonNull;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.AutoCreateStrategy;
import com.yandex.datasync.MergeAtomSize;
import com.yandex.datasync.MergeWinner;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.api.Api;
import com.yandex.datasync.internal.api.exceptions.BaseException;
import com.yandex.datasync.internal.api.exceptions.NetworkException;
import com.yandex.datasync.internal.api.exceptions.http.NotFoundException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.DatabasesRepository;
import com.yandex.datasync.internal.database.repository.SnapshotRepository;
import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.DatabaseChangeType;
import com.yandex.datasync.internal.model.response.ApplyChangesResponse;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.DatabasesResponse;
import com.yandex.datasync.internal.model.response.DeltasResponse;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.operation.ImmediatelyOperationProcessor;
import com.yandex.datasync.internal.operation.Operation;
import com.yandex.datasync.internal.operation.OperationProcessor;
import com.yandex.datasync.internal.util.DateFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;

import static com.yandex.datasync.util.ResourcesUtil.getTextFromFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class FullSyncOperationTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_DATABASE_ID_2 = "user_schedule2";

    private static final String MOCK_DATABASE_ID_3 = "user_schedule3";

    private static final String MOCK_USER_ID = "mock_user_id";

    private DatabaseManager databaseManager;

    @Spy
    private final OperationProcessor mockProcessor = new ImmediatelyOperationProcessor();

    @Spy
    private final MockRawDataObserver mockObserver = new MockRawDataObserver();

    @Mock
    private Api mockApi;

    @Before
    public void setUp() {
        initMocks(this);

        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
    }

    @Test
    public void testEmptySync() throws Exception {

        enqueueDatabaseList();

        final Operation operation = new FullSyncOperation(MergeWinner.MINE,
                                                          MergeAtomSize.VALUE,
                                                          databaseManager,
                                                          mockProcessor,
                                                          mockApi,
                                                          MOCK_CONTEXT,
                                                          mockObserver,
                                                          AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES);
        operation.run();

        final InOrder order = inOrder(mockProcessor, mockObserver);

        order.verify(mockProcessor, times(0)).run(any());
        order.verify(mockObserver).notifyFullSyncSuccess(MOCK_CONTEXT);

        order.verifyNoMoreInteractions();
    }

    @Test
    public void testSyncOneDatabase() throws Exception {
        enqueueDatabaseList();

        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_DATABASE_ID);

        enqueueChanges(MOCK_DATABASE_ID);

        final Operation operation = new FullSyncOperation(MergeWinner.MINE,
                                                          MergeAtomSize.VALUE,
                                                          databaseManager,
                                                          mockProcessor,
                                                          mockApi,
                                                          MOCK_CONTEXT,
                                                          mockObserver,
                                                          AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES);
        operation.run();

        final InOrder order = inOrder(mockProcessor, mockObserver);

        order.verify(mockProcessor).run(isA(DatabaseSyncOperation.class));
        order.verify(mockObserver).notifyDatabaseSynced(eq(MOCK_CONTEXT), any());

        order.verify(mockObserver).notifyFullSyncSuccess(MOCK_CONTEXT);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testNoNetwork() throws Exception {
        enqueueDatabaseList();

        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_DATABASE_ID);

        enqueueNoNerwork(MOCK_DATABASE_ID);

        final Operation operation = new FullSyncOperation(MergeWinner.MINE,
                                                          MergeAtomSize.VALUE,
                                                          databaseManager,
                                                          mockProcessor,
                                                          mockApi,
                                                          MOCK_CONTEXT,
                                                          mockObserver,
                                                          AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES);
        operation.run();

        final InOrder order = inOrder(mockProcessor, mockObserver);

        order.verify(mockProcessor).run(isA(DatabaseSyncOperation.class));
        order.verify(mockObserver).notifyFullSyncFailed(eq(MOCK_CONTEXT));

        order.verifyNoMoreInteractions();
    }

    @Test
    public void testSyncTwoDatabases() throws Exception {
        enqueueDatabaseList();

        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_DATABASE_ID);

        fillDatabaseInfo("get_database_info_2.json");
        fillDatabase(MOCK_DATABASE_ID_2);

        enqueueChanges(MOCK_DATABASE_ID);

        enqueueChanges(MOCK_DATABASE_ID_2);

        final Operation operation = new FullSyncOperation(MergeWinner.MINE,
                                                          MergeAtomSize.VALUE,
                                                          databaseManager,
                                                          mockProcessor,
                                                          mockApi,
                                                          MOCK_CONTEXT,
                                                          mockObserver,
                                                          AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES);
        operation.run();

        final InOrder order = inOrder(mockProcessor, mockObserver);

        order.verify(mockProcessor).run(isA(DatabaseSyncOperation.class));
        order.verify(mockObserver).notifyDatabaseSynced(eq(MOCK_CONTEXT), any());

        order.verify(mockProcessor).run(isA(DatabaseSyncOperation.class));
        order.verify(mockObserver).notifyDatabaseSynced(eq(MOCK_CONTEXT), any());

        order.verify(mockObserver).notifyFullSyncSuccess(MOCK_CONTEXT);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testSyncTwoDatabasesSecondFailed() throws Exception {
        enqueueDatabaseList();

        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_DATABASE_ID);

        fillDatabaseInfo("get_database_info_2.json");
        fillDatabase(MOCK_DATABASE_ID_2);

        enqueueChanges(MOCK_DATABASE_ID);

        enqueueNoNerwork(MOCK_DATABASE_ID_2);

        final Operation operation = new FullSyncOperation(MergeWinner.MINE,
                                                          MergeAtomSize.VALUE,
                                                          databaseManager,
                                                          mockProcessor,
                                                          mockApi,
                                                          MOCK_CONTEXT,
                                                          mockObserver,
                                                          AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES);
        operation.run();

        final InOrder order = inOrder(mockProcessor, mockObserver);

        order.verify(mockProcessor).run(isA(DatabaseSyncOperation.class));
        order.verify(mockObserver).notifyDatabaseSynced(eq(MOCK_CONTEXT), any());

        order.verify(mockProcessor).run(isA(DatabaseSyncOperation.class));

        order.verify(mockObserver).notifyFullSyncFailed(MOCK_CONTEXT);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testSyncNewDatabase() throws Exception {
        enqueueDatabaseList();
        enqueueNotFound(MOCK_DATABASE_ID_3);
        insertNewDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_3);
        enqueueCreateDatabase("get_database_info_3.json", MOCK_DATABASE_ID_3);
        enqueueApplyChanges("post_changes.json", MOCK_DATABASE_ID_3);

        final Operation operation = new FullSyncOperation(MergeWinner.MINE,
                                                          MergeAtomSize.VALUE,
                                                          databaseManager,
                                                          mockProcessor,
                                                          mockApi,
                                                          MOCK_CONTEXT,
                                                          mockObserver,
                                                          AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES);
        operation.run();

        final InOrder order = inOrder(mockProcessor, mockObserver);
        order.verify(mockProcessor).run(isA(DatabaseSyncOperation.class));
        order.verify(mockObserver).notifyDatabaseSynced(eq(MOCK_CONTEXT), any());
        order.verify(mockObserver).notifyFullSyncSuccess(MOCK_CONTEXT);
        order.verifyNoMoreInteractions();

        assertThat(mockObserver.getDatabase().getDatabaseId(), is(MOCK_DATABASE_ID_3));
    }

    private void insertNewDatabase(@NonNull final YDSContext databaseContext,
                                   @NonNull final String databaseId) {

        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(MOCK_CONTEXT);
        databaseWrapper.beginTransaction();

        final ContentValues values = new ContentValues();

        final String currentDate = DateFormat.format(new Date());

        values.put(DatabaseDescriptor.Databases.Rows.CREATED, currentDate);
        values.put(DatabaseDescriptor.Databases.Rows.MODIFIED, currentDate);
        values.put(DatabaseDescriptor.Databases.Rows.FULL_SNAPSHOT_SYNCED, true);
        values.put(DatabaseDescriptor.Databases.Rows.RECORDS_COUNT, 0);
        values.put(DatabaseDescriptor.Databases.Rows.REVISION, 0);
        values.put(DatabaseDescriptor.Databases.Rows.SYNCED, DatabaseChangeType.INSERT.name());
        values.put(DatabaseDescriptor.Databases.Rows.DATABASE_ID, databaseId);

        databaseWrapper.insert(DatabaseDescriptor.Databases.TABLE_NAME, values);
        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();

        //create database file
        databaseManager.openDatabaseWrapped(databaseContext, databaseId);
    }

    private void enqueueDatabaseList() throws Exception {
        final DatabasesResponse databasesResponse =
                new Moshi.Builder().build().adapter(DatabasesResponse.class)
                        .fromJson(getTextFromFile("get_databases_list.json"));

        when(mockApi.getDatabaseList(eq(MOCK_CONTEXT), anyInt(), anyInt()))
                .thenReturn(databasesResponse);
    }

    private void enqueueChanges(@NonNull final String databaseId) throws Exception {
        final DeltasResponse deltasResponse =
                new Moshi.Builder().build().adapter(DeltasResponse.class)
                        .fromJson(getTextFromFile("get_changes.json"));

        when(mockApi.getDeltas(eq(MOCK_CONTEXT), eq(databaseId), anyInt()))
                .thenReturn(deltasResponse);
    }

    private void enqueueCreateDatabase(@NonNull final String fileName,
                                       @NonNull final String databaseId) throws Exception {
        final DatabaseDto databaseDto =
                new Moshi.Builder().build().adapter(DatabaseDto.class)
                        .fromJson(getTextFromFile(fileName));

        when(mockApi.createDatabase(eq(MOCK_CONTEXT), eq(databaseId))).thenReturn(databaseDto);
    }

    private void enqueueApplyChanges(@NonNull final String fileName,
                                     @NonNull final String databaseId) throws Exception {
        final ApplyChangesResponse databaseDto =
                new Moshi.Builder().build().adapter(ApplyChangesResponse.class)
                        .fromJson(getTextFromFile(fileName));

        when(mockApi.postChanges(eq(MOCK_CONTEXT), eq(databaseId), anyLong(), any()))
                .thenReturn(databaseDto);
    }

    private void enqueueNotFound(@NonNull final String databaseId) throws BaseException {
        when(mockApi.getDatabaseInfo(eq(MOCK_CONTEXT), eq(databaseId), eq(true)))
                .thenThrow(new NotFoundException("not found"));
    }

    private void enqueueNoNerwork(@NonNull final String databaseId) throws Exception {
        when(mockApi.getDeltas(eq(MOCK_CONTEXT), eq(databaseId), anyLong()))
                .thenThrow(new NetworkException("no network"));
    }

    private void fillDatabaseInfo(@NonNull final String fileName) throws Exception {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(MOCK_CONTEXT);
        final DatabasesRepository databasesRepository = new DatabasesRepository(databaseWrapper);

        final String databaseInfoString = getTextFromFile(fileName);
        final DatabaseDto databaseDto = new Moshi.Builder().build().adapter(DatabaseDto.class)
                .fromJson(databaseInfoString);
        databasesRepository.save(databaseDto);
    }

    private void fillDatabase(@NonNull final String databaseId) throws Exception {
        final SnapshotRepository snapshotRepository = new SnapshotRepository(databaseManager,
                                                                             MOCK_CONTEXT,
                                                                             databaseId);
        final String snapshotString = getTextFromFile("database_snapshot.json");
        final SnapshotResponse snapshotResponse =
                new Moshi.Builder().build().adapter(SnapshotResponse.class)
                        .fromJson(snapshotString);
        snapshotRepository.save(snapshotResponse);
    }
}
