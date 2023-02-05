/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.network.sync;

import android.content.ContentValues;
import android.database.Cursor;
import androidx.annotation.NonNull;

import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.api.Api;
import com.yandex.datasync.internal.api.exceptions.BaseException;
import com.yandex.datasync.internal.api.exceptions.NetworkException;
import com.yandex.datasync.internal.api.exceptions.NotSyncedException;
import com.yandex.datasync.internal.api.exceptions.http.NotFoundException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.DatabasesRepository;
import com.yandex.datasync.internal.database.repository.SnapshotRepository;
import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.DatabaseChangeType;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.observer.RawDataObserver;
import com.yandex.datasync.internal.util.DateFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;

import static com.yandex.datasync.asserters.DatabaseDtoAsserter.assertDatabase;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class DeleteDatabaseSyncStrategyTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_USER_ID = "mock_user_id";

    private DatabaseManager databaseManager;

    @Mock
    private Api api;

    @Mock
    private RawDataObserver observable;

    @Before
    public void setUp() {
        initMocks(this);
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
    }

    @Test
    public void testNoNetwork() throws BaseException {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        assertNotNull(getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID));

        when(api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID))
                .thenThrow(new NetworkException("network fail"));

        final SyncStrategy strategy = new DeleteDatabaseSyncStrategy(MOCK_CONTEXT,
                                                                     MOCK_DATABASE_ID,
                                                                     api,
                                                                     localDatabaseInfo,
                                                                     databaseManager);
        try {
            strategy.sync();
            fail("NetworkException is expected");
        } catch (final NetworkException exception) {
            final SQLiteDatabaseWrapper databaseWrapper =
                    databaseManager.openDatabaseWrapped(MOCK_CONTEXT);
            try (
                    Cursor cursor =
                            databaseWrapper.query(DatabaseDescriptor.Databases.TABLE_NAME, null,
                                                  null)
            ) {
                assertTrue(cursor.moveToFirst());
            }

            assertTrue(databaseManager.hasDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID));
        }
    }

    @Test
    public void testNotFoundViaRepository() throws BaseException {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        assertNotNull(getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID));

        when(api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID))
                .thenThrow(new NotFoundException("resource not found"));

        final SyncStrategy strategy = new DeleteDatabaseSyncStrategy(MOCK_CONTEXT,
                                                                     MOCK_DATABASE_ID,
                                                                     api,
                                                                     localDatabaseInfo,
                                                                     databaseManager);
        final DatabaseDto actualDatabaseInfo = strategy.sync();

        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(MOCK_CONTEXT);
        try (
                Cursor cursor =
                        databaseWrapper.query(DatabaseDescriptor.Databases.TABLE_NAME, null, null)
        ) {
            assertFalse(cursor.moveToFirst());
        }

        assertFalse(databaseManager.hasDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID));
        assertDatabase(actualDatabaseInfo, localDatabaseInfo);
    }

    @Test
    public void testDeleteFromApi() throws BaseException {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        assertNotNull(getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID));

        when(api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID)).thenReturn(true);

        final SyncStrategy strategy = new DeleteDatabaseSyncStrategy(MOCK_CONTEXT,
                                                                     MOCK_DATABASE_ID,
                                                                     api,
                                                                     localDatabaseInfo,
                                                                     databaseManager);
        final DatabaseDto actualDatabaseInfo = strategy.sync();

        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(MOCK_CONTEXT);
        try (
                Cursor cursor =
                        databaseWrapper.query(DatabaseDescriptor.Databases.TABLE_NAME, null, null)
        ) {
            assertFalse(cursor.moveToFirst());
        }

        assertFalse(databaseManager.hasDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID));
        assertDatabase(actualDatabaseInfo, localDatabaseInfo);
    }

    @Test(expected = NotSyncedException.class)
    public void testNotSynced() throws BaseException {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);

        when(api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID)).thenReturn(true);

        final SyncStrategy strategy = new DeleteDatabaseSyncStrategy(MOCK_CONTEXT,
                                                                     MOCK_DATABASE_ID,
                                                                     api,
                                                                     localDatabaseInfo,
                                                                     databaseManager);
        final DatabaseDto actualDatabaseInfo = strategy.sync();
        assertNotNull(actualDatabaseInfo);

        assertNotNull(getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID));
    }

    private SnapshotResponse getSnapshot(@NonNull final YDSContext databaseContext,
                                         @NonNull final String databaseId) throws BaseException {
        return new SnapshotRepository(databaseManager, databaseContext, databaseId).get();
    }

    private void fillDatabase(@NonNull final YDSContext databaseContext,
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
        values.put(DatabaseDescriptor.Databases.Rows.SYNCED, DatabaseChangeType.DELETE.name());
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