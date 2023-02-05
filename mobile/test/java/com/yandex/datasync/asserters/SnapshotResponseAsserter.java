/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.util.DateFormat;

import static com.yandex.datasync.asserters.RecordsDtoAsserter.assertRecords;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNotNull;

public final class SnapshotResponseAsserter {

    @SuppressWarnings("ConstantConditions")
    public static void assertSnapshot(@Nullable final SnapshotResponse actual,
                                      @Nullable final SnapshotResponse expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.getRecordsCount(), is(expected.getRecordsCount()));
        assertThat(DateFormat.parse(actual.getCreated()),
                   is(DateFormat.parse(expected.getCreated())));
        assertThat(DateFormat.parse(actual.getModified()),
                   is(DateFormat.parse(expected.getModified())));
        assertThat(actual.getDatabaseId(), is(expected.getDatabaseId()));
        assertThat(actual.getRevision(), is(expected.getRevision()));
        assertRecords(actual.getRecords(), expected.getRecords());
    }
}
