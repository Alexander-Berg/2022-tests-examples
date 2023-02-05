/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.local;

import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.operation.Operation;
import com.yandex.datasync.internal.operation.network.MockRawDataObserver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class ResetDatabaseOperationTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "mock_database_id";

    private static final String MOCK_USER_ID = "mock_user_id";

    private MockRawDataObserver rawDataObserver = new MockRawDataObserver();

    private DatabaseManager databaseManager;

    @Before
    public void setUp() {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
        rawDataObserver = new MockRawDataObserver();
    }

    @Test
    public void testResetDatabase() {
        final Operation operation = new ResetDatabaseOperation(MOCK_CONTEXT,
                                                               MOCK_DATABASE_ID,
                                                               databaseManager,
                                                               rawDataObserver);
        operation.run();

        assertThat(rawDataObserver.getDatabaseContext(), is(MOCK_CONTEXT));
        assertThat(rawDataObserver.getDatabaseId(), is(MOCK_DATABASE_ID));
    }
}