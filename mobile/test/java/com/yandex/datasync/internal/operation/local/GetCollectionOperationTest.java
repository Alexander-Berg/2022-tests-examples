/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.local;

import androidx.annotation.NonNull;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.api.exceptions.NotSyncedException;
import com.yandex.datasync.internal.api.exceptions.http.NotFoundException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.DatabasesRepository;
import com.yandex.datasync.internal.database.repository.SnapshotRepository;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.RecordDto;
import com.yandex.datasync.internal.model.response.RecordsDto;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.operation.Operation;
import com.yandex.datasync.internal.operation.network.MockRawDataObserver;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.Iterator;

import static com.yandex.datasync.asserters.RecordsDtoAsserter.assertRecords;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class GetCollectionOperationTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_DATABASE_ID_2 = "user_schedule_2";

    private static final String MOCK_DATABASE_ID_3 = ".ext.yanotes@notes";

    private static final String MOCK_COLLECTION_ID = "calendar";

    private static final String MOCK_COLLECTION_ID_2 = "notes";

    private final MockRawDataObserver observable = new MockRawDataObserver();

    private DatabaseManager databaseManager;

    private SQLiteDatabaseWrapper databaseWrapper;

    @Before
    public void setUp() throws IOException {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        fillDatabaseInfo();

        databaseWrapper = databaseManager.openDatabaseWrapped(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test
    public void testRun() throws IOException {
        final String jsonString = ResourcesUtil.getTextFromFile("database_snapshot.json");

        final SnapshotResponse snapshot =
                new Moshi.Builder().build().adapter(SnapshotResponse.class).fromJson(jsonString);

        final SnapshotRepository repository = new SnapshotRepository(databaseManager,
                                                                     MOCK_CONTEXT,
                                                                     MOCK_DATABASE_ID);
        repository.save(snapshot);

        final Operation operation = new GetCollectionOperation(MOCK_CONTEXT,
                                                               MOCK_DATABASE_ID,
                                                               MOCK_COLLECTION_ID,
                                                               databaseManager,
                                                               observable);
        operation.run();

        final SnapshotResponse expectedSnapshot = clearSnapshot(snapshot, MOCK_COLLECTION_ID);

        assertRecords(observable.getSnapshot().getRecords(), expectedSnapshot.getRecords());
    }

    @Test
    public void testNotExists() {
        final GetSnapshotOperation operation = new GetSnapshotOperation(MOCK_CONTEXT,
                                                                        MOCK_DATABASE_ID_2,
                                                                        databaseManager,
                                                                        observable);
        operation.run();

        assertThat(observable.getException(), instanceOf(NotSyncedException.class));
    }

    @Test
    public void getAbsentCollection() throws IOException {
        final String jsonString = ResourcesUtil.getTextFromFile("empty_database.json");

        final SnapshotResponse snapshot =
                new Moshi.Builder().build().adapter(SnapshotResponse.class).fromJson(jsonString);

        final SnapshotRepository repository = new SnapshotRepository(databaseManager,
                                                                     MOCK_CONTEXT,
                                                                     MOCK_DATABASE_ID_3);
        repository.save(snapshot);

        final GetCollectionOperation operation =
                new GetCollectionOperation(MOCK_CONTEXT,
                                           MOCK_DATABASE_ID_3,
                                           MOCK_COLLECTION_ID_2,
                                           databaseManager,
                                           observable);

        operation.run();

        assertThat(observable.getException(), instanceOf(NotFoundException.class));
    }

    @SuppressWarnings("ConstantConditions")
    private SnapshotResponse clearSnapshot(@NonNull final SnapshotResponse snapshot,
                                           @NonNull final String collectionId) {
        final RecordsDto records = snapshot.getRecords();
        final Iterator<RecordDto> recordIterator = records.getItems().iterator();
        while (recordIterator.hasNext()) {
            final RecordDto currentRecord = recordIterator.next();
            if (!collectionId.equals(currentRecord.getCollectionId())) {
                recordIterator.remove();
            }
        }
        return snapshot;
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