/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.internal.model.response.DatabaseDto;

import java.util.List;

import static com.yandex.datasync.util.ReflectionMatcher.reflectionEqualTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public final class DatabaseDtoListAsserter {

    public static void assertDatabasesList(@Nullable final List<DatabaseDto> actual,
                                           @Nullable final List<DatabaseDto> expected) {

        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.size(), is(expected.size()));

        assertThat(actual, is(reflectionEqualTo(expected)));
    }
}
