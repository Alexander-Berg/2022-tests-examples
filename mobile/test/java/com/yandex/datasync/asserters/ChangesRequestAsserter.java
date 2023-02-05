/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.internal.model.request.ChangesRequest;

import static com.yandex.datasync.asserters.ChangesListAsserter.assertChangesList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public final class ChangesRequestAsserter {

    public static final void assertChangesRequest(@Nullable final ChangesRequest actual,
                                                  @Nullable final ChangesRequest expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat("ChangesRequest.getDeltaId()", actual.getDeltaId(), is(expected.getDeltaId()));
        assertChangesList(actual.getChanges(), expected.getChanges());
    }
}
