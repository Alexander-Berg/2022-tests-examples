package ru.yandex.webmaster3.core.digest;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ifilippov5 on 05.09.17.
 */
public class DigestSchedulerUtilTest {

    @Test
    public void getSlidingWindowTest() {
        DateTime date = new DateTime("2017-09-04T16:21:03");
        //DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
        List<DateTime> dates = DigestSchedulerUtil.getSlidingWindow(ChronoUnit.DAYS, 7, date);

        List<DateTime> expected = Arrays.asList(
                new DateTime("2017-09-04T00:00"),
                new DateTime("2017-09-03T00:00"),
                new DateTime("2017-09-02T00:00"),
                new DateTime("2017-09-01T00:00"),
                new DateTime("2017-08-31T00:00"),
                new DateTime("2017-08-30T00:00"),
                new DateTime("2017-08-29T00:00"));
        Assert.assertEquals(expected, dates);

        dates = DigestSchedulerUtil.getSlidingWindow(ChronoUnit.HOURS, 5, date);

        expected = Arrays.asList(
                new DateTime("2017-09-04T16:00"),
                new DateTime("2017-09-04T15:00"),
                new DateTime("2017-09-04T14:00"),
                new DateTime("2017-09-04T13:00"),
                new DateTime("2017-09-04T12:00"));
        Assert.assertEquals(expected, dates);

        date = new DateTime("2017-09-06T16:21:03");
        dates = DigestSchedulerUtil.getSlidingWindow(ChronoUnit.WEEKS, 3, date);

        expected = Arrays.asList(
                new DateTime("2017-09-04T00:00"),
                new DateTime("2017-08-28T00:00"),
                new DateTime("2017-08-21T00:00"));
        Assert.assertEquals(expected, dates);

        date = new DateTime("2017-09-06T16:21:03");
        dates = DigestSchedulerUtil.getSlidingWindow(ChronoUnit.HALF_DAYS, 4, date);

        expected = Arrays.asList(
                new DateTime("2017-09-06T12:00"),
                new DateTime("2017-09-06T00:00"),
                new DateTime("2017-09-05T12:00"),
                new DateTime("2017-09-05T00:00"));
        Assert.assertEquals(expected, dates);

        date = new DateTime("2017-09-06T00:00:01");
        dates = DigestSchedulerUtil.getSlidingWindow(ChronoUnit.HALF_DAYS, 4, date);

        expected = Arrays.asList(
                new DateTime("2017-09-06T00:00"),
                new DateTime("2017-09-05T12:00"),
                new DateTime("2017-09-05T00:00"),
                new DateTime("2017-09-04T12:00"));
        Assert.assertEquals(expected, dates);

    }

    @Test
    public void getLastDigestTest() {
        DateTime now = new DateTime("2017-09-06T16:21:03");
        String cron = "0 0 0 * * 1";

        Pair<DateTime, DateTime> expected = Pair.of(new DateTime("2017-08-28T00:00:00"), new DateTime("2017-09-04T00:00:00"));
        Assert.assertEquals(expected, DigestSchedulerUtil.getLastTwoDigests(cron, now));

        cron = "0 0 */3 * * *";
        expected = Pair.of(new DateTime("2017-09-06T12:00:00"), new DateTime("2017-09-06T15:00:00"));
        Assert.assertEquals(expected, DigestSchedulerUtil.getLastTwoDigests(cron, now));

        now = new DateTime("2017-09-01T14:59:59");
        cron = "0 0 15 * * *";
        expected = Pair.of(new DateTime("2017-08-30T15:00:00"), new DateTime("2017-08-31T15:00:00"));
        Assert.assertEquals(expected, DigestSchedulerUtil.getLastTwoDigests(cron, now));
    }

    @Test
    public void getFixedWindowTest() {
        DateTime now = new DateTime("2017-09-06T16:21:03");
        String cron = "0 0 0 * * 1";
        List<DateTime> dates = DigestSchedulerUtil.getFixedWindow(cron, ChronoUnit.DAYS, now);

        List<DateTime> expected = Arrays.asList(
                new DateTime("2017-09-04T00:00"),
                new DateTime("2017-09-03T00:00"),
                new DateTime("2017-09-02T00:00"),
                new DateTime("2017-09-01T00:00"),
                new DateTime("2017-08-31T00:00"),
                new DateTime("2017-08-30T00:00"),
                new DateTime("2017-08-29T00:00"));
        Assert.assertEquals(expected, dates);

        dates = DigestSchedulerUtil.getFixedWindow(cron, ChronoUnit.WEEKS, now);

        expected = Arrays.asList(
                new DateTime("2017-09-04T00:00"));
        Assert.assertEquals(expected, dates);

        now = new DateTime("2017-09-06T16:21:03");
        cron = "0 0 0 * * 1";
        dates = DigestSchedulerUtil.getFixedWindow(cron, ChronoUnit.HALF_DAYS, now);

        expected = Arrays.asList(
                new DateTime("2017-09-04T00:00"),
                new DateTime("2017-09-03T12:00"),
                new DateTime("2017-09-03T00:00"),
                new DateTime("2017-09-02T12:00"),
                new DateTime("2017-09-02T00:00"),
                new DateTime("2017-09-01T12:00"),
                new DateTime("2017-09-01T00:00"),
                new DateTime("2017-08-31T12:00"),
                new DateTime("2017-08-31T00:00"),
                new DateTime("2017-08-30T12:00"),
                new DateTime("2017-08-30T00:00"),
                new DateTime("2017-08-29T12:00"),
                new DateTime("2017-08-29T00:00"),
                new DateTime("2017-08-28T12:00"));
        Assert.assertEquals(expected, dates);
    }
}
