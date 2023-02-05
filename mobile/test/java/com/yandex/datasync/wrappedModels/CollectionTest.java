/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.wrappedModels;

import androidx.annotation.NonNull;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.model.response.RecordDto;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.operation.OperationProcessor;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.yandex.datasync.asserters.RecordAsserter.assertRecord;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("ConstantConditions")
public class CollectionTest {

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_COLLECTION_ID = "calendar";

    private static final String MOCK_RECORD_ID = "year";

    private static final YDSContext MOCK_CONTEXT = YDSContext.USER;

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String INVALID_RECORD_ID = "invalid_record_id";

    private Snapshot snapshot;

    private DatabaseManager databaseManager;

    private SnapshotResponse snapshotResponse;

    @Mock
    private OperationProcessor processor;

    @Before
    public void setUp() throws IOException {

        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        final String jsonString = ResourcesUtil.getTextFromFile("database_snapshot.json");
        snapshotResponse =
                new Moshi.Builder().build().adapter(SnapshotResponse.class)
                        .fromJson(jsonString);

        snapshot = new Snapshot(databaseManager,
                                MOCK_CONTEXT,
                                MOCK_DATABASE_ID,
                                processor,
                                snapshotResponse);
    }

    @Test
    public void testGetCollectionId() throws IOException {

        final Collection collection =
                snapshot.getCollection(MOCK_COLLECTION_ID);

        assertThat(MOCK_COLLECTION_ID, is(collection.getCollectionId()));
    }

    @Test
    public void testHasRecord() throws IOException {

        final Collection collection =
                snapshot.getCollection(MOCK_COLLECTION_ID);

        assertTrue(collection.hasRecord(MOCK_RECORD_ID));

        assertFalse(collection.hasRecord(INVALID_RECORD_ID));
    }

    @Test
    public void testGetRecord() {
        final Collection collection =
                snapshot.getCollection(MOCK_COLLECTION_ID);
        final Record actual = collection.getRecord(MOCK_RECORD_ID);

        final List<RecordDto> recordsList =
                getRecordsListByCollectionIdAndRecordID(MOCK_COLLECTION_ID,
                                                        MOCK_RECORD_ID);

        assertThat(recordsList.size(), is(1));

        final Record expected = new Record(databaseManager,
                                           MOCK_CONTEXT,
                                           MOCK_DATABASE_ID,
                                           MOCK_COLLECTION_ID,
                                           MOCK_RECORD_ID,
                                           processor,
                                           recordsList.get(0));

        assertRecord(actual, expected);
    }

    @Test
    public void testGetRecordIds() {
        final Collection collection =
                snapshot.getCollection(MOCK_COLLECTION_ID);

        final String recordIds[] = collection.getRecordsIds();

        final List<RecordDto> recordsList = getRecordsListByCollectionId(MOCK_COLLECTION_ID);
        assertThat(recordIds.length, is(recordsList.size()));
    }

    private List<RecordDto> getRecordsListByCollectionId(@NonNull final String collectionId) {
        final List<RecordDto> result = new ArrayList<>();
        for (final RecordDto record : snapshotResponse.getRecords().getItems()) {
            if (collectionId.equals(record.getCollectionId())) {
                result.add(record);
            }
        }
        return result;
    }

    private List<RecordDto> getRecordsListByCollectionIdAndRecordID(
            @NonNull final String collectionId,
            @NonNull final String recordId) {
        final List<RecordDto> result = new ArrayList<>();
        for (final RecordDto record : snapshotResponse.getRecords().getItems()) {
            if (collectionId.equals(record.getCollectionId()) &&
                recordId.equals(record.getRecordId())) {

                result.add(record);
            }
        }

        return result;
    }
}