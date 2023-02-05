/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.wrappedModels.Database;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public final class DatabaseAsserter {

    public static void assertDatabase(@Nullable final Database actual,
                                      @Nullable final Database expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.getId(), is(expected.getId()));
        assertThat(actual.getRevision(), is(expected.getRevision()));
        assertThat(actual.getCreatedAt(), is(expected.getCreatedAt()));
        assertThat(actual.getModifiedAt(), is(expected.getModifiedAt()));
        assertThat(actual.getTitle(), is(expected.getTitle()));
        assertThat(actual.getDatabaseContext(), is(expected.getDatabaseContext()));
        assertThat(actual.isFullSnapshotSynced(), is(expected.isFullSnapshotSynced()));
    }
}
