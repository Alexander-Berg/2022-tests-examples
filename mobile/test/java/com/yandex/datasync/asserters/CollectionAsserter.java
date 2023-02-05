/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.wrappedModels.Collection;

import static com.yandex.datasync.asserters.RecordAsserter.assertRecord;
import static com.yandex.datasync.asserters.RecordAsserter.assertRecordIgnoreRevision;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsArrayContainingInAnyOrder.arrayContainingInAnyOrder;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public final class CollectionAsserter {

    public static void assertCollection(@Nullable final Collection actual,
                                        @Nullable final Collection expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.getCollectionId(), is(expected.getCollectionId()));

        assertNotNull(actual.getDatabaseId());
        assertNotNull(actual.getDatabaseContext());

        assertThat(actual.getDatabaseId(), is(expected.getDatabaseId()));
        assertThat(actual.getDatabaseContext(), is(expected.getDatabaseContext()));

        final String[] actualRecordsId = actual.getRecordsIds();
        final String[] expectedRecordsIds = expected.getRecordsIds();

        assertThat(actualRecordsId.length, is(expectedRecordsIds.length));
        assertThat(actualRecordsId, is(arrayContainingInAnyOrder(expectedRecordsIds)));

        for (final String recordId : actualRecordsId) {
            assertRecord(actual.getRecord(recordId), expected.getRecord(recordId));
        }
    }

    public static void assertCollectionIgnoreRevision(@Nullable final Collection actual,
                                                      @Nullable final Collection expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.getCollectionId(), is(expected.getCollectionId()));

        assertNotNull(actual.getDatabaseId());
        assertNotNull(actual.getDatabaseContext());

//        assertThat(actual.getDatabaseId(), is(expected.getDatabaseId()));
        assertThat(actual.getDatabaseContext(), is(expected.getDatabaseContext()));

        final String[] actualRecordsId = actual.getRecordsIds();
        final String[] expectedRecordsIds = expected.getRecordsIds();

        assertThat(actualRecordsId.length, is(expectedRecordsIds.length));
        assertThat(actualRecordsId, is(arrayContainingInAnyOrder(expectedRecordsIds)));

        for (final String recordId : actualRecordsId) {
            assertRecordIgnoreRevision(actual.getRecord(recordId), expected.getRecord(recordId));
        }
    }
}
