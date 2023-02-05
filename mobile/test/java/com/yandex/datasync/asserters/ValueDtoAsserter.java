/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.Datatype;
import com.yandex.datasync.internal.model.ValueDto;
import com.yandex.datasync.internal.util.DateFormat;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public final class ValueDtoAsserter {

    public static void assertValue(@Nullable final ValueDto actual,
                                   @Nullable final ValueDto expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.getType(), is(actual.getType()));

        if (actual.getType() == Datatype.LIST) {
            assertListValue(actual, expected);
        } else {
            assertNotListValue(actual, expected);
        }
    }

    private static void assertListValue(@Nullable final ValueDto actual,
                                        @Nullable final ValueDto expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        final List<ValueDto> actualValues = actual.getListValues();
        final List<ValueDto> expectedValues = expected.getListValues();

        assertNotNull(actualValues);
        assertNotNull(expectedValues);

        assertThat("actualValues= " + actualValues + ", expectedValues= " + expectedValues,
                   actualValues.size(), is(expectedValues.size()));

        for (int i = 0; i < actualValues.size(); i++) {
            assertValue(actualValues.get(i), expectedValues.get(i));
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static void assertNotListValue(@Nullable final ValueDto actual,
                                           @Nullable final ValueDto expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.getStringValue(), is(expected.getStringValue()));
        assertThat(actual.getBinaryValue(), is(expected.getBinaryValue()));
        assertThat(actual.getBooleanValue(), is(expected.getBooleanValue()));
        if(actual.getDatetimeValue() != null) {
            assertThat(DateFormat.parse(actual.getDatetimeValue()),
                       is(DateFormat.parse(expected.getDatetimeValue())));
        }else{
            assertThat(actual.getDatetimeValue(), is(expected.getDatetimeValue()));
        }
        assertThat(actual.getDoubleValue(), is(expected.getDoubleValue()));
        assertThat(actual.getIntegerValue(), is(expected.getIntegerValue()));
        assertThat(actual.getNanValue(), is(expected.getNanValue()));
        assertThat(actual.getNinfValue(), is(expected.getNinfValue()));
        assertThat(actual.getNullValue(), is(expected.getNullValue()));
    }
}
