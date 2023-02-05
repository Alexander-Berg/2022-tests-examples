/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.database.sql.cursor.RecordCursor;
import com.yandex.datasync.internal.model.ChangesDto;
import com.yandex.datasync.internal.model.RecordChangeType;
import com.yandex.datasync.internal.model.response.DeltaItemDto;
import com.yandex.datasync.internal.model.response.DeltasResponse;
import com.yandex.datasync.internal.model.response.FieldDto;
import com.yandex.datasync.internal.model.response.RecordDto;

import java.util.List;

import static com.yandex.datasync.asserters.FieldDtoAsserter.assertField;
import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Record.Rows.COLLECTION_ID;
import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Record.Rows.RECORD_ID;
import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Record.TABLE_NAME;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class RecordDtoAsserter {

    public static void assertRecord(@Nullable final RecordDto actual,
                                    @Nullable final RecordDto expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.getCollectionId(), is(expected.getCollectionId()));
        assertThat(actual.getRecordId(), is(expected.getRecordId()));
        assertThat(actual.getRevision(), is(not(0)));
        assertThat(actual.getRevision(), is(expected.getRevision()));

        final List<FieldDto> actualFields = actual.getFields();
        final List<FieldDto> expectedFields = expected.getFields();

        assertNotNull(actualFields);
        assertNotNull(expectedFields);

        assertThat(actualFields.size(), is(expectedFields.size()));

        for (int i = 0; i < actualFields.size(); i++) {
            assertField(actualFields.get(i), expectedFields.get(i));
        }
    }

    public static void assertRecordRevision(@NonNull final SQLiteDatabaseWrapper databaseWrapper,
                                            @NonNull final String collectionId,
                                            @NonNull final String recordId,
                                            final long expectedRevision) {
        final String selection = COLLECTION_ID + " =? " + " AND " + RECORD_ID + " =? ";
        final String selectionArgs[] = {collectionId, recordId};

        try (
                final RecordCursor cursor = new RecordCursor(
                        databaseWrapper.query(TABLE_NAME, selection, selectionArgs))
        ) {
            assertTrue(cursor.moveToFirst());
            do {
                final String msg = "collectionId: " + cursor.getCollectionId()
                                   + " recordId: " + recordId + " has invalid revision";
                assertThat(expectedRevision, is(not(0)));
                assertThat(msg, cursor.getRevision(), is(expectedRevision));
            } while (cursor.moveToNext());
        }
    }

    public static void assertRecordNotExists(@NonNull final SQLiteDatabaseWrapper databaseWrapper,
                                             @NonNull final String collectionId,
                                             @NonNull final String recordId) {
        final String selection = COLLECTION_ID + " =? " + " AND " + RECORD_ID + " =? ";
        final String selectionArgs[] = {collectionId, recordId};

        try (
                final RecordCursor cursor = new RecordCursor(
                        databaseWrapper.query(TABLE_NAME, selection, selectionArgs))
        ) {
            assertFalse(cursor.moveToFirst());
        }
    }

    public static void assertRecordRevision(@NonNull final SQLiteDatabaseWrapper databaseWrapper,
                                            @NonNull final DeltasResponse deltasResponse) {

        for (final DeltaItemDto deltaItem : deltasResponse.getItems()) {
            final long revision = deltaItem.getRevision();

            for (final ChangesDto changes : deltaItem.getChanges()) {
                final String collectionId = changes.getCollectionId();
                final String recordId = changes.getRecordId();

                assertNotNull(collectionId);
                assertNotNull(recordId);

                if (changes.getChangeType() == RecordChangeType.DELETE) {
                    assertRecordNotExists(databaseWrapper, collectionId, recordId);
                } else {
                    assertRecordRevision(databaseWrapper, collectionId, recordId, revision);
                }
            }
        }
    }

    public static void assertRecordChangeType(@NonNull final SQLiteDatabaseWrapper databaseWrapper,
                                              @NonNull final String collectionId,
                                              @NonNull final String recordId,
                                              @Nullable final RecordChangeType expectedChangeType) {
        final String selection = DatabaseDescriptor.Record.Rows.COLLECTION_ID + " =? "
                                 + " AND " + DatabaseDescriptor.Record.Rows.RECORD_ID + " =? ";
        final String selectionArgs[] = {collectionId, recordId};

        try (
                RecordCursor recordCursor = new RecordCursor(databaseWrapper.query(
                        DatabaseDescriptor.Record.TABLE_NAME,
                        selection,
                        selectionArgs))
        ) {
            assertTrue(recordCursor.moveToFirst());
            assertThat(recordCursor.getChangeType(), is(expectedChangeType));
        }
    }
}
