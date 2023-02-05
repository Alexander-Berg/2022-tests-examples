/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.merge;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.DatabasesRepository;
import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.DatabasesResponse;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import static com.yandex.datasync.asserters.DatabasesResponseAsserter.assertDatabases;

@RunWith(RobolectricTestRunner.class)
public class DatabasesMergerTest {

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private DatabaseManager databaseManager;

    @Before
    public void setUp() {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
    }

    @Test
    public void testMergeWithRemoveOne() throws Exception {

        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(YDSContext.APP);

        final String jsonString = ResourcesUtil.getTextFromFile("get_databases_list.json");
        final DatabasesResponse expected =
                new Moshi.Builder().build().adapter(DatabasesResponse.class).fromJson(jsonString);

        final DatabasesRepository repository = new DatabasesRepository(databaseWrapper);

        databaseWrapper.beginTransaction();

        new DatabasesMerger(databaseWrapper, expected.getDatabaseList()).merge();

        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();

        assertDatabases(repository.get(), expected);

        final List<DatabaseDto> databaseList = expected.getDatabaseList();
        databaseList.remove(databaseList.size() - 1);

        databaseWrapper.beginTransaction();

        new DatabasesMerger(databaseWrapper, expected.getDatabaseList()).merge();

        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();

        assertDatabases(repository.get(), expected);
    }

    @Test
    public void testMergeWithAddOne() throws Exception {

        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(YDSContext.APP);

        final String jsonString = ResourcesUtil.getTextFromFile("get_databases_list.json");
        final DatabasesResponse expected =
                new Moshi.Builder().build().adapter(DatabasesResponse.class).fromJson(jsonString);

        final DatabasesRepository repository = new DatabasesRepository(databaseWrapper);

        databaseWrapper.beginTransaction();

        new DatabasesMerger(databaseWrapper, expected.getDatabaseList()).merge();

        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();

        assertDatabases(repository.get(), expected);

        databaseWrapper.beginTransaction();
        final String selection = DatabaseDescriptor.Databases.Rows.DATABASE_ID + "=?";
        final String selectionArgs[] = {MOCK_DATABASE_ID};
        databaseWrapper.delete(DatabaseDescriptor.Databases.TABLE_NAME, selection, selectionArgs);
        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();

        databaseWrapper.beginTransaction();

        new DatabasesMerger(databaseWrapper, expected.getDatabaseList()).merge();

        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();

        assertDatabases(repository.get(), expected);
    }

    @Test
    public void testMergeWithNoChange() throws Exception {

        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(YDSContext.APP);

        final String jsonString = ResourcesUtil.getTextFromFile("get_databases_list.json");
        final DatabasesResponse expected =
                new Moshi.Builder().build().adapter(DatabasesResponse.class).fromJson(jsonString);

        final DatabasesRepository repository = new DatabasesRepository(databaseWrapper);

        databaseWrapper.beginTransaction();

        new DatabasesMerger(databaseWrapper, expected.getDatabaseList()).merge();

        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();

        assertDatabases(repository.get(), expected);

        databaseWrapper.beginTransaction();

        new DatabasesMerger(databaseWrapper, expected.getDatabaseList()).merge();

        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();

        assertDatabases(repository.get(), expected);
    }
}