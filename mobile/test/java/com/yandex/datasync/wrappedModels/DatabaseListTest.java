/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.wrappedModels;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.DataSyncManager;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.DatabasesResponse;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.List;

import static com.yandex.datasync.asserters.DatabaseAsserter.assertDatabase;

@SuppressWarnings("ConstantConditions")
@RunWith(RobolectricTestRunner.class)
public class DatabaseListTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final DataSyncManager dataSyncManager = null;

    @Test
    public void openDatabaseTest() throws IOException {
        final String jsonString = ResourcesUtil.getTextFromFile("get_databases_list.json");
        final DatabasesResponse databases =
                new Moshi.Builder().build().adapter(DatabasesResponse.class)
                        .fromJson(jsonString);

        final DatabaseList databaseListWrapper =
                new DatabaseList(dataSyncManager, MOCK_CONTEXT, databases.getDatabaseList());

        final List<DatabaseDto> databaseList = databases.getDatabaseList();

        for (int i = 0; i < databaseList.size(); i++) {
            final DatabaseDto database = databaseList.get(i);
            final Database expected = new Database(dataSyncManager, MOCK_CONTEXT, database);

            final Database actual = databaseListWrapper.getDatabase(i);

            assertDatabase(actual, expected);
        }
    }
}