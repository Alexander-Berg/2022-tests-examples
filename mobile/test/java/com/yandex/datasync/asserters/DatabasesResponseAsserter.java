/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.internal.model.response.DatabasesResponse;

import static com.yandex.datasync.asserters.DatabaseDtoListAsserter.assertDatabasesList;
import static org.junit.Assert.assertNotNull;

public final class DatabasesResponseAsserter {

    public static void assertDatabases(@Nullable final DatabasesResponse actual,
                                       @Nullable final DatabasesResponse expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertNotNull(actual.getDatabaseList());
        assertNotNull(expected.getDatabaseList());

        assertDatabasesList(actual.getDatabaseList(), expected.getDatabaseList());
    }
}
