package ru.yandex.webmaster3.worker.spamban;

import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by ifilippov5 on 24.01.17.
 */
public class AddSpamBanRulesTaskTest {
    private AddSpamBanRulesTask addSpamBanRulesTask = new AddSpamBanRulesTaskMock();

    @NotNull
    private static Instant getDate(String hourMinute) {
        return Instant.parse("2017-02-16T" + hourMinute + ":00Z");
    }

    @Test
    public void testIsExceedQuota() {
        TreeMap<Instant, Long> ipCounter = new TreeMap<>();

        ipCounter.put(getDate("00:00"), 999L);
        ipCounter.put(getDate("00:01"), 1000L);
        Assert.assertFalse(addSpamBanRulesTask.isExceedQuota(ipCounter).isPresent());
    }

    @Test
    public void testIsExceedQuota1() {
        TreeMap<Instant, Long> ipCounter = new TreeMap<>();

        ipCounter.put(getDate("00:00"), 999L);
        ipCounter.put(getDate("00:01"), 1001L);
        Assert.assertTrue(addSpamBanRulesTask.isExceedQuota(ipCounter).isPresent());
    }

    @Test
    public void testIsExceedQuota2() {
        TreeMap<Instant, Long> ipCounter = new TreeMap<>();

        ipCounter.put(getDate("00:00"), 1000L);
        ipCounter.put(getDate("00:01"), 1000L);
        ipCounter.put(getDate("00:02"), 1000L);
        ipCounter.put(getDate("00:03"), 1000L);
        ipCounter.put(getDate("00:04"), 1000L);
        Assert.assertFalse(addSpamBanRulesTask.isExceedQuota(ipCounter).isPresent());
    }
    @Test
    public void testIsExceedQuota3() {
        TreeMap<Instant, Long> ipCounter = new TreeMap<>();

        ipCounter.put(getDate("00:00"), 1000L);
        ipCounter.put(getDate("00:01"), 1000L);
        ipCounter.put(getDate("00:02"), 1001L);
        ipCounter.put(getDate("00:03"), 1000L);
        ipCounter.put(getDate("00:04"), 1000L);
        Assert.assertTrue(addSpamBanRulesTask.isExceedQuota(ipCounter).isPresent());
    }

    @Test
    public void testIsExceedQuota4() {
        TreeMap<Instant, Long> ipCounter = new TreeMap<>();

        ipCounter.put(getDate("00:00"), 1000L);
        ipCounter.put(getDate("00:01"), 1000L);
        ipCounter.put(getDate("00:02"), 0L);
        ipCounter.put(getDate("00:03"), 0L);
        ipCounter.put(getDate("00:04"), 0L);
        ipCounter.put(getDate("00:05"), 0L);
        ipCounter.put(getDate("00:06"), 0L);
        ipCounter.put(getDate("00:07"), 1000L);
        ipCounter.put(getDate("00:08"), 1000L);
        ipCounter.put(getDate("00:09"), 1000L);
        ipCounter.put(getDate("00:10"), 1000L);
        ipCounter.put(getDate("00:11"), 1000L);
        Assert.assertFalse(addSpamBanRulesTask.isExceedQuota(ipCounter).isPresent());
    }

    @Test
    public void testIsExceedQuota5() {
        TreeMap<Instant, Long> ipCounter = new TreeMap<>();

        ipCounter.put(getDate("00:00"), 1000L);
        ipCounter.put(getDate("00:01"), 1000L);
        ipCounter.put(getDate("00:02"), 1L);
        ipCounter.put(getDate("00:03"), 1L);
        ipCounter.put(getDate("00:04"), 1L);
        ipCounter.put(getDate("00:05"), 1L);
        ipCounter.put(getDate("00:06"), 1L);
        ipCounter.put(getDate("00:07"), 1000L);
        ipCounter.put(getDate("00:08"), 1000L);
        ipCounter.put(getDate("00:09"), 1000L);
        ipCounter.put(getDate("00:10"), 1001L);
        ipCounter.put(getDate("00:11"), 0L);
        Assert.assertTrue(addSpamBanRulesTask.isExceedQuota(ipCounter).isPresent());
    }

    @Test
    public void testIsExceedQuota6() {
        TreeMap<Instant, Long> ipCounter = new TreeMap<>();

        ipCounter.put(getDate("00:00"), 1000L);
        ipCounter.put(getDate("00:01"), 1000L);
        ipCounter.put(getDate("00:02"), 1L);
        ipCounter.put(getDate("00:03"), 1L);
        ipCounter.put(getDate("00:04"), 1L);
        ipCounter.put(getDate("00:05"), 1L);
        ipCounter.put(getDate("00:06"), 1L);
        ipCounter.put(getDate("00:07"), 1L);
        ipCounter.put(getDate("00:08"), 1L);
        ipCounter.put(getDate("00:09"), 1000L);
        ipCounter.put(getDate("00:10"), 1001L);
        ipCounter.put(getDate("00:11"), 500L);
        ipCounter.put(getDate("00:12"), 1000L);
        ipCounter.put(getDate("00:13"), 1001L);
        ipCounter.put(getDate("00:14"), 500L);
        Assert.assertTrue(addSpamBanRulesTask.isExceedQuota(ipCounter).isPresent());
    }

    @Test
    public void testIsExceedQuota7() {
        TreeMap<Instant, Long> ipCounter = new TreeMap<>();

        // 00:00 - 00:03 = 4000
        ipCounter.put(getDate("00:00"), 1000L);
        ipCounter.put(getDate("00:01"), 1000L);
        ipCounter.put(getDate("00:02"), 1000L);
        ipCounter.put(getDate("00:03"), 1000L);

        // 00:13 - 00:16 = 4000
        ipCounter.put(getDate("00:13"), 1000L);
        ipCounter.put(getDate("00:14"), 1000L);
        ipCounter.put(getDate("00:14"), 1000L);
        ipCounter.put(getDate("00:16"), 1000L);
        Assert.assertFalse(addSpamBanRulesTask.isExceedQuota(ipCounter).isPresent());
    }

    @Test
    public void testIsExceedQuota8() {
        TreeMap<Instant, Long> ipCounter = new TreeMap<>();

        // 00:00 - 00:03 = 4000
        ipCounter.put(getDate("00:00"), 1000L);
        ipCounter.put(getDate("00:01"), 1000L);
        ipCounter.put(getDate("00:02"), 1000L);
        ipCounter.put(getDate("00:03"), 1000L);

        // 00:10 = 4000
        ipCounter.put(getDate("00:10"), 1001L);
        Assert.assertTrue(addSpamBanRulesTask.isExceedQuota(ipCounter).isPresent());
    }

    public static class AddSpamBanRulesTaskMock extends AddSpamBanRulesTask {

        @Override
        long getQuota() {
            return 1000;
        }

        @Override
        long getIntervalQuota() {
            return 5000;
        }
    }
}
