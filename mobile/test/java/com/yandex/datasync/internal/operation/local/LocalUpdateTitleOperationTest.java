/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.local;

import android.database.Cursor;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.DatabasesRepository;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.DatabaseChangeType;
import com.yandex.datasync.internal.model.response.DatabasesResponse;
import com.yandex.datasync.internal.operation.Operation;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Databases.Rows.SYNCED;
import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Databases.TABLE_NAME;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class LocalUpdateTitleOperationTest {

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String NEW_MOCK_DATABASE_TITLE = "new_user_schedule_title";

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    @Mock
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

        final Operation operation = new LocalUpdateTitleOperation(MOCK_CONTEXT,
                                                                  MOCK_DATABASE_ID,
                                                                  NEW_MOCK_DATABASE_TITLE,
                                                                  databaseManager);
        operation.run();

        databases.getDatabaseList().remove(0);

        final String selection = SYNCED + "=?";
        final String selectionArgs[] = {DatabaseChangeType.UPDATE.name()};

        try (final Cursor cursor = databaseWrapper.query(TABLE_NAME, selection, selectionArgs)) {
            assertThat(cursor.getCount(), is(1));
        }
    }
}