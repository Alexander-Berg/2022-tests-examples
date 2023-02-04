package ru.yandex.solomon.math.stat;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Ivan Tsybulin
 */
public class DailyProfileTest {

    final static String[] ONE_WEEK = {
        "2018-06-04", "2018-06-05", "2018-06-06", "2018-06-07",
        "2018-06-08", "2018-06-09", "2018-06-10"
    };

    private int bucketOf(String dateStr, DailyProfile profile) {
        LocalDate date = LocalDate.parse(dateStr);
        return profile.getDayBucket(date);
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
    public void byNameLookupTest() throws Exception {
        Assert.assertEquals(DailyProfile.ANYDAY , DailyProfile.byName("anyday" ).get());
        Assert.assertEquals(DailyProfile.DAILY  , DailyProfile.byName("daily"  ).get());
        Assert.assertEquals(DailyProfile.WORK   , DailyProfile.byName("work"   ).get());
        Assert.assertEquals(DailyProfile.SUNSAT , DailyProfile.byName("sunsat" ).get());
        Assert.assertEquals(DailyProfile.WEEKEND, DailyProfile.byName("weekend").get());
        Assert.assertFalse(DailyProfile.byName("foobar").isPresent());
    }

}
