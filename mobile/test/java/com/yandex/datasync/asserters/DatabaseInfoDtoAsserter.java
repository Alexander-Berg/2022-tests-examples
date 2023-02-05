/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.util.DateFormat;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public final class DatabaseInfoDtoAsserter {

    @SuppressWarnings("ConstantConditions")
    public static void assertDatabaseInfo(@Nullable final DatabaseDto actual,
                                          @Nullable final DatabaseDto expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.getRecordsCount(), is(expected.getRecordsCount()));
        assertThat(DateFormat.parse(actual.getCreated()),
                   is(DateFormat.parse(expected.getCreated())));
        assertThat(DateFormat.parse(actual.getModified()),
                   is(DateFormat.parse(expected.getModified())));
        assertThat(actual.getDatabaseId(), is(expected.getDatabaseId()));
        assertThat(actual.getTitle(), is(expected.getTitle()));
        assertThat(actual.getRevision(), is(expected.getRevision()));
        assertThat(actual.getSize(), is(expected.getSize()));
    }
}