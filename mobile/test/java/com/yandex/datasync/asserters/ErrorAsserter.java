/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.wrappedModels.Error;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public final class ErrorAsserter {

    public static void assertError(@Nullable final Error actual,
                                   @Nullable final Error expected) {

        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.getDescription(), is(expected.getDescription()));
        assertThat(actual.getErrorType(), is(expected.getErrorType()));
    }
}
