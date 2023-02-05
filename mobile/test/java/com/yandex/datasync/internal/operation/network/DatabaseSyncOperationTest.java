/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.network;

import android.content.ContentValues;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.datasync.AutoCreateStrategy;
import com.yandex.datasync.MergeAtomSize;
import com.yandex.datasync.MergeWinner;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.api.Api;
import com.yandex.datasync.internal.api.exceptions.BaseException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.DatabaseChangeType;
import com.yandex.datasync.internal.observer.RawDataObserver;
import com.yandex.datasync.internal.operation.network.sync.DeleteDatabaseSyncStrategy;
import com.yandex.datasync.internal.operation.network.sync.FirstIncomingSyncStrategy;
import com.yandex.datasync.internal.operation.network.sync.FirstOutgoingSyncStrategy;
import com.yandex.datasync.internal.operation.network.sync.SecondSyncStrategy;
import com.yandex.datasync.internal.util.DateFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class DatabaseSyncOperationTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_COLLECTION_ID = "sport";

    private static final String MOCK_COLLECTION_ID_2 = "sport_2";

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
    public void testDeleteDatabaseStrategy() throws BaseException {
        deleteNewDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final DatabaseSyncOperation operation = spy(new DatabaseSyncOperation(MergeWinner.MINE,
                                                                              MergeAtomSize.VALUE,
                                                                              MOCK_CONTEXT,
                                                                              MOCK_DATABASE_ID,
                                                                              databaseManager,
                                                                              api,
                                                                              observable,
                                                                              AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES));
        doNothing().when(operation).sync(any());

        operation.run();

        verify(operation).sync(isA(DeleteDatabaseSyncStrategy.class));
    }

    @Test
    public void testInsertDatabaseStrategy() throws BaseException {
        insertNewDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final DatabaseSyncOperation operation = spy(new DatabaseSyncOperation(MergeWinner.MINE,
                                                                              MergeAtomSize.VALUE,
                                                                              MOCK_CONTEXT,
                                                                              MOCK_DATABASE_ID,
                                                                              databaseManager,
                                                                              api,
                                                                              observable,
                                                                              AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES));
        doNothing().when(operation).sync(any());

        operation.run();

        verify(operation).sync(isA(FirstOutgoingSyncStrategy.class));
    }

    @Test
    public void testFirstIncomingSyncStrategy() throws BaseException {
        final DatabaseSyncOperation operation = spy(new DatabaseSyncOperation(MergeWinner.MINE,
                                                                              MergeAtomSize.VALUE,
                                                                              MOCK_CONTEXT,
                                                                              MOCK_DATABASE_ID,
                                                                              databaseManager,
                                                                              api,
                                                                              observable,
                                                                              AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES));
        doNothing().when(operation).sync(any());

        operation.run();

        verify(operation).sync(isA(FirstIncomingSyncStrategy.class));
    }

    @Test
    public void testFirstIncomingSyncStrategyCollections() throws BaseException {
        final DatabaseSyncOperation operation = spy(new DatabaseSyncOperation(MergeWinner.MINE,
                                                                              MergeAtomSize.VALUE,
                                                                              MOCK_CONTEXT,
                                                                              MOCK_DATABASE_ID,
                                                                              databaseManager,
                                                                              api,
                                                                              observable,
                                                                              AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES));
        operation.includeCollection(MOCK_COLLECTION_ID);
        operation.includeCollection(MOCK_COLLECTION_ID_2);
        doNothing().when(operation).sync(any());

        operation.run();

        verify(operation).sync(isA(FirstIncomingSyncStrategy.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testFirstIncomingSyncStrategyAddCollectionAfterStart() throws BaseException {
        final DatabaseSyncOperation operation = spy(new DatabaseSyncOperation(MergeWinner.MINE,
                                                                              MergeAtomSize.VALUE,
                                                                              MOCK_CONTEXT,
                                                                              MOCK_DATABASE_ID,
                                                                              databaseManager,
                                                                              api,
                                                                              observable,
                                                                              AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES));
        doNothing().when(operation).sync(any());
        operation.run();

        operation.includeCollection(MOCK_COLLECTION_ID);
        operation.includeCollection(MOCK_COLLECTION_ID_2);
    }

    @Test
    public void testSecondSyncStrategy() throws BaseException {
        fillDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final DatabaseSyncOperation operation = spy(new DatabaseSyncOperation(MergeWinner.MINE,
                                                                              MergeAtomSize.VALUE,
                                                                              MOCK_CONTEXT,
                                                                              MOCK_DATABASE_ID,
                                                                              databaseManager,
                                                                              api,
                                                                              observable,
                                                                              AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES));
        doNothing().when(operation).sync(any());

        operation.run();

        verify(operation).sync(isA(SecondSyncStrategy.class));
    }

    @Test
    public void testSecondSyncStrategyCollections() throws BaseException {
        fillDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final DatabaseSyncOperation operation = spy(new DatabaseSyncOperation(MergeWinner.MINE,
                                                                              MergeAtomSize.VALUE,
                                                                              MOCK_CONTEXT,
                                                                              MOCK_DATABASE_ID,
                                                                              databaseManager,
                                                                              api,
                                                                              observable,
                                                                              AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES));
        operation.includeCollection(MOCK_COLLECTION_ID);
        operation.includeCollection(MOCK_COLLECTION_ID_2);
        doNothing().when(operation).sync(any());

        operation.run();

        verify(operation).sync(isA(SecondSyncStrategy.class));
    }

    private void fillDatabaseInfo(@NonNull final YDSContext databaseContext,
                                  @NonNull final String databaseId) {
        addDatabase(databaseContext, databaseId, null);
    }

    private void insertNewDatabase(@NonNull final YDSContext databaseContext,
                                   @NonNull final String databaseId) {
        addDatabase(databaseContext, databaseId, DatabaseChangeType.INSERT);
    }

    private void deleteNewDatabase(@NonNull final YDSContext databaseContext,
                                   @NonNull final String databaseId) {

        addDatabase(databaseContext, databaseId, DatabaseChangeType.DELETE);
    }

    private void addDatabase(@NonNull final YDSContext databaseContext,
                             @NonNull final String databaseId,
                             @Nullable final DatabaseChangeType databaseChangeType) {

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
        if (databaseChangeType != null) {
            values.put(DatabaseDescriptor.Databases.Rows.SYNCED, databaseChangeType.name());
        } else {
            values.putNull(DatabaseDescriptor.Databases.Rows.SYNCED);
        }
        values.put(DatabaseDescriptor.Databases.Rows.DATABASE_ID, databaseId);

        databaseWrapper.insert(DatabaseDescriptor.Databases.TABLE_NAME, values);
        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();

        //create database file
        databaseManager.openDatabaseWrapped(databaseContext, databaseId);
    }
}
