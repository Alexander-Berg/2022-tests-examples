/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class DateFormatTest {

    private static final String MOCK_DATE = "2016-08-02T09:02:05.893000+00:00";

    @Test
    public void testParse() {
        final Date expectedDate = DateFormat.parse(MOCK_DATE);
        final String actual = DateFormat.format(expectedDate);

        assertThat(DateFormat.parse(actual).getTime(), is(expectedDate.getTime()));
        assertThat(DateFormat.parse(actual), is(DateFormat.parse(MOCK_DATE)));
    }

    @Test
    public void testFormat() {
        final Date expectedDate = new Date();
        final String formattedDate = DateFormat.format(expectedDate);
        assertThat(DateFormat.parse(formattedDate), is(expectedDate));
        assertThat(DateFormat.parse(formattedDate).getTime(), is(expectedDate.getTime()));
    }
}