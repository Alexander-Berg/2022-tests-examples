package ru.yandex.webmaster3.core.util;

import java.util.TreeMap;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class DailyQuotaUtilTest {
    @Test
    public void testDailyQuota() throws Exception {
        TreeMap<LocalDate, Integer> usages = new TreeMap<>();
        usages.put(new LocalDate("2016-10-01"), 5);
        usages.put(new LocalDate("2016-10-03"), 10);
        usages.put(new LocalDate("2016-10-04"), 8);
        usages.put(new LocalDate("2016-10-06"), 12);
        usages.put(new LocalDate("2016-10-07"), 9);
        usages.put(new LocalDate("2016-10-08"), 8);
        usages.put(new LocalDate("2016-10-09"), 14);
        usages.put(new LocalDate("2016-10-10"), 2);
        usages.put(new LocalDate("2016-10-12"), 11);
        usages.put(new LocalDate("2016-10-13"), 11);
        usages.put(new LocalDate("2016-10-14"), 11);
        usages.put(new LocalDate("2016-10-15"), 0);

        assertQuota(5, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-01"), usages, 10, 7));
        assertQuota(10, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-02"), usages, 10, 7));
        assertQuota(0, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-03"), usages, 10, 7));
        assertQuota(2, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-04"), usages, 10, 7));
        assertQuota(10, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-05"), usages, 10, 7));
        assertQuota(0, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-06"), usages, 10, 7));
        assertQuota(0, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-07"), usages, 10, 7));
        assertQuota(1, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-08"), usages, 10, 7));
        assertQuota(0, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-09"), usages, 10, 7));
        assertQuota(4, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-10"), usages, 10, 7));
        assertQuota(10, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-11"), usages, 10, 7));
        assertQuota(0, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-12"), usages, 10, 7));
        assertQuota(0, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-13"), usages, 10, 7));
        assertQuota(0, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-14"), usages, 10, 7));
        assertQuota(7, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-15"), usages, 10, 7));
    }

    @Test
    public void testDailyQuota1() throws Exception {
        TreeMap<LocalDate, Integer> usages = new TreeMap<>();
        usages.put(new LocalDate("2016-10-01"), 11);
        usages.put(new LocalDate("2016-10-02"), 10);
        usages.put(new LocalDate("2016-10-03"), 9);
        usages.put(new LocalDate("2016-10-04"), 8);
        usages.put(new LocalDate("2016-10-05"), 7);
        usages.put(new LocalDate("2016-10-06"), 6);
        usages.put(new LocalDate("2016-10-07"), 5);

        assertQuota(0, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-01"), usages, 10, 7));
        assertQuota(0, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-02"), usages, 10, 7));
        assertQuota(0, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-03"), usages, 10, 7));
        assertQuota(2, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-04"), usages, 10, 7));
        assertQuota(3, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-05"), usages, 10, 7));
        assertQuota(4, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-06"), usages, 10, 7));
        assertQuota(5, DailyQuotaUtil.computeRemainingQuotaDelUrl(new LocalDate("2016-10-07"), usages, 10, 7));
    }

    private void assertQuota(int expected, DailyQuotaUtil.QuotaUsage actual) {
        Assert.assertEquals(expected, actual.getQuotaRemain());
    }
}
