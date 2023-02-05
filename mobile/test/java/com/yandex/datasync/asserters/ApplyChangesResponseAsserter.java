/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.internal.model.response.ApplyChangesResponse;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public final class ApplyChangesResponseAsserter {

    public static void assertApplyChangesResponse(@Nullable final ApplyChangesResponse actual,
                                                  @Nullable final ApplyChangesResponse expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.getHref(), is(expected.getHref()));
        assertThat(actual.getMethod(), is(expected.getMethod()));
        assertThat(actual.isTemplated(), is(expected.isTemplated()));
        assertThat(actual.getRevision(), is(expected.getRevision()));
    }
}