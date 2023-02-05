/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.database.sql.triggers;

import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.database.sql.cursor.FieldCursor;
import com.yandex.datasync.internal.database.sql.cursor.ValueCursor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import static com.yandex.datasync.internal.database.sql.triggers.TriggersTestObjectFactory.fillDatabase;
import static com.yandex.datasync.internal.util.Arrays2.asStringArray;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class DeleteFieldTriggerTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_COLLECTION_ID = "calendar";

    private static final String MOCK_RECORD_ID = "week";

    private static final String MOCK_FIELD_ID = "days_of_weeks";

    private SQLiteDatabaseWrapper databaseWrapper;

    private DatabaseManager databaseManager;

    @Before
    public void setUp() {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        databaseWrapper = databaseManager.openDatabaseWrapped(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test
    public void testDeleteValues() throws IOException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final long internalFieldId = getInternalFieldId();

        assertThat(internalFieldId, greaterThan(0L));

        final int countBeforeDelete = getValuesCount(internalFieldId);
        assertThat(countBeforeDelete, greaterThan(0));

        deleteField(internalFieldId);

        final int countAfterDelete = getValuesCount(internalFieldId);

        assertThat(countAfterDelete, is(0));
    }

    private void deleteField(final long internalFieldId) {
        final String selection = DatabaseDescriptor.Field.Rows.ID + " =? ";
        final String selectionArgs[] = asStringArray(internalFieldId);
        databaseWrapper.delete(DatabaseDescriptor.Field.TABLE_NAME, selection, selectionArgs);
    }

    private int getValuesCount(final long internalFieldId) {
        final String selection = DatabaseDescriptor.Value.Rows.INTERNAL_FIELD_ID + " =? ";
        final String selectionArgs[] = asStringArray(internalFieldId);

        final int result;
        try (
                final ValueCursor fieldCursor = new ValueCursor(
                        databaseWrapper.query(DatabaseDescriptor.Value.TABLE_NAME, selection,
                                              selectionArgs))
        ) {
            result = fieldCursor.getCount();
        }

        return result;
    }

    private long getInternalFieldId() {
        final String selection = DatabaseDescriptor.Field.Rows.COLLECTION_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Field.Rows.RECORD_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Field.Rows.FIELD_ID + " =? ";
        final String[] selectionArgs = {MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID};

        try (
                final FieldCursor fieldCursor = new FieldCursor(
                        databaseWrapper.query(DatabaseDescriptor.Field.TABLE_NAME, selection,
                                              selectionArgs))
        ) {
            fieldCursor.moveToFirst();
            return fieldCursor.getInternalId();
        }
    }
}