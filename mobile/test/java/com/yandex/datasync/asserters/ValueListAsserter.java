/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.wrappedModels.ValuesList;
import com.yandex.datasync.wrappedModels.Value;

import static com.yandex.datasync.asserters.ValueAsserter.assertValue;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public final class ValueListAsserter {

    public static void assertValueList(@Nullable final ValuesList expected,
                                       @Nullable final ValuesList actual) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat("actual=" + actual + ", expected=" + expected,
                   actual.size(), is(expected.size()));

        for (int i = 0; i < actual.size(); i++) {
            final Value actualValue = actual.getValue(i);
            final Value expectedValue = expected.getValue(i);

            assertValue(actualValue, expectedValue);
        }
    }
}
