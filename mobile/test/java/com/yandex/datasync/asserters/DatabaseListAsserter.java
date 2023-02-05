/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.wrappedModels.DatabaseList;

import static com.yandex.datasync.asserters.DatabaseAsserter.assertDatabase;
import static org.junit.Assert.assertNotNull;

public final class DatabaseListAsserter {

    public static void assertDatabasesList(@Nullable final DatabaseList actual,
                                           @Nullable final DatabaseList expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        for (int i = 0; i < actual.size(); i++) {
            assertDatabase(actual.getDatabase(i), expected.getDatabase(i));
        }
    }
}
