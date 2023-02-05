/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.wrappedModels;

import android.content.res.Resources;
import androidx.annotation.NonNull;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.model.response.RecordDto;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.operation.ImmediatelyOperationProcessor;
import com.yandex.datasync.internal.operation.OperationProcessor;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.yandex.datasync.asserters.CollectionAsserter.assertCollection;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ConstantConditions")
@RunWith(RobolectricTestRunner.class)
public class SnapshotTest {

    private static final String MOCK_COLLECTION_ID = "calendar";

    private static final String INVALID_COLLECTION_ID = "invalid_collection_id";

    private static final YDSContext MOCK_CONTEXT = YDSContext.USER;

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_DATABASE_ID = "mock_database_id";

    private SnapshotResponse snapshotResponse;

    private Snapshot snapshot;

    private DatabaseManager databaseManager;

    private final OperationProcessor processor = new ImmediatelyOperationProcessor();

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
    public void testHasCollection() {

        assertTrue(snapshot.hasCollection(MOCK_COLLECTION_ID));
        assertFalse(snapshot.hasCollection(INVALID_COLLECTION_ID));
    }

    @Test
    public void testGetExistingCollection() throws IOException {

        final Collection actual = snapshot.getCollection(MOCK_COLLECTION_ID);

        final Collection expected =
                getCollectionWrapperFromSnapshot(snapshotResponse, MOCK_COLLECTION_ID);

        assertCollection(actual, expected);
    }

    @Test(expected = Resources.NotFoundException.class)
    public void testGetNotExistingCollection() throws IOException {

        assertFalse(snapshot.hasCollection(INVALID_COLLECTION_ID));

        snapshot.getCollection(INVALID_COLLECTION_ID);
    }

    private Collection getCollectionWrapperFromSnapshot(@NonNull final SnapshotResponse snapshot,
                                                        @NonNull final String collectionId) {
        final Map<String, RecordDto> map = new HashMap<>();
        for (final RecordDto record : snapshot.getRecords().getItems()) {
            if (collectionId.equals(record.getCollectionId())) {
                map.put(record.getRecordId(), record);
            }
        }
        return new Collection(databaseManager,
                              MOCK_CONTEXT,
                              MOCK_DATABASE_ID,
                              collectionId,
                              processor,
                              map);
    }
}