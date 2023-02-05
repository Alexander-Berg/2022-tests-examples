/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.database.sql.triggers;

import android.content.ContentValues;

import com.yandex.datasync.Datatype;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.SnapshotRepository;
import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.database.sql.cursor.FieldCursor;
import com.yandex.datasync.internal.database.sql.cursor.ValueCursor;
import com.yandex.datasync.internal.database.sql.cursor.ValueTestExtCursor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import static com.yandex.datasync.internal.database.sql.triggers.TriggersTestObjectFactory.fillDatabase;
import static com.yandex.datasync.internal.util.Arrays2.asStringArray;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ListItemInsertTriggerTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_COLLECTION_ID = "calendar";

    private static final String MOCK_RECORD_ID = "week";

    private static final String MOCK_FIELD_ID = "days_of_weeks";

    private static final String MOCK_VALUE = "monday_2";

    private static final long MOCK_LIST_POSITION = 3;

    private SQLiteDatabaseWrapper databaseWrapper;

    private DatabaseManager databaseManager;

    @Before
    public void setUp() {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        databaseWrapper = databaseManager.openDatabaseWrapped(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test
    public void testInsertItem() throws IOException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final long internalFieldId = getInternalFieldId();
        final long parentId = getParentId(internalFieldId);

        insertListItem(internalFieldId, MOCK_LIST_POSITION, parentId);

        final String selection = DatabaseDescriptor.Value.Rows.INTERNAL_FIELD_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Value.Rows.PARENT_ID + " <> ?"
                                 + " AND " + DatabaseDescriptor.Value.Rows.LIST_POSITION + " =? ";
        final String selectionArgs[] = asStringArray(internalFieldId,
                                                     SnapshotRepository.NO_HAVE_PARENT_ID,
                                                     MOCK_LIST_POSITION);

        try (
                ValueTestExtCursor cursor = new ValueTestExtCursor(
                        databaseWrapper.query(DatabaseDescriptor.Value.TABLE_NAME, selection,
                                              selectionArgs))
        ) {
            assertTrue(cursor.moveToFirst());
            assertThat(cursor.getCount(), is(1));
            assertThat(cursor.getValue(), is(MOCK_VALUE));
        }
    }

    @Test
    public void testListPositionOrder() throws IOException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final long internalFieldId = getInternalFieldId();
        final long parentId = getParentId(internalFieldId);

        insertListItem(internalFieldId, MOCK_LIST_POSITION, parentId);

        final String selection = DatabaseDescriptor.Value.Rows.INTERNAL_FIELD_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Value.Rows.PARENT_ID + " <> ?";
        final String selectionArgs[] = asStringArray(internalFieldId,
                                                     SnapshotRepository.NO_HAVE_PARENT_ID);

        final String orderBy = DatabaseDescriptor.Value.Rows.LIST_POSITION + " asc";

        int expectedListPosition = 0;

        try (
                ValueTestExtCursor cursor = new ValueTestExtCursor(
                        databaseWrapper.query(DatabaseDescriptor.Value.TABLE_NAME, selection,
                                              selectionArgs, orderBy))
        ) {
            assertTrue(cursor.moveToFirst());
            do {
                assertThat(cursor.getListPosition(), is(expectedListPosition));
                expectedListPosition++;
            } while (cursor.moveToNext());
        }
    }

    @Test
    public void testListItemsCount() throws IOException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final long internalFieldId = getInternalFieldId();
        final long parentId = getParentId(internalFieldId);

        final String selection = DatabaseDescriptor.Value.Rows.INTERNAL_FIELD_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Value.Rows.PARENT_ID + " <>? ";
        final String selectionArgs[] = asStringArray(internalFieldId,
                                                     SnapshotRepository.NO_HAVE_PARENT_ID);

        final String orderBy = DatabaseDescriptor.Value.Rows.LIST_POSITION + " asc";

        try (
                ValueTestExtCursor cursor = new ValueTestExtCursor(
                        databaseWrapper.query(DatabaseDescriptor.Value.TABLE_NAME, selection,
                                              selectionArgs, orderBy))
        ) {
            assertTrue(cursor.moveToFirst());
            assertThat(cursor.getCount(), is(7));
        }

        insertListItem(internalFieldId, MOCK_LIST_POSITION, parentId);

        try (
                ValueTestExtCursor cursor = new ValueTestExtCursor(
                        databaseWrapper.query(DatabaseDescriptor.Value.TABLE_NAME, selection,
                                              selectionArgs, orderBy))
        ) {
            assertTrue(cursor.moveToFirst());
            assertThat(cursor.getCount(), is(8));
        }
    }

    private void insertListItem(final long internalFieldId,
                                final long listPosition,
                                final long parentId) {

        final ContentValues values = new ContentValues();

        values.put(DatabaseDescriptor.Value.Rows.INTERNAL_FIELD_ID, internalFieldId);
        values.put(DatabaseDescriptor.Value.Rows.PARENT_ID, parentId);
        values.put(DatabaseDescriptor.Value.Rows.TYPE, Datatype.STRING.name());
        values.put(DatabaseDescriptor.Value.Rows.VALUE, MOCK_VALUE);
        values.put(DatabaseDescriptor.Value.Rows.LIST_POSITION, listPosition);

        databaseWrapper.beginTransaction();

        databaseWrapper.insert(DatabaseDescriptor.Value.VIEW_NAME, values);

        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();
    }

    private long getInternalFieldId() {
        final String selection = DatabaseDescriptor.Field.Rows.COLLECTION_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Field.Rows.RECORD_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Field.Rows.FIELD_ID + " =? ";

        final String[] selectionArgs = {MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID};

        final long fieldId;
        try (
                final FieldCursor fieldCursor = new FieldCursor(
                        databaseWrapper.query(DatabaseDescriptor.Field.TABLE_NAME, selection,
                                              selectionArgs))
        ) {
            fieldCursor.moveToFirst();
            fieldId = fieldCursor.getInternalId();
        }
        return fieldId;
    }

    private long getParentId(final long internalFieldId) {
        final String selection = DatabaseDescriptor.Value.Rows.INTERNAL_FIELD_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Value.Rows.PARENT_ID + " =? ";
        final String[] selectionArgs = asStringArray(internalFieldId,
                                                     SnapshotRepository.NO_HAVE_PARENT_ID);

        try (
                final ValueCursor fieldCursor = new ValueCursor(
                        databaseWrapper.query(DatabaseDescriptor.Value.TABLE_NAME, selection,
                                              selectionArgs))
        ) {
            fieldCursor.moveToFirst();
            return fieldCursor.getId();
        }
    }

}