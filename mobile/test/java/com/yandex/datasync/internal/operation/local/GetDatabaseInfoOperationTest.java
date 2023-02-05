/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.local;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.api.exceptions.http.NotFoundException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.DatabasesRepository;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.operation.Operation;
import com.yandex.datasync.internal.operation.network.MockRawDataObserver;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class GetDatabaseInfoOperationTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_USER_ID = "mock_user_id";

    private DatabaseManager databaseManager;

    private MockRawDataObserver mockOberverable;

    @Before
    public void setUp() {
        initMocks(this);
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
        mockOberverable = spy(new MockRawDataObserver());
    }

    @Test
    public void testGetDatabaseInfo() throws IOException {
        fillDatabaseInfo();
        final Operation operation = new GetDatabaseInfoOperation(MOCK_CONTEXT,
                                                                 MOCK_DATABASE_ID,
                                                                 databaseManager,
                                                                 mockOberverable);
        operation.run();
        final InOrder order = inOrder(mockOberverable);
        order.verify(mockOberverable).notifyDatabaseInfoRetrieved(eq(MOCK_CONTEXT), any());
        order.verifyNoMoreInteractions();

        final DatabaseDto actual = mockOberverable.getDatabase();
        assertThat(actual.getDatabaseId(), is(MOCK_DATABASE_ID));
    }

    @Test
    public void testDatabaseNotExists() {
        final Operation operation = new GetDatabaseInfoOperation(MOCK_CONTEXT,
                                                                 MOCK_DATABASE_ID,
                                                                 databaseManager,
                                                                 mockOberverable);
        operation.run();

        final InOrder order = inOrder(mockOberverable);
        order.verify(mockOberverable).notifyError(isA(NotFoundException.class));
        order.verifyNoMoreInteractions();
    }

    private void fillDatabaseInfo() throws IOException {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(MOCK_CONTEXT);
        final DatabasesRepository changesRepository = new DatabasesRepository(databaseWrapper);
        final String databaseInfo = ResourcesUtil.getTextFromFile("get_database_info.json");
        final DatabaseDto databaseDto =
                new Moshi.Builder().build().adapter(DatabaseDto.class).fromJson(databaseInfo);
        changesRepository.save(databaseDto);
    }
}
