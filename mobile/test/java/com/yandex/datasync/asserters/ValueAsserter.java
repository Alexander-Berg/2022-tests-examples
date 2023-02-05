/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.Datatype;
import com.yandex.datasync.wrappedModels.Value;

import static com.yandex.datasync.asserters.ValueListAsserter.assertValueList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public final class ValueAsserter {

    public static void assertValue(@Nullable final Value actual,
                                   @Nullable final Value expected) {

        assertNotNull(actual);
        assertNotNull(expected);
        assertThat(actual.getDatatype(), is(expected.getDatatype()));

        if (actual.getDatatype() == Datatype.LIST) {
            assertValueList(actual.getListValue(), expected.getListValue());
        } else {
            assertNotListValue(actual, expected);
        }
    }

    private static void assertNotListValue(@Nullable final Value actual,
                                           @Nullable final Value expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.getStringValue(), is(expected.getStringValue()));
        assertThat(actual.getBinaryData(), is(expected.getBinaryData()));
        assertThat(actual.getBooleanValue(), is(expected.getBooleanValue()));
        assertThat(actual.getDatetime(), is(expected.getDatetime()));
        assertThat(actual.getDoubleValue(), is(expected.getDoubleValue()));
        assertThat(actual.getIntegerValue(), is(expected.getIntegerValue()));
        assertThat(actual.getNanValue(), is(expected.getNanValue()));
        assertThat(actual.getNinfValue(), is(expected.getNinfValue()));
        assertThat(actual.getInfValue(), is(expected.getInfValue()));
        assertThat(actual.getNullValue(), is(expected.getNullValue()));
    }
}
