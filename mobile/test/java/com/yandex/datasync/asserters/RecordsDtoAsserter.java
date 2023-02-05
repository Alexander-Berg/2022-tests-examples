/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.internal.model.response.RecordDto;
import com.yandex.datasync.internal.model.response.RecordsDto;

import java.util.List;

import static com.yandex.datasync.asserters.RecordDtoAsserter.assertRecord;
import static com.yandex.datasync.util.ReflectionMatcher.reflectionEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public final class RecordsDtoAsserter {

    public static void assertRecords(@Nullable final RecordsDto actual,
                                     @Nullable final RecordsDto expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        final List<RecordDto> actualItems = actual.getItems();
        final List<RecordDto> expectedItems = expected.getItems();

        assertNotNull(actualItems);
        assertNotNull(expectedItems);

        assertThat(actualItems.size(), is(expectedItems.size()));

        assertThat(actual.getItems(), is(reflectionEqualTo(expected.getItems())));

        for (int i = 0; i < actualItems.size(); i++) {
            final RecordDto actualRec = actualItems.get(i);
            final RecordDto expectedRec = expectedItems.get(expectedItems.lastIndexOf(actualRec));
            assertRecord(actualRec, expectedRec);
        }
    }
}
