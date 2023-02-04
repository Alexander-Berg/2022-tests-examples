package ru.yandex.webmaster3.storage.links;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;

import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.data.HttpCodeInfo;

/**
 * @author aherman
 */
public class LinksHistoryServiceTest {

    @Test
    public void testConvert() throws Exception {
        Map<Instant, Map<Integer, Long>> httpCodes = new HashMap<>();

        LocalDate i1 = new LocalDate("2015-08-01");
        LocalDate i2 = new LocalDate("2015-08-02");
        LocalDate i3 = new LocalDate("2015-08-03");
        LocalDate i4 = new LocalDate("2015-08-04");
        LocalDate i5 = new LocalDate("2015-08-05");

        LocalTime time = new LocalTime(0, 0, 0);

        Map<Integer, Long> i1Codes = new HashMap<>();
        Map<Integer, Long> i2Codes = new HashMap<>();
        Map<Integer, Long> i3Codes = new HashMap<>();
        Map<Integer, Long> i4Codes = new HashMap<>();
        Map<Integer, Long> i5Codes = new HashMap<>();

        // 200 |-----|
        i1Codes.put(200, 1L);
        i2Codes.put(200, 2L);
        i3Codes.put(200, 3L);
        i4Codes.put(200, 4L);
        i5Codes.put(200, 5L);

        // 301 |--___|
        i1Codes.put(301, 6L);
        i2Codes.put(301, 7L);
//        i3Codes.put(301, 0L);
//        i4Codes.put(301, 0L);
//        i5Codes.put(301, 0L);

        // 302 |___--|
//        i1Codes.put(302, 0L);
//        i2Codes.put(302, 0L);
//        i3Codes.put(302, 8L);
        i4Codes.put(302, 8L);
        i5Codes.put(302, 9L);

        // 404 |_---_|
//        i1Codes.put(404, 0L);
        i2Codes.put(404, 10L);
        i3Codes.put(404, 11L);
        i4Codes.put(404, 12L);
//        i5Codes.put(404, 0L);

        // 500 |-____-|
        i1Codes.put(500, 13L);
//        i2Codes.put(500, 0L);
//        i3Codes.put(500, 0L);
//        i4Codes.put(500, 0L);
        i5Codes.put(500, 14L);

        httpCodes.put(i1.toDateTime(time, DateTimeZone.UTC).toInstant(), i1Codes);
        httpCodes.put(i2.toDateTime(time, DateTimeZone.UTC).toInstant(), i2Codes);
        httpCodes.put(i3.toDateTime(time, DateTimeZone.UTC).toInstant(), i3Codes);
        httpCodes.put(i4.toDateTime(time, DateTimeZone.UTC).toInstant(), i4Codes);
        httpCodes.put(i5.toDateTime(time, DateTimeZone.UTC).toInstant(), i5Codes);

        RawHttpCodeHistory rawLinkHistory = new RawHttpCodeHistory(httpCodes);
        Map<HttpCodeInfo, NavigableMap<LocalDate, Long>> result = LinksService.convert(rawLinkHistory, null);

        Assert.assertTrue(result.containsKey(new HttpCodeInfo(200)));
        Assert.assertTrue(result.containsKey(new HttpCodeInfo(301)));
        Assert.assertTrue(result.containsKey(new HttpCodeInfo(302)));
        Assert.assertFalse(result.containsKey(new HttpCodeInfo(303)));
        Assert.assertTrue(result.containsKey(new HttpCodeInfo(404)));
        Assert.assertTrue(result.containsKey(new HttpCodeInfo(500)));

        Map<LocalDate, Long> expected200 = asMap(
            new LocalDate[]{i1, i2, i3, i4, i5},
            new Long[] {1L, 2L, 3L, 4L, 5L}
        );
        Map<LocalDate, Long> expected301 = asMap(
            new LocalDate[]{i1, i2, i3, i4, i5},
            new Long[] {6L, 7L, 0L, 0L, 0L}
        );
        Map<LocalDate, Long> expected302 = asMap(
            new LocalDate[]{i1, i2, i3, i4, i5},
            new Long[] {0L, 0L, 0L, 8L, 9L}
        );
        Map<LocalDate, Long> expected404 = asMap(
            new LocalDate[]{i1, i2, i3, i4, i5},
            new Long[] {0L, 10L, 11L, 12L, 0L}
        );
        Map<LocalDate, Long> expected500 = asMap(
            new LocalDate[]{i1, i2, i3, i4, i5},
            new Long[] {13L, 0L, 0L, 0L, 14L}
        );
        Assert.assertEquals(expected200, result.get(new HttpCodeInfo(200)));
        Assert.assertEquals(expected301, result.get(new HttpCodeInfo(301)));
        Assert.assertEquals(expected302, result.get(new HttpCodeInfo(302)));
        Assert.assertEquals(expected404, result.get(new HttpCodeInfo(404)));
        Assert.assertEquals(expected500, result.get(new HttpCodeInfo(500)));
    }

    private static<T1, T2> Map<T1, T2> asMap(T1[] t1, T2[] t2) {
        if (t1.length != t2.length) {
            throw new IllegalArgumentException();
        }
        Map<T1, T2> result = new HashMap<>();
        for (int i = 0; i < t1.length; i++) {
            result.put(t1[i], t2[i]);
        }
        return result;
    }
}
