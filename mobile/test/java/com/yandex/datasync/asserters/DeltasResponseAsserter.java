/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.internal.model.response.DeltasResponse;

import static com.yandex.datasync.util.ReflectionMatcher.reflectionEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public final class DeltasResponseAsserter {

    public static void assertDeltas(@Nullable final DeltasResponse actual,
                                    @Nullable final DeltasResponse expected) {
        assertNotNull(actual);
        assertNotNull(expected);
        assertThat(actual.getBaseRevision(), is(expected.getBaseRevision()));
        assertThat(actual.getItems(), is(reflectionEqualTo(expected.getItems())));
        assertThat(actual.getTotal(), is(expected.getTotal()));
        assertThat(actual.getLimit(), is(expected.getLimit()));
        assertThat(actual.getRevision(), is(expected.getRevision()));
    }
}
