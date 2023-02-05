/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.network.sync;

import android.content.ContentValues;
import androidx.annotation.NonNull;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.AutoCreateStrategy;
import com.yandex.datasync.MergeAtomSize;
import com.yandex.datasync.MergeWinner;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.editor.RecordEditor;
import com.yandex.datasync.internal.api.Api;
import com.yandex.datasync.internal.api.exceptions.BaseException;
import com.yandex.datasync.internal.api.exceptions.DatabaseAlreadyExistsException;
import com.yandex.datasync.internal.api.exceptions.NetworkException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.DatabasesRepository;
import com.yandex.datasync.internal.database.repository.SnapshotRepository;
import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.database.sql.cursor.FieldCursor;
import com.yandex.datasync.internal.database.sql.cursor.RecordCursor;
import com.yandex.datasync.internal.database.sql.cursor.ValueCursor;
import com.yandex.datasync.internal.model.DatabaseChangeType;
import com.yandex.datasync.internal.model.response.ApplyChangesResponse;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.observer.RawDataObserver;
import com.yandex.datasync.internal.operation.ImmediatelyOperationProcessor;
import com.yandex.datasync.internal.operation.OperationProcessor;
import com.yandex.datasync.internal.util.DateFormat;
import com.yandex.datasync.wrappedModels.Snapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.yandex.datasync.util.ResourcesUtil.getTextFromFile;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class FirstOutgoingSyncStrategyTest {

    @ParameterizedRobolectricTestRunner.Parameters(name = "MergeWinner = {0}, MergeAtomSize = {1}")
    public static List<Object[]> data() {
        return Arrays.asList(new Object[][]{

                {MergeWinner.THEIRS.name(), MergeAtomSize.COLLECTION.name()},
                {MergeWinner.THEIRS.name(), MergeAtomSize.RECORD.name()},
                {MergeWinner.THEIRS.name(), MergeAtomSize.VALUE.name()},

                {MergeWinner.MINE.name(), MergeAtomSize.COLLECTION.name()},
                {MergeWinner.MINE.name(), MergeAtomSize.RECORD.name()},
                {MergeWinner.MINE.name(), MergeAtomSize.VALUE.name()}
        });
    }

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_DATABASE_ID_2 = "user_schedule2";

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_COLLECTION_ID = "mock_collection_id";

    private static final String MOCK_RECORD_ID = "monday";

    public static final String MOCK_FILED_ID_1 = "starts";

    public static final String MOCK_FIELD_ID_2 = "ends";

    public static final String MOCK_FIELD_1_VALUE = "10 p.m.";

    public static final String MOCK_FIELD_2_VALUE = "12 p.m.";

    public static final long MOCK_REVISION = 1L;

    private DatabaseManager databaseManager;

    @Mock
    private Api api;

    @Mock
    private RawDataObserver observable;

    private final OperationProcessor processor = new ImmediatelyOperationProcessor();

    private final MergeAtomSize mergeAtomSize;

    private final MergeWinner mergeWinner;

    public FirstOutgoingSyncStrategyTest(@NonNull final String mergeWinnerName,
                                         @NonNull final String mergeAtomSizeName) {
        this.mergeWinner = MergeWinner.valueOf(mergeWinnerName);
        this.mergeAtomSize = MergeAtomSize.valueOf(mergeAtomSizeName);
    }

    @Before
    public void setUp() {
        initMocks(this);
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
    }

    @Test(expected = NetworkException.class)
    public void testNoNetwork() throws BaseException {
        fillDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);

        when(api.createDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID))
                .thenThrow(new NetworkException("network fail"));

        final SyncStrategy strategy = new FirstOutgoingSyncStrategy(mergeWinner,
                                                                    mergeAtomSize,
                                                                    MOCK_CONTEXT,
                                                                    MOCK_DATABASE_ID,
                                                                    api,
                                                                    databaseManager,
                                                                    localDatabaseInfo,
                                                                    AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES
        );
        strategy.sync();
    }

    @Test(expected = NetworkException.class)
    public void testNoNetworkOnPostChanges() throws BaseException, IOException {
        fillDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        addChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);

        when(api.createDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID))
                .thenReturn(getDatabaseInfoFromFile("get_database_info.json"));
        when(api.postChanges(eq(MOCK_CONTEXT), eq(MOCK_DATABASE_ID), anyLong(), any()))
                .thenThrow(new NetworkException("network fail"));

        final SyncStrategy strategy = new FirstOutgoingSyncStrategy(mergeWinner,
                                                                    mergeAtomSize,
                                                                    MOCK_CONTEXT,
                                                                    MOCK_DATABASE_ID,
                                                                    api,
                                                                    databaseManager,
                                                                    localDatabaseInfo,
                                                                    AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES
        );
        strategy.sync();
    }

    @Test
    public void testDatabaseValuesOnInsertNewDatabase() throws Exception {
        fillDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        addChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final ApplyChangesResponse applyChangesResponse =
                getApplyChangesResponse("post_changes.json", MOCK_REVISION);

        when(api.createDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID))
                .thenReturn(localDatabaseInfo);
        when(api.postChanges(eq(MOCK_CONTEXT), eq(MOCK_DATABASE_ID), eq(0L), any()))
                .thenReturn(applyChangesResponse);

        assertRecordsNotSynced(MOCK_CONTEXT, MOCK_DATABASE_ID);
        assertRecordsRevision(MOCK_CONTEXT, MOCK_DATABASE_ID, 0L);
        assertFieldsNotSynced(MOCK_CONTEXT, MOCK_DATABASE_ID);
        assertValuesNotSynced(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final SyncStrategy strategy = new FirstOutgoingSyncStrategy(mergeWinner,
                                                                    mergeAtomSize,
                                                                    MOCK_CONTEXT,
                                                                    MOCK_DATABASE_ID,
                                                                    api,
                                                                    databaseManager,
                                                                    localDatabaseInfo,
                                                                    AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES
        );
        strategy.sync();

        assertRecordsMissSynced(MOCK_CONTEXT, MOCK_DATABASE_ID);
        assertRecordsRevision(MOCK_CONTEXT, MOCK_DATABASE_ID, MOCK_REVISION);
        assertFieldsMissSynced(MOCK_CONTEXT, MOCK_DATABASE_ID);
        assertValuesMissSynced(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test
    public void testOrderOnInsertNewDatabase() throws BaseException, IOException {
        fillDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        addChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final ApplyChangesResponse applyChangesResponse =
                getApplyChangesResponse("post_changes.json", MOCK_REVISION);

        when(api.createDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID))
                .thenReturn(localDatabaseInfo);
        when(api.postChanges(eq(MOCK_CONTEXT), eq(MOCK_DATABASE_ID), eq(0L), any()))
                .thenReturn(applyChangesResponse);

        final SyncStrategy strategy = new FirstOutgoingSyncStrategy(mergeWinner,
                                                                    mergeAtomSize,
                                                                    MOCK_CONTEXT,
                                                                    MOCK_DATABASE_ID,
                                                                    api,
                                                                    databaseManager,
                                                                    localDatabaseInfo,
                                                                    AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES
        );
        strategy.sync();

        final InOrder order = inOrder(api);
        order.verify(api).createDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        order.verify(api).postChanges(eq(MOCK_CONTEXT), eq(MOCK_DATABASE_ID), eq(0L), any());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testOrderOnDatabaseAlreadyExists() throws Exception {
        fillDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        addChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info.json");

        final ApplyChangesResponse applyChangesResponse =
                getApplyChangesResponse("post_changes.json", MOCK_REVISION);

        final SnapshotResponse snapshot = getSnapshotResponseFromFile("database_snapshot.json");

        when(api.createDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID))
                .thenThrow(new DatabaseAlreadyExistsException("database already exists"));
        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID))
                .thenReturn(snapshot);
        when(api.postChanges(eq(MOCK_CONTEXT),
                             eq(MOCK_DATABASE_ID),
                             eq(snapshot.getRevision()),
                             any()))
                .thenReturn(applyChangesResponse);

        final SyncStrategy strategy = new FirstOutgoingSyncStrategy(mergeWinner,
                                                                    mergeAtomSize,
                                                                    MOCK_CONTEXT,
                                                                    MOCK_DATABASE_ID,
                                                                    api,
                                                                    databaseManager,
                                                                    localDatabaseInfo,
                                                                    AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES
        );
        strategy.sync();

        final InOrder order = inOrder(api);
        order.verify(api).createDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        order.verify(api).postChanges(eq(MOCK_CONTEXT),
                                      eq(MOCK_DATABASE_ID),
                                      eq(snapshot.getRevision()),
                                      any());
        order.verifyNoMoreInteractions();
    }

    private void assertRecordsNotSynced(@NonNull final YDSContext databaseContext,
                                        @NonNull final String databaseId) {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(databaseContext, databaseId);

        final String recordSelections =
                DatabaseDescriptor.Record.Rows.INTERNAL_CHANGE_TYPE + " is not null";
        final String recordSelectionArgs[] = null;

        try (
                RecordCursor recordCursor = new RecordCursor(
                        databaseWrapper.query(DatabaseDescriptor.Record.TABLE_NAME,
                                              recordSelections,
                                              recordSelectionArgs))
        ) {
            assertTrue(recordCursor.moveToFirst());
        }
    }

    private void assertRecordsMissSynced(@NonNull final YDSContext databaseContext,
                                         @NonNull final String databaseId) {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(databaseContext, databaseId);

        final String recordSelections =
                DatabaseDescriptor.Record.Rows.INTERNAL_CHANGE_TYPE + " is not null";
        final String recordSelectionArgs[] = null;

        try (
                RecordCursor recordCursor = new RecordCursor(
                        databaseWrapper.query(DatabaseDescriptor.Record.TABLE_NAME,
                                              recordSelections,
                                              recordSelectionArgs))
        ) {
            assertFalse(recordCursor.moveToFirst());
        }
    }

    private void assertRecordsRevision(@NonNull final YDSContext databaseContext,
                                       @NonNull final String databaseId,
                                       final long expectedRevision) {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(databaseContext, databaseId);
        try (
                RecordCursor recordCursor = new RecordCursor(
                        databaseWrapper.query(DatabaseDescriptor.Record.TABLE_NAME,
                                              null,
                                              null))
        ) {
            assertTrue(recordCursor.moveToFirst());
            do {
                assertThat(recordCursor.getRevision(), is(expectedRevision));
            } while (recordCursor.moveToNext());
        }
    }

    private void assertValuesNotSynced(@NonNull final YDSContext databaseContext,
                                       @NonNull final String databaseId) {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(databaseContext, databaseId);

        final String recordSelections =
                DatabaseDescriptor.Value.Rows.INTERNAL_CHANGE_TYPE + " is not null";
        final String recordSelectionArgs[] = null;

        try (
                ValueCursor recordCursor = new ValueCursor(
                        databaseWrapper.query(DatabaseDescriptor.Value.TABLE_NAME,
                                              recordSelections,
                                              recordSelectionArgs))
        ) {
            assertTrue(recordCursor.moveToFirst());
        }
    }

    private void assertFieldsMissSynced(@NonNull final YDSContext databaseContext,
                                        @NonNull final String databaseId) {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(databaseContext, databaseId);

        final String recordSelections =
                DatabaseDescriptor.Field.Rows.INTERNAL_CHANGE_TYPE + " is not null";
        final String recordSelectionArgs[] = null;

        try (
                FieldCursor recordCursor = new FieldCursor(
                        databaseWrapper.query(DatabaseDescriptor.Field.TABLE_NAME,
                                              recordSelections,
                                              recordSelectionArgs))
        ) {
            assertFalse(recordCursor.moveToFirst());
        }
    }

    private void assertFieldsNotSynced(@NonNull final YDSContext databaseContext,
                                       @NonNull final String databaseId) {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(databaseContext, databaseId);

        final String recordSelections =
                DatabaseDescriptor.Field.Rows.INTERNAL_CHANGE_TYPE + " is not null";
        final String recordSelectionArgs[] = null;

        try (
                FieldCursor recordCursor = new FieldCursor(
                        databaseWrapper.query(DatabaseDescriptor.Field.TABLE_NAME,
                                              recordSelections,
                                              recordSelectionArgs))
        ) {
            assertTrue(recordCursor.moveToFirst());
        }
    }

    private void assertValuesMissSynced(@NonNull final YDSContext databaseContext,
                                        @NonNull final String databaseId) {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(databaseContext, databaseId);

        final String recordSelections =
                DatabaseDescriptor.Value.Rows.INTERNAL_CHANGE_TYPE + " is not null";
        final String recordSelectionArgs[] = null;

        try (
                ValueCursor recordCursor = new ValueCursor(
                        databaseWrapper.query(DatabaseDescriptor.Value.TABLE_NAME,
                                              recordSelections,
                                              recordSelectionArgs))
        ) {
            assertFalse(recordCursor.moveToFirst());
        }
    }

    private void addChanges(@NonNull final YDSContext databaseContext,
                            @NonNull final String databaseId) throws BaseException {
        final Snapshot snapshot = getSnapshot(databaseContext, databaseId);
        final RecordEditor recordEditor =
                snapshot.edit()
                        .addCollection(MOCK_COLLECTION_ID)
                        .addRecord(MOCK_RECORD_ID);

        recordEditor.addField(MOCK_FILED_ID_1).putStringValue(MOCK_FIELD_1_VALUE);
        recordEditor.addField(MOCK_FIELD_ID_2).putStringValue(MOCK_FIELD_2_VALUE);
        recordEditor.commit();
    }

    private void editChanges(@NonNull final YDSContext databaseContext,
                             @NonNull final String databaseId) throws BaseException {
        final Snapshot snapshot = getSnapshot(databaseContext, databaseId);
        final RecordEditor recordEditor =
                snapshot.edit()
                        .editCollection(MOCK_COLLECTION_ID)
                        .editRecord(MOCK_RECORD_ID);

        recordEditor.addField(MOCK_FILED_ID_1).putStringValue(MOCK_FIELD_1_VALUE);
        recordEditor.addField(MOCK_FIELD_ID_2).putStringValue(MOCK_FIELD_2_VALUE);
        recordEditor.commit();
    }

    private Snapshot getSnapshot(@NonNull final YDSContext databaseContext,
                                 @NonNull final String databaseId) throws BaseException {
        final SnapshotRepository repository = new SnapshotRepository(databaseManager,
                                                                     databaseContext,
                                                                     databaseId);
        return getSnapshot(databaseContext, databaseId, repository.get());
    }

    private SnapshotResponse getSnapshotResponseFromFile(final String fileName) throws IOException {
        return new Moshi.Builder().build().adapter(SnapshotResponse.class)
                .fromJson(getTextFromFile(fileName));
    }

    private Snapshot getSnapshot(@NonNull final YDSContext databaseContext,
                                 @NonNull final String databaseId,
                                 @NonNull final SnapshotResponse snapshot) throws BaseException {
        return new Snapshot(databaseManager,
                            databaseContext,
                            databaseId,
                            processor,
                            snapshot);
    }

    private ApplyChangesResponse getApplyChangesResponse(@NonNull final String fileName,
                                                         final long revision)
            throws IOException {
        final ApplyChangesResponse result =
                new Moshi.Builder().build().adapter(ApplyChangesResponse.class)
                        .fromJson(getTextFromFile(fileName));
        result.setRevision(revision);
        return result;
    }

    private DatabaseDto getDatabaseInfoFromFile(@NonNull final String fileName) throws IOException {
        return new Moshi.Builder().build().adapter(DatabaseDto.class)
                .fromJson(getTextFromFile(fileName));
    }

    private void fillDatabaseInfo(@NonNull final YDSContext databaseContext,
                                  @NonNull final String databaseId) {

        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(databaseContext);
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

    private DatabaseDto getDatabaseInfo(@NonNull final YDSContext databaseContext,
                                        @NonNull final String databaseId) {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(databaseContext);

        final DatabasesRepository databasesRepository = new DatabasesRepository(databaseWrapper);
        return databasesRepository.get(databaseId);
    }
}
