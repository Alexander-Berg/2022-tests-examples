/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.local;

import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.api.exceptions.DatabaseAlreadyExistsException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.operation.Operation;
import com.yandex.datasync.internal.operation.network.MockRawDataObserver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class CreateDatabaseOperationTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "mock_database_id";

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
    public void testCreateDatabase() {
        final Operation operation = new CreateDatabaseOperation(MOCK_CONTEXT,
                                                                MOCK_DATABASE_ID,
                                                                databaseManager,
                                                                mockOberverable);
        operation.run();
        verify(mockOberverable).notifyDatabaseCreated(eq(MOCK_CONTEXT), any());

        final DatabaseDto actual = mockOberverable.getDatabase();
        assertThat(actual.getDatabaseId(), is(MOCK_DATABASE_ID));
        assertThat(actual.getRevision(), is(0L));
    }

    @Test
    public void testAlreadyExists() {
        final Operation operation = new CreateDatabaseOperation(MOCK_CONTEXT,
                                                                MOCK_DATABASE_ID,
                                                                databaseManager,
                                                                mockOberverable);
        operation.run();
        operation.run();

        final InOrder order = inOrder(mockOberverable);
        order.verify(mockOberverable).notifyDatabaseCreated(eq(MOCK_CONTEXT), any());
        order.verify(mockOberverable).notifyError(isA(DatabaseAlreadyExistsException.class));
        order.verifyNoMoreInteractions();
    }
}
