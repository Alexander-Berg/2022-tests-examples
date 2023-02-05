/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.local;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.api.exceptions.NotSyncedException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.DatabasesRepository;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.response.DatabasesResponse;
import com.yandex.datasync.internal.operation.network.MockRawDataObserver;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import static com.yandex.datasync.asserters.DatabaseDtoListAsserter.assertDatabasesList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class GetDatabasesListOperationTest {

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private final MockRawDataObserver observable = new MockRawDataObserver();

    private DatabaseManager databaseManager;

    private SQLiteDatabaseWrapper databaseWrapper;

    @Before
    public void setUp() {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        databaseWrapper = databaseManager.openDatabaseWrapped(MOCK_CONTEXT);
    }

    @Test
    public void testRun() throws IOException {
        final String jsonString = ResourcesUtil.getTextFromFile("get_databases_list.json");

        final DatabasesResponse databases =
                new Moshi.Builder().build().adapter(DatabasesResponse.class).fromJson(jsonString);

        final DatabasesRepository repository = new DatabasesRepository(databaseWrapper);
        repository.save(databases);

        final GetDatabasesListOperation operation = new GetDatabasesListOperation(MOCK_CONTEXT,
                                                                                  databaseManager,
                                                                                  observable);
        operation.run();

        assertDatabasesList(observable.getDatabaseList(), databases.getDatabaseList());
    }

    @Test
    public void testError() {
        final GetDatabasesListOperation operation = new GetDatabasesListOperation(MOCK_CONTEXT,
                                                                                  databaseManager,
                                                                                  observable);
        operation.run();

        assertThat(observable.getException(), instanceOf(NotSyncedException.class));
    }
}