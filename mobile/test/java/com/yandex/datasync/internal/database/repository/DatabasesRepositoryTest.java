/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.database.repository;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.DatabasesResponse;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import static com.yandex.datasync.asserters.DatabaseInfoDtoAsserter.assertDatabaseInfo;
import static com.yandex.datasync.asserters.DatabasesResponseAsserter.assertDatabases;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class DatabasesRepositoryTest {

    private static final String MOCK_USER_ID = "mock_user_id";

    private DatabasesRepository databasesRepository;

    @Before
    public void setUp() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(YDSContext.APP);

        databasesRepository = new DatabasesRepository(databaseWrapper);
    }

    @Test
    public void testGet() throws Exception {
        final String jsonString = ResourcesUtil.getTextFromFile("get_databases_list.json");
        final DatabasesResponse databases =
                new Moshi.Builder().build().adapter(DatabasesResponse.class).fromJson(jsonString);

        databasesRepository.save(databases);

        final DatabasesResponse databasesFromRepository = databasesRepository.get();

        assertDatabases(databasesFromRepository, databases);
    }

    @Test
    public void testGetOneAlreadyExisting() throws IOException {
        final String jsonString = ResourcesUtil.getTextFromFile("get_database_info.json");
        final DatabaseDto databaseDto =
                new Moshi.Builder().build().adapter(DatabaseDto.class).fromJson(jsonString);

        databasesRepository.save(databaseDto);

        assertNotNull(databaseDto.getDatabaseId());
        assertDatabaseInfo(databasesRepository.get(databaseDto.getDatabaseId()), databaseDto);
    }

    @Test
    public void testGetOneFirstTime() throws IOException {
        final String jsonString = ResourcesUtil.getTextFromFile("get_database_info.json");
        final DatabaseDto databaseDto =
                new Moshi.Builder().build().adapter(DatabaseDto.class).fromJson(jsonString);
        databasesRepository.save(databaseDto);

        final String jsonString2 = ResourcesUtil.getTextFromFile("get_database_info_2.json");
        final DatabaseDto databaseDto2 =
                new Moshi.Builder().build().adapter(DatabaseDto.class).fromJson(jsonString2);
        databasesRepository.save(databaseDto2);

        assertNotNull(databaseDto2.getDatabaseId());
        assertDatabaseInfo(databasesRepository.get(databaseDto2.getDatabaseId()), databaseDto2);
    }
}