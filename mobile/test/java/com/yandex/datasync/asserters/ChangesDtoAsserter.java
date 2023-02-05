/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.NonNull;

import com.yandex.datasync.Datatype;
import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.database.sql.cursor.FieldCursor;
import com.yandex.datasync.internal.database.sql.cursor.RecordCursor;
import com.yandex.datasync.internal.database.sql.cursor.ValueCursor;
import com.yandex.datasync.internal.model.FieldChangeType;
import com.yandex.datasync.internal.model.RecordChangeType;

import static com.yandex.datasync.internal.util.Arrays2.asStringArray;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class ChangesDtoAsserter {

    public static void assertRecordChanges(@NonNull final SQLiteDatabaseWrapper databaseWrapper,
                                           @NonNull final RecordChangeType recordChangeType,
                                           @NonNull final String collectionId,
                                           @NonNull final String recordId) {
        final String selection = DatabaseDescriptor.Record.Rows.INTERNAL_CHANGE_TYPE + " =? ";
        final String selectionArgs[] = {recordChangeType.name()};
        try (
                final RecordCursor cursor = new RecordCursor(
                        databaseWrapper.query(DatabaseDescriptor.Record.TABLE_NAME, selection,
                                              selectionArgs))
        ) {
            assertTrue(cursor.moveToFirst());
            assertThat(cursor.getCount(), is(1));
            assertThat(cursor.getCollectionId(), is(collectionId));
            assertThat(cursor.getRecordId(), is(recordId));
        }
    }

    public static void assertFieldChanges(@NonNull final SQLiteDatabaseWrapper databaseWrapper,
                                          @NonNull final FieldChangeType fieldChangeType,
                                          @NonNull final String collectionId,
                                          @NonNull final String recordId,
                                          @NonNull final String fieldId) {
        final String selection = DatabaseDescriptor.Field.Rows.COLLECTION_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Field.Rows.RECORD_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Field.Rows.FIELD_ID + " =? ";

        final String selectionArgs[] = {collectionId, recordId, fieldId};
        try (
                final FieldCursor cursor = new FieldCursor(
                        databaseWrapper.query(DatabaseDescriptor.Field.TABLE_NAME, selection,
                                              selectionArgs))
        ) {
            assertTrue(cursor.moveToFirst());
            assertThat(cursor.getCount(), is(1));
            assertThat(cursor.getChangeType(), is(fieldChangeType));
        }
    }

    public static void assertFieldListChanges(@NonNull final SQLiteDatabaseWrapper databaseWrapper,
                                              @NonNull final FieldChangeType fieldChangeType,
                                              @NonNull final String collectionId,
                                              @NonNull final String recordId,
                                              @NonNull final String fieldId,
                                              final int listPosition) {
        assertFieldChanges(databaseWrapper, fieldChangeType, collectionId, recordId, fieldId);
        final long internalFieldId =
                getInternalFieldId(databaseWrapper, collectionId, recordId, fieldId);

        final String selection = DatabaseDescriptor.Value.Rows.INTERNAL_FIELD_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Value.Rows.LIST_POSITION + " =? ";

        final String selectionArgs[] = asStringArray(internalFieldId, listPosition);

        try (
                final ValueCursor valueCursor = new ValueCursor(
                        databaseWrapper.query(DatabaseDescriptor.Value.TABLE_NAME, selection,
                                              selectionArgs))
        ) {
            assertTrue(valueCursor.moveToFirst());
            assertThat(valueCursor.getListPosition(), is(listPosition));
        }
    }

    public static void assertValueChange(@NonNull final SQLiteDatabaseWrapper databaseWrapper,
                                         @NonNull final FieldChangeType fieldChangeType,
                                         @NonNull final String collectionId,
                                         @NonNull final String recordId,
                                         @NonNull final String fieldId,
                                         @NonNull final Datatype datatype,
                                         @NonNull final String value) {

        final String selection = DatabaseDescriptor.Value.Rows.INTERNAL_FIELD_ID + " =? ";

        final long internalFieldId =
                getInternalFieldId(databaseWrapper, collectionId, recordId, fieldId);
        assertThat(internalFieldId, not(-1));

        final String selectionArgs[] = {String.valueOf(internalFieldId)};
        try (
                final ValueCursor cursor = new ValueCursor(
                        databaseWrapper.query(DatabaseDescriptor.Value.TABLE_NAME, selection,
                                              selectionArgs))
        ) {
            assertTrue(cursor.moveToFirst());
        }
    }

    private static long getInternalFieldId(@NonNull final SQLiteDatabaseWrapper databaseWrapper,
                                           @NonNull final String collectionId,
                                           @NonNull final String recordId,
                                           @NonNull final String fieldId) {
        final long result;
        final String selection = DatabaseDescriptor.Field.Rows.COLLECTION_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Field.Rows.RECORD_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Field.Rows.FIELD_ID + " =? ";
        final String selectionArgs[] = {collectionId, recordId, fieldId};
        try (
                final FieldCursor fieldCursor = new FieldCursor(
                        databaseWrapper.query(DatabaseDescriptor.Field.TABLE_NAME, selection,
                                              selectionArgs))
        ) {
            result = fieldCursor.moveToFirst() ? fieldCursor.getInternalId() : -1;
        }
        return result;
    }
}
