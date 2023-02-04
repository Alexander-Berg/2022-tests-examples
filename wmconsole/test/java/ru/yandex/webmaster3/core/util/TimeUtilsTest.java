package ru.yandex.webmaster3.core.util;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class TimeUtilsTest {
    @Test
    public void testDateFormat() throws Exception {
        DateTime dateTime = new DateTime("2017-02-27T01:00:00", TimeUtils.EUROPE_MOSCOW_ZONE);
        Instant instant = dateTime.toInstant();

        Assert.assertEquals("20170227", dateTime.toString(TimeUtils.DF_YYYYMMDD_MSK));
        Assert.assertEquals("20170227", instant.toString(TimeUtils.DF_YYYYMMDD_MSK));
        Assert.assertEquals("20170227", instant.toDateTime(TimeUtils.EUROPE_MOSCOW_ZONE).toString(TimeUtils.DF_YYYYMMDD_MSK));
    }
}
