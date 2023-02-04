package ru.yandex.solomon.math.stat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Ivan Tsybulin
 */
public class CalendarSplitterTest {

    final static String[] ONE_WEEK = {
        "2018-06-04", "2018-06-05", "2018-06-06", "2018-06-07",
        "2018-06-08", "2018-06-09", "2018-06-10"
    };

    private long instantMillis(String instantStr) {
        return Instant.parse(instantStr).toEpochMilli();
    }

    private int bucketOf(String dateStr, DailyProfile profile) {
        LocalDate date = LocalDate.parse(dateStr);
        return profile.getDayBucket(date);
    }

    private int bucketOf(String instantStr, CalendarSplitter cal) {
        return cal.bucketOf(instantMillis(instantStr));
    }

    private CalendarSplitter.BucketWithStart bucketWithStartOf(String instantStr, CalendarSplitter cal) {
        long instant = Instant.parse(instantStr).toEpochMilli();
        return cal.bucketWithStartOf(instant);
    }

    @Test
    public void anydayTest() throws Exception {
        DailyProfile profile = DailyProfile.ANYDAY;
        Assert.assertEquals(1, profile.bucketCount());

        for (int i = 0; i < 7; i++) {
            Assert.assertEquals(0, bucketOf(ONE_WEEK[i], profile));
        }
    }

    @Test
    public void dailyTest() throws Exception {
        DailyProfile profile = DailyProfile.DAILY;
        Assert.assertEquals(7, profile.bucketCount());

        for (int i = 0; i < 7; i++) {
            Assert.assertEquals(i, bucketOf(ONE_WEEK[i], profile));
        }
    }

    @Test
    public void workTest() throws Exception {
        DailyProfile profile = DailyProfile.WORK;
        Assert.assertEquals(2, profile.bucketCount());

        final int[] expected = {0, 0, 0, 0, 0, 1, 1};
        for (int i = 0; i < 7; i++) {
            Assert.assertEquals(expected[i], bucketOf(ONE_WEEK[i], profile));
        }
    }

    @Test
    public void sunsatTest() throws Exception {
        DailyProfile profile = DailyProfile.SUNSAT;
        Assert.assertEquals(3, profile.bucketCount());

        final int[] expected = {0, 0, 0, 0, 0, 1, 2};
        for (int i = 0; i < 7; i++) {
            Assert.assertEquals(expected[i], bucketOf(ONE_WEEK[i], profile));
        }
    }

    @Test
    public void weekendTest() throws Exception {
        DailyProfile profile = DailyProfile.WEEKEND;
        Assert.assertEquals(4, profile.bucketCount());

        final int[] expected = {0, 0, 0, 0, 1, 2, 3};
        for (int i = 0; i < 7; i++) {
            Assert.assertEquals(expected[i], bucketOf(ONE_WEEK[i], profile));
        }
    }

    @Test
    public void weeklyUTCTest() throws Exception {
        CalendarSplitter cal = new CalendarSplitter(1, DailyProfile.DAILY);
        Assert.assertEquals(7, cal.bucketCount());

        Assert.assertEquals(0, bucketOf("2018-06-04T00:00:00Z", cal));
        Assert.assertEquals(0, bucketOf("2018-06-04T23:59:59Z", cal));
        Assert.assertEquals(6, bucketOf("2018-06-10T00:00:00Z", cal));
        Assert.assertEquals(6, bucketOf("2018-06-10T23:59:59Z", cal));
    }

    @Test
    public void weeklyMSKTest() throws Exception {
        CalendarSplitter cal = new CalendarSplitter(1, DailyProfile.DAILY, Duration.ofHours(3));
        Assert.assertEquals(7, cal.bucketCount());

        Assert.assertEquals(0, bucketOf("2018-06-04T00:00:00Z", cal));
        Assert.assertEquals(1, bucketOf("2018-06-04T23:59:59Z", cal));
        Assert.assertEquals(6, bucketOf("2018-06-10T00:00:00Z", cal));
        Assert.assertEquals(0, bucketOf("2018-06-10T23:59:59Z", cal));
    }

    @Test
    public void bucketStartMSKTest() throws Exception {
        CalendarSplitter cal = new CalendarSplitter(1, DailyProfile.DAILY, Duration.ofHours(3));
        Assert.assertEquals(7, cal.bucketCount());

        CalendarSplitter.BucketWithStart bs = bucketWithStartOf("2018-06-04T23:59:59Z", cal);
        Assert.assertEquals(1, bs.bucketNumber);
        Assert.assertEquals(instantMillis("2018-06-04T21:00:00Z"), bs.startMillis);
    }

    @Test
    public void hourlyTest() throws Exception {
        CalendarSplitter cal = new CalendarSplitter(24, DailyProfile.DAILY, Duration.ofHours(3));
        Assert.assertEquals(24*7, cal.bucketCount());

        Assert.assertEquals(0     , bucketOf("2018-06-03T21:00:00Z", cal));
        Assert.assertEquals(0     , bucketOf("2018-06-03T21:59:59Z", cal));
        Assert.assertEquals(1     , bucketOf("2018-06-03T22:00:00Z", cal));
        Assert.assertEquals(1     , bucketOf("2018-06-03T22:59:59Z", cal));
        Assert.assertEquals(3     , bucketOf("2018-06-04T00:00:00Z", cal));
        Assert.assertEquals(3     , bucketOf("2018-06-04T00:59:59Z", cal));
        Assert.assertEquals(24+3  , bucketOf("2018-06-05T00:30:00Z", cal));
        Assert.assertEquals(24*7-1, bucketOf("2018-06-10T20:59:59Z", cal));
        Assert.assertEquals(0     , bucketOf("2018-06-10T21:00:00Z", cal));
    }

    @Test
    public void overflowTest() throws Exception {
        CalendarSplitter cal = new CalendarSplitter(96, DailyProfile.DAILY);

        long[] counts = new long[cal.bucketCount()];
        long total = 0;

        final long MILLIS_PER_WEEK = 7L * 86400_000L;

        for (long tsMilli = 0; tsMilli < 52L * MILLIS_PER_WEEK; tsMilli += 10_000) {
            int bucketId = cal.bucketOf(tsMilli);
            Assert.assertTrue("For tsMilli = " + tsMilli + " bucketId = " + bucketId + " is invalid", (bucketId >= 0) && (bucketId < counts.length));

            counts[bucketId]++;
            total++;
        }

        for (Long val : counts) {
            Assert.assertTrue("Distribution is not uniform, delta = " + 100. * Math.abs(val * counts.length - total) / total + "%",
                Math.abs(val * counts.length - total) < 1e-6 * total);
        }

    }

}
