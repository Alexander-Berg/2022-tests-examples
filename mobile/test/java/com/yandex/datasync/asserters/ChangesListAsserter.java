/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.internal.model.ChangeDto;
import com.yandex.datasync.internal.model.ChangesDto;

import java.util.List;

import static com.yandex.datasync.asserters.ValueDtoAsserter.assertValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public final class ChangesListAsserter {

    public static void assertChangesList(@Nullable final List<ChangesDto> actual,
                                         @Nullable final List<ChangesDto> expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.size(), not(0));

        assertThat(actual.size(), is(expected.size()));
        for (int i = 0, size = actual.size(); i < size; i++) {
            assertChangesDto(actual.get(i), expected.get(i));
        }
    }

    public static void assertChangesDto(@Nullable final ChangesDto actual,
                                        @Nullable final ChangesDto expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertNotNull(actual.getCollectionId());
        assertNotNull(actual.getRecordId());
        assertNotNull(actual.getChangeType());

        assertThat(actual.getCollectionId(), is(expected.getCollectionId()));
        assertThat(actual.getRecordId(), is(expected.getRecordId()));
        assertThat(actual.getChangeType(), is(expected.getChangeType()));

        assertChangeList(actual.getChangeList(), expected.getChangeList());
    }

    public static void assertChangeList(@Nullable final List<ChangeDto> actual,
                                        @Nullable final List<ChangeDto> expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.size(), is(expected.size()));

        for (int i = 0, size = actual.size(); i < size; i++) {
            assertChangeDto(actual.get(i), expected.get(i));
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static void assertChangeDto(@Nullable final ChangeDto actual,
                                       @Nullable final ChangeDto expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.getChangeType(), is(expected.getChangeType()));
        assertThat(actual.getFieldId(), is(expected.getFieldId()));

        assertThat(actual.getListItemDest(), is(expected.getListItemDest()));

        switch (actual.getChangeType()) {
            case LIST_ITEM_DELETE:
                assertNull(actual.getValue());
                assertNull(expected.getValue());
                assertThat(actual.getListItemDest(), is(0));
                break;
            case LIST_ITEM_MOVE:
                assertNull(actual.getValue());
                assertNull(expected.getValue());
                assertThat(actual.getListItemDest(), not(0));
                break;
            case LIST_ITEM_INSERT:
            case LIST_ITEM_SET:
                assertNotNull(actual.getValue());
                assertNotNull(expected.getValue());
                assertValue(actual.getValue(), expected.getValue());
                assertThat(actual.getListItem(), not(0));
                assertThat(actual.getListItemDest(), is(0));
                break;
            case DELETE:
                assertNull(actual.getValue());
                assertNull(expected.getValue());
                assertThat(actual.getListItem(), is(0));
                assertThat(actual.getListItemDest(), is(0));
                break;
            case SET:
                assertNotNull(actual.getValue());
                assertNotNull(expected.getValue());
                assertValue(actual.getValue(), expected.getValue());
                break;
            default:
                fail("not supported field change");
        }
    }
}
