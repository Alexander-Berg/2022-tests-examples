/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.wrappedModels;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.Config;
import com.yandex.datasync.Credentials;
import com.yandex.datasync.DataSyncManager;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.operation.OperationProcessor;
import com.yandex.datasync.internal.operation.local.GetSnapshotOperation;
import com.yandex.datasync.internal.operation.local.ResetDatabaseOperation;
import com.yandex.datasync.internal.operation.network.DatabaseSyncOperation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.yandex.datasync.util.ResourcesUtil.getTextFromFile;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class DatabaseTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_TOKEN = "mock_token";

    private static final String MOCK_USER_ID = "mock_user_id";

    private DataSyncManager dataSyncManager;

    @Mock
    private OperationProcessor mockOperationProcessor;

    @Before
    public void setUp() {
        initMocks(this);
        dataSyncManager = new DataSyncManager(RuntimeEnvironment.application);
        final Config.Builder configBuilder = new Config.Builder();
        configBuilder.operationProcessor(mockOperationProcessor);
        configBuilder.credentials(new Credentials(MOCK_USER_ID, MOCK_TOKEN));
        dataSyncManager.init(configBuilder.build());
    }

    @Test
    public void testSync() throws Exception {
        final DatabaseDto databaseInfo = getDatabaseDto();
        final Database database = new Database(dataSyncManager, MOCK_CONTEXT, databaseInfo);
        database.sync();

        verify(mockOperationProcessor).run(isA(DatabaseSyncOperation.class));
    }

    @Test
    public void testReset() throws Exception {
        final DatabaseDto databaseInfo = getDatabaseDto();
        final Database database = new Database(dataSyncManager, MOCK_CONTEXT, databaseInfo);
        database.reset();

        verify(mockOperationProcessor).run(isA(ResetDatabaseOperation.class));
    }

    @Test
    public void testRequestSnapshot() throws Exception {
        final DatabaseDto databaseInfo = getDatabaseDto();
        final Database database = new Database(dataSyncManager, MOCK_CONTEXT, databaseInfo);
        database.requestSnapshot();

        verify(mockOperationProcessor).run(isA(GetSnapshotOperation.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testBrokenObjectSync() {
        final DatabaseDto databaseDto = new DatabaseDto();
        final Database database = new Database(dataSyncManager, MOCK_CONTEXT, databaseDto);
        database.sync();
    }

    @Test(expected = IllegalStateException.class)
    public void testBrokenObjectReset() {
        final DatabaseDto databaseDto = new DatabaseDto();
        final Database database = new Database(dataSyncManager, MOCK_CONTEXT, databaseDto);
        database.reset();
    }

    @Test(expected = IllegalStateException.class)
    public void testBrokenObjectRequestSnapshot() {
        final DatabaseDto databaseDto = new DatabaseDto();
        final Database database = new Database(dataSyncManager, MOCK_CONTEXT, databaseDto);
        database.requestSnapshot();
    }

    private DatabaseDto getDatabaseDto() throws Exception {
        final String databaseInfoString = getTextFromFile("get_database_info.json");
        return new Moshi.Builder().build().adapter(DatabaseDto.class)
                .fromJson(databaseInfoString);
    }
}
