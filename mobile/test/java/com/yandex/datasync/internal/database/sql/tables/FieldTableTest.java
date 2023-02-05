/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.database.sql.tables;

import android.database.Cursor;
import androidx.annotation.NonNull;

import com.yandex.datasync.Datatype;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.SnapshotRepository;
import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.ValueDto;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class FieldTableTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_STRING_VALUE = "mock_string_value";

    private static final String MOCK_COLLECTION_ID = "calendar";

    private static final String MOCK_RECORD_ID = "week";

    private static final String MOCK_FIELD_ID = "days_of_weeks";

    private static final String MOCK_FIELD_ID_2 = "days_of_weeks_2";

    private static final ValueDto MOCK_VALUE = new ValueDto() {
        {
            setType(Datatype.STRING);
            setStringValue(MOCK_STRING_VALUE);
        }
    };

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
    public void testUniqueFields() {
        assertThat(getRecordsCount(), is(0));

        insertField(MOCK_FIELD_ID);
        assertThat(getRecordsCount(), is(1));

        insertField(MOCK_FIELD_ID);
        assertThat(getRecordsCount(), is(1));
    }

    @Test
    public void testMultipleField() {
        assertThat(getRecordsCount(), is(0));

        insertField(MOCK_FIELD_ID);
        assertThat(getRecordsCount(), is(1));

        insertField(MOCK_FIELD_ID_2);
        assertThat(getRecordsCount(), is(2));
    }

    private void insertField(@NonNull final String fieldId) {
        databaseWrapper.beginTransaction();
        snapshotRepository.saveField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, fieldId, MOCK_VALUE);
        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();
    }

    private int getRecordsCount() {
        try (
                final Cursor cursor = databaseWrapper.query(DatabaseDescriptor.Field.TABLE_NAME,
                                                            null, null)
        ) {
            return cursor.moveToFirst() ? cursor.getCount() : 0;
        }
    }
}