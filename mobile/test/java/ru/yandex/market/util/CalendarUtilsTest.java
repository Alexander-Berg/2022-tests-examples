package ru.yandex.market.util;

import org.junit.Assert;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import ru.yandex.market.calendar.CalendarUtils;

public class CalendarUtilsTest {

    @Test
    public void testFormatAsRfc1123() {
        // 3 февраля 2017 14:23:36 +0300
        ZonedDateTime dateTime = ZonedDateTime.of(2017, 2, 3, 14, 23, 36, 0,
                ZoneId.ofOffset("", ZoneOffset.ofHours(3)));

        Date date = new Date(TimeUnit.SECONDS.toMillis(dateTime.toEpochSecond()));
        String asString = CalendarUtils.formatAsRfc1123(date);
        ZonedDateTime parsedDateTime = ZonedDateTime.parse(asString,
                DateTimeFormatter.RFC_1123_DATE_TIME);
        Assert.assertTrue(dateTime.isEqual(parsedDateTime));
    }
}
