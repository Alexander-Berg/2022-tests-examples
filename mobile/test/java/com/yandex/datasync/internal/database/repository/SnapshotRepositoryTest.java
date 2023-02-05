/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.database.repository;

import android.database.Cursor;
import androidx.annotation.NonNull;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.api.exceptions.BaseException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.operation.ImmediatelyOperationProcessor;
import com.yandex.datasync.util.ResourcesUtil;
import com.yandex.datasync.wrappedModels.Snapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import static com.yandex.datasync.asserters.CollectionAsserter.assertCollection;
import static com.yandex.datasync.asserters.RecordsDtoAsserter.assertRecords;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class SnapshotRepositoryTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.USER;

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_DATABASE_ID_2 = "user_schedule2";

    private static final String MOCK_COLLECTION_ID = "calendar";

    private DatabaseManager databaseManager;

    @Before
    public void setUp() throws IOException {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        fillDatabaseInfo("get_database_info.json");
        fillDatabaseInfo("get_database_info_2.json");
    }

    @Test
    public void testGet() throws Exception {

        final SnapshotRepository databasesRepository = new SnapshotRepository(databaseManager,
                                                                              MOCK_CONTEXT,
                                                                              MOCK_DATABASE_ID);

        final String jsonString = ResourcesUtil.getTextFromFile("database_snapshot.json");
        final SnapshotResponse expected =
                new Moshi.Builder().build().adapter(SnapshotResponse.class).fromJson(jsonString);
        databasesRepository.save(expected);
        final SnapshotResponse actual = databasesRepository.get();

        assertRecords(actual.getRecords(), expected.getRecords());
        assertThat(actual.getRecordsCount(), is(expected.getRecordsCount()));
    }

    @Test
    public void testGetAll() throws Exception {

        final SnapshotRepository repository = new SnapshotRepository(databaseManager,
                                                                     MOCK_CONTEXT,
                                                                     MOCK_DATABASE_ID);

        final String jsonString = ResourcesUtil.getTextFromFile("database_snapshot.json");
        final SnapshotResponse expected =
                new Moshi.Builder().build().adapter(SnapshotResponse.class).fromJson(jsonString);
        repository.save(expected);
        final SnapshotResponse actual = repository.get();

        assertRecords(actual.getRecords(), expected.getRecords());
        assertThat(actual.getRecordsCount(), is(expected.getRecordsCount()));
    }

    @Test
    public void testSeveralDatabaseId() throws IOException, BaseException {

        final String jsonString = ResourcesUtil.getTextFromFile("database_snapshot.json");

        final String jsonString2 = ResourcesUtil.getTextFromFile("database_snapshot_2.json");

        final SnapshotResponse expected1 =
                new Moshi.Builder().build().adapter(SnapshotResponse.class).fromJson(jsonString);

        final SnapshotResponse expected2 =
                new Moshi.Builder().build().adapter(SnapshotResponse.class).fromJson(jsonString2);

        final SnapshotRepository repository1 = new SnapshotRepository(databaseManager,
                                                                      MOCK_CONTEXT,
                                                                      MOCK_DATABASE_ID);
        repository1.save(expected1);

        final SnapshotRepository repository2 = new SnapshotRepository(databaseManager,
                                                                      MOCK_CONTEXT,
                                                                      MOCK_DATABASE_ID_2);
        repository2.save(expected2);

        final SnapshotResponse actual1 = repository1.get();

        assertRecords(actual1.getRecords(), expected1.getRecords());
        assertThat(actual1.getRecordsCount(), is(expected1.getRecordsCount()));

        final SnapshotResponse actual2 = repository2.get();

        assertRecords(actual2.getRecords(), expected2.getRecords());
        assertThat(actual2.getRecordsCount(), is(expected2.getRecordsCount()));
    }

    @Test
    public void testGetWithCollectionId() throws IOException, BaseException {

        final SnapshotRepository repository = new SnapshotRepository(databaseManager,
                                                                     MOCK_CONTEXT,
                                                                     MOCK_DATABASE_ID);

        final String jsonString = ResourcesUtil.getTextFromFile("database_snapshot.json");
        final SnapshotResponse expected =
                new Moshi.Builder().build().adapter(SnapshotResponse.class).fromJson(jsonString);
        repository.save(expected);

        final Snapshot expectedSnapshot = new Snapshot(databaseManager,
                                                       MOCK_CONTEXT,
                                                       MOCK_DATABASE_ID,
                                                       new ImmediatelyOperationProcessor(),
                                                       expected);

        final Snapshot actualSnapshot = new Snapshot(databaseManager,
                                                     MOCK_CONTEXT,
                                                     MOCK_DATABASE_ID,
                                                     new ImmediatelyOperationProcessor(),
                                                     repository.get(MOCK_COLLECTION_ID));

        assertThat(actualSnapshot.getCollectionIds().length, is(1));
        assertThat(actualSnapshot.getCollectionIds()[0], is(MOCK_COLLECTION_ID));

        assertCollection(expectedSnapshot.getCollection(MOCK_COLLECTION_ID),
                         actualSnapshot.getCollection(MOCK_COLLECTION_ID));
    }

    @Test
    public void testDoubleSave() throws IOException, BaseException {

        final SnapshotRepository databasesRepository = new SnapshotRepository(databaseManager,
                                                                              MOCK_CONTEXT,
                                                                              MOCK_DATABASE_ID);
        final String jsonString = ResourcesUtil.getTextFromFile("database_snapshot.json");
        final SnapshotResponse expected =
                new Moshi.Builder().build().adapter(SnapshotResponse.class).fromJson(jsonString);
        databasesRepository.save(expected);
        databasesRepository.save(expected);
        final SnapshotResponse actual = databasesRepository.get();

        assertRecords(actual.getRecords(), expected.getRecords());
        assertThat(actual.getRecordsCount(), is(expected.getRecordsCount()));
    }

    @Test
    public void testDoubleSaveViaCursor() throws IOException {

        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final SnapshotRepository databasesRepository = new SnapshotRepository(databaseManager,
                                                                              MOCK_CONTEXT,
                                                                              MOCK_DATABASE_ID);
        final String jsonString = ResourcesUtil.getTextFromFile("database_snapshot.json");
        final SnapshotResponse expected =
                new Moshi.Builder().build().adapter(SnapshotResponse.class).fromJson(jsonString);

        databasesRepository.save(expected);

        final int expectedValuesCount = getValuesCount(databaseWrapper);

        databasesRepository.save(expected);

        assertThat(getValuesCount(databaseWrapper), is(expectedValuesCount));
    }

    private int getValuesCount(final SQLiteDatabaseWrapper databaseWrapper) {
        try (
                final Cursor cursor = databaseWrapper.query(DatabaseDescriptor.Value.TABLE_NAME,
                                                            null, null)
        ) {
            return cursor.moveToFirst() ? cursor.getCount() : 0;
        }
    }

    private void fillDatabaseInfo(@NonNull final String databaseFileName) throws IOException {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(MOCK_CONTEXT);
        final DatabasesRepository changesRepository = new DatabasesRepository(databaseWrapper);
        final String databaseInfo = ResourcesUtil.getTextFromFile(databaseFileName);
        final DatabaseDto databaseDto =
                new Moshi.Builder().build().adapter(DatabaseDto.class).fromJson(databaseInfo);
        changesRepository.save(databaseDto);
    }
}