/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync;

import com.yandex.datasync.internal.api.exceptions.BaseException;
import com.yandex.datasync.internal.operation.ImmediatelyOperationProcessor;
import com.yandex.datasync.internal.operation.OperationProcessor;
import com.yandex.datasync.internal.operation.RequestSnapshotOperation;
import com.yandex.datasync.internal.operation.local.CreateDatabaseOperation;
import com.yandex.datasync.internal.operation.local.GetCollectionOperation;
import com.yandex.datasync.internal.operation.local.GetDatabaseInfoOperation;
import com.yandex.datasync.internal.operation.local.GetDatabasesListOperation;
import com.yandex.datasync.internal.operation.local.GetSnapshotOperation;
import com.yandex.datasync.internal.operation.local.ResetCollectionOperation;
import com.yandex.datasync.internal.operation.local.ResetDatabaseOperation;
import com.yandex.datasync.internal.operation.network.DatabaseSyncOperation;
import com.yandex.datasync.internal.operation.network.FullSyncOperation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class DataSyncManagerTest {

    private static final String MOCK_TOKEN = "mock_token";

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final Credentials CREDENTIALS = new Credentials(MOCK_USER_ID, MOCK_TOKEN);

    private static final LogLevel MOCK_LOG_LEVEL = LogLevel.DEBUG;

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_COLLECTION_ID = "mock_collection_id";

    @Mock
    private WrappersObserver mockObserver;

    @Mock
    private OperationProcessor mockProcessor;

    private Config mockConfig;

    private final OperationProcessor immediatelyProcessor = new ImmediatelyOperationProcessor();

    @Before
    public void setUp() {
        initMocks(this);
        mockConfig = new Config.Builder()
                .operationProcessor(mockProcessor)
                .credentials(CREDENTIALS)
                .logLevel(MOCK_LOG_LEVEL)
                .build();
    }

    @Test(expected = NotInitializedException.class)
    public void testRequestDatabaseNotInitializedException() throws BaseException {
        final DataSyncManager dataSyncManager = new DataSyncManager(RuntimeEnvironment.application);
        dataSyncManager.requestDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = NotInitializedException.class)
    public void testRequestDatabaseListNotInitializedException() throws BaseException {
        final DataSyncManager dataSyncManager = new DataSyncManager(RuntimeEnvironment.application);
        dataSyncManager.requestDatabasesList(MOCK_CONTEXT);
    }

    @Test
    public void testRequestDatabasesList() throws IOException {
        final DataSyncManager dataSyncManager = new DataSyncManager(RuntimeEnvironment.application);
        dataSyncManager.init(mockConfig);
        dataSyncManager.requestDatabasesList(MOCK_CONTEXT);

        verify(mockProcessor).run(isA(GetDatabasesListOperation.class));
    }

    @Test
    public void testFullSync() {
        final DataSyncManager dataSyncManager = new DataSyncManager(RuntimeEnvironment.application);
        dataSyncManager.init(mockConfig);
        dataSyncManager.sync(MOCK_CONTEXT);

        verify(mockProcessor).run(isA(FullSyncOperation.class));
    }

    @Test
    public void testSyncDatabase() {
        final DataSyncManager dataSyncManager = new DataSyncManager(RuntimeEnvironment.application);
        dataSyncManager.init(mockConfig);
        dataSyncManager.sync(MOCK_CONTEXT, MOCK_DATABASE_ID);

        verify(mockProcessor).run(isA(DatabaseSyncOperation.class));
    }

    @Test
    public void testRequestCollection() {
        final DataSyncManager dataSyncManager = new DataSyncManager(RuntimeEnvironment.application);
        dataSyncManager.init(mockConfig);
        dataSyncManager.requestCollection(MOCK_CONTEXT, MOCK_DATABASE_ID, MOCK_COLLECTION_ID);

        verify(mockProcessor).run(isA(RequestSnapshotOperation.class));
    }

    @Test
    public void testRequestDatabase() {
        final DataSyncManager dataSyncManager = new DataSyncManager(RuntimeEnvironment.application);
        dataSyncManager.init(mockConfig);
        dataSyncManager.requestDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        verify(mockProcessor).run(isA(RequestSnapshotOperation.class));
    }

    @Test
    public void testResetCollection() {
        final DataSyncManager dataSyncManager = new DataSyncManager(RuntimeEnvironment.application);
        dataSyncManager.init(mockConfig);
        dataSyncManager.resetCollection(MOCK_CONTEXT, MOCK_DATABASE_ID, MOCK_COLLECTION_ID);

        verify(mockProcessor).run(isA(ResetCollectionOperation.class));
    }

    @Test
    public void testResetDatabase() {
        final DataSyncManager dataSyncManager = new DataSyncManager(RuntimeEnvironment.application);
        dataSyncManager.init(mockConfig);
        dataSyncManager.resetDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        verify(mockProcessor).run(isA(ResetDatabaseOperation.class));
    }

    @Test
    public void testRequestLocalCollection() throws Exception {
        final DataSyncManager dataSyncManager = new DataSyncManager(RuntimeEnvironment.application);
        dataSyncManager.init(mockConfig);

        dataSyncManager.requestDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        dataSyncManager.requestLocalCollection(MOCK_CONTEXT, MOCK_DATABASE_ID, MOCK_COLLECTION_ID);

        verify(mockProcessor).run(isA(GetCollectionOperation.class));
    }

    @Test
    public void testRequestLocalSnapshot() {
        final DataSyncManager dataSyncManager = new DataSyncManager(RuntimeEnvironment.application);
        dataSyncManager.init(mockConfig);
        dataSyncManager.requestLocalDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        verify(mockProcessor).run(isA(GetSnapshotOperation.class));
    }

    @Test
    public void testCreateDatabase() {
        final DataSyncManager dataSyncManager = new DataSyncManager(RuntimeEnvironment.application);
        dataSyncManager.init(mockConfig);
        dataSyncManager.createDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);

        verify(mockProcessor).run(isA(CreateDatabaseOperation.class));
    }

    @Test
    public void testRequestLocalDatabaseInfo() {
        final DataSyncManager dataSyncManager = new DataSyncManager(RuntimeEnvironment.application);
        dataSyncManager.init(mockConfig);
        dataSyncManager.requestLocalDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);

        verify(mockProcessor).run(isA(GetDatabaseInfoOperation.class));
    }
}
