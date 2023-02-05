/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.database.sql.triggers;

import android.content.ContentValues;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.SnapshotRepository;
import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.database.sql.cursor.FieldCursor;
import com.yandex.datasync.internal.database.sql.cursor.ValueCursor;
import com.yandex.datasync.internal.database.sql.cursor.ValueTestExtCursor;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import static com.yandex.datasync.internal.util.Arrays2.asStringArray;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ListItemMoveDownTriggerTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_COLLECTION_ID = "calendar";

    private static final String MOCK_RECORD_ID = "week";

    private static final String MOCK_FIELD_ID = "days_of_weeks";

    private static final String MOCK_VALUE = "Friday";

    private static final int MOCK_POSITION = 5;

    private static final int MOCK_POSITION_DESTINATION = 1;

    private SQLiteDatabaseWrapper databaseWrapper;

    private DatabaseManager databaseManager;

    @Before
    public void setUp() {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        databaseWrapper = databaseManager.openDatabaseWrapped(MOCK_CONTEXT,
                                                              MOCK_DATABASE_ID);
    }

    @Test
    public void testListPositionOrder() throws IOException {
        fillDatabase();

        final long internalFieldId = getInternalFieldId();
        final long parentId = getParentId(internalFieldId);

        moveItem(internalFieldId, MOCK_POSITION, MOCK_POSITION_DESTINATION, parentId);

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
    public void testValueMove() throws IOException {
        fillDatabase();

        final long internalFieldId = getInternalFieldId();
        final long parentId = getParentId(internalFieldId);

        final int moveFrom = MOCK_POSITION;
        final int moveTo = MOCK_POSITION_DESTINATION;
        final String expectedValue = getValue(internalFieldId, parentId, moveFrom);

        moveItem(internalFieldId, moveFrom, moveTo, parentId);

        final String selection = DatabaseDescriptor.Value.Rows.INTERNAL_FIELD_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Value.Rows.PARENT_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Value.Rows.LIST_POSITION + " =? ";
        final String selectionArgs[] = asStringArray(internalFieldId,
                                                     parentId,
                                                     MOCK_POSITION_DESTINATION);

        try (
                ValueTestExtCursor cursor = new ValueTestExtCursor(
                        databaseWrapper.query(DatabaseDescriptor.Value.TABLE_NAME, selection,
                                              selectionArgs))
        ) {
            assertTrue(cursor.moveToFirst());
            assertThat(cursor.getCount(), is(1));
            assertThat(cursor.getValue(), is(expectedValue));
            assertThat(cursor.getValue(), is(MOCK_VALUE));
        }
    }

    @Test
    public void testLastToFirst() throws IOException {
        fillDatabase();

        final long internalFieldId = getInternalFieldId();
        final long parentId = getParentId(internalFieldId);

        final int moveFrom = getSize(internalFieldId, parentId) - 1;
        final int moveTo = 0;
        final String expectedValue = getValue(internalFieldId, parentId, moveFrom);

        moveItem(internalFieldId, moveFrom, moveTo, parentId);

        final String selection = DatabaseDescriptor.Value.Rows.INTERNAL_FIELD_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Value.Rows.PARENT_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Value.Rows.LIST_POSITION + " =? ";
        final String selectionArgs[] = asStringArray(internalFieldId,
                                                     parentId,
                                                     moveTo);

        try (
                ValueTestExtCursor cursor = new ValueTestExtCursor(
                        databaseWrapper.query(DatabaseDescriptor.Value.TABLE_NAME, selection,
                                              selectionArgs))
        ) {
            assertTrue(cursor.moveToFirst());
            assertThat(cursor.getCount(), is(1));
            assertThat(cursor.getValue(), is(expectedValue));
        }
    }

    private void moveItem(final long internalFieldId,
                          final long position,
                          final long positionDestination,
                          final long parentId) {

        final String selection = DatabaseDescriptor.Value.Rows.INTERNAL_FIELD_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Value.Rows.LIST_POSITION + " =? "
                                 + " AND " + DatabaseDescriptor.Value.Rows.PARENT_ID + " =? ";
        final String selectionArgs[] = asStringArray(internalFieldId, position, parentId);

        final ContentValues values = new ContentValues();
        values.put(DatabaseDescriptor.Value.Rows.LIST_POSITION, positionDestination);

        databaseWrapper.beginTransaction();
        databaseWrapper.update(DatabaseDescriptor.Value.VIEW_NAME,
                               values,
                               selection,
                               selectionArgs);

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

    private int getSize(final long internalFieldId, final long internalParentId) {
        final String selection = DatabaseDescriptor.Value.Rows.INTERNAL_FIELD_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Value.Rows.PARENT_ID + " =? ";
        final String[] selectionArgs = asStringArray(internalFieldId,
                                                     internalParentId);

        try (
                final ValueCursor fieldCursor = new ValueCursor(
                        databaseWrapper.query(DatabaseDescriptor.Value.TABLE_NAME, selection,
                                              selectionArgs))
        ) {
            fieldCursor.moveToFirst();
            return fieldCursor.getCount();
        }
    }

    private String getValue(final long internalFieldId, final long parentId, final int moveTo) {
        final String selection = DatabaseDescriptor.Value.Rows.INTERNAL_FIELD_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Value.Rows.PARENT_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Value.Rows.LIST_POSITION + " =? ";
        final String[] selectionArgs = asStringArray(internalFieldId, parentId, moveTo);

        try (
                final ValueCursor fieldCursor = new ValueCursor(
                        databaseWrapper.query(DatabaseDescriptor.Value.TABLE_NAME, selection,
                                              selectionArgs))
        ) {
            fieldCursor.moveToFirst();
            return fieldCursor.getValue();
        }
    }

    private void fillDatabase() throws IOException {
        final String jsonString = ResourcesUtil.getTextFromFile("database_snapshot.json");
        final SnapshotResponse snapshot =
                new Moshi.Builder().build().adapter(SnapshotResponse.class)
                        .fromJson(jsonString);
        final SnapshotRepository snapshotRepository = new SnapshotRepository(databaseManager,
                                                                             MOCK_CONTEXT,
                                                                             MOCK_DATABASE_ID);
        snapshotRepository.save(snapshot);
    }
}
