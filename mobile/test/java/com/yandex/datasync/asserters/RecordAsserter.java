/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.wrappedModels.Record;
import com.yandex.datasync.wrappedModels.Value;

import static com.yandex.datasync.asserters.ValueAsserter.assertValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsArrayContainingInAnyOrder.arrayContainingInAnyOrder;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class RecordAsserter {

    public static void assertRecord(@Nullable final Record actual,
                                    @Nullable final Record expected) {

        assertNotNull(actual);
        assertNotNull(expected);

        final String[] actualFieldsIds = actual.getFieldsIds();
        final String[] expectedFieldsIds = expected.getFieldsIds();

        assertThat(actualFieldsIds.length, is(expectedFieldsIds.length));
        assertThat(actualFieldsIds, is(arrayContainingInAnyOrder(expectedFieldsIds)));
        assertThat(actual.getRevision(), is(not(0)));
        assertThat(actual.getRevision(), is(expected.getRevision()));

        for (final String fieldId : actualFieldsIds) {

            assertTrue(actual.hasField(fieldId));
            assertTrue(expected.hasField(fieldId));

            final Value actualValue = actual.getValue(fieldId);
            final Value expectedValue = expected.getValue(fieldId);
            assertValue(actualValue, expectedValue);
        }
    }

    public static void assertRecordIgnoreRevision(@Nullable final Record actual,
                                                  @Nullable final Record expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        final String[] actualFieldsIds = actual.getFieldsIds();
        final String[] expectedFieldsIds = expected.getFieldsIds();

        assertThat(actualFieldsIds.length, is(expectedFieldsIds.length));
        assertThat(actualFieldsIds, is(arrayContainingInAnyOrder(expectedFieldsIds)));
        for (final String fieldId : actualFieldsIds) {

            assertTrue(actual.hasField(fieldId));
            assertTrue(expected.hasField(fieldId));

            final Value actualValue = actual.getValue(fieldId);
            final Value expectedValue = expected.getValue(fieldId);
            assertValue(actualValue, expectedValue);
        }
    }
}
