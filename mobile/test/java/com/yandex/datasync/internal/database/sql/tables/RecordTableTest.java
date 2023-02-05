/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.database.sql.tables;

import android.database.Cursor;
import androidx.annotation.NonNull;

import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.SnapshotRepository;
import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class RecordTableTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_COLLECTION_ID = "calendar";

    private static final String MOCK_RECORD_ID = "week";

    private static final String MOCK_RECORD_ID_2 = "week_2";

    private static final long MOCK_REVISION = 10L;

    private SnapshotRepository snapshotRepository;

    private SQLiteDatabaseWrapper databaseWrapper;

    @Before
    public void setUp() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        databaseWrapper = databaseManager.openDatabaseWrapped(MOCK_CONTEXT, MOCK_DATABASE_ID);

        snapshotRepository = new SnapshotRepository(databaseManager,
                                                    MOCK_CONTEXT,
                                                    MOCK_DATABASE_ID);
    }

    @Test
    public void testUniqueRecord() {
        assertThat(getRecordsCount(), is(0));

        insertRecord(MOCK_RECORD_ID);
        assertThat(getRecordsCount(), is(1));

        insertRecord(MOCK_RECORD_ID);
        assertThat(getRecordsCount(), is(1));
    }

    @Test
    public void testMultipleRecord() {
        assertThat(getRecordsCount(), is(0));

        insertRecord(MOCK_RECORD_ID);
        assertThat(getRecordsCount(), is(1));

        insertRecord(MOCK_RECORD_ID_2);
        assertThat(getRecordsCount(), is(2));
    }

    private void insertRecord(@NonNull final String recordId) {
        databaseWrapper.beginTransaction();

        snapshotRepository.saveRecord(MOCK_COLLECTION_ID, recordId, MOCK_REVISION);

        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();
    }

    private int getRecordsCount() {
        try (
                final Cursor cursor = databaseWrapper.query(DatabaseDescriptor.Record.TABLE_NAME,
                                                            null, null)
        ) {
            return cursor.moveToFirst() ? cursor.getCount() : 0;
        }
    }
}