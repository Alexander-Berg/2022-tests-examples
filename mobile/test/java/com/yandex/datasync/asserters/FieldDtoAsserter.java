/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.database.sql.cursor.FieldCursor;
import com.yandex.datasync.internal.model.response.FieldDto;

import static com.yandex.datasync.asserters.ValueDtoAsserter.assertValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class FieldDtoAsserter {

    public static void assertField(@Nullable final FieldDto actual,
                                   @Nullable final FieldDto expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.getFieldId(), is(expected.getFieldId()));

        assertValue(actual.getValue(), expected.getValue());
    }

    public static void assertFieldsNotExists(@NonNull final SQLiteDatabaseWrapper databaseWrapper,
                                             @NonNull final String collectionId,
                                             @NonNull final String recordId) {
        final String selection = DatabaseDescriptor.Field.Rows.COLLECTION_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Field.Rows.RECORD_ID + " =? ";
        final String selectionArgs[] = {collectionId, recordId};
        try (
                FieldCursor cursor = new FieldCursor(
                        databaseWrapper.query(DatabaseDescriptor.Field.TABLE_NAME,
                                              selection,
                                              selectionArgs))
        ) {
            assertFalse(cursor.moveToFirst());
        }
    }

    public static void assertFieldsExists(@NonNull final SQLiteDatabaseWrapper databaseWrapper,
                                          @NonNull final String collectionId,
                                          @NonNull final String recordId) {
        final String selection = DatabaseDescriptor.Field.Rows.COLLECTION_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Field.Rows.RECORD_ID + " =? ";
        final String selectionArgs[] = {collectionId, recordId};
        try (
                FieldCursor cursor = new FieldCursor(
                        databaseWrapper.query(DatabaseDescriptor.Field.TABLE_NAME,
                                              selection,
                                              selectionArgs))
        ) {
            assertTrue(cursor.moveToFirst());
        }
    }
}
