package ru.yandex.yandexbus.inhouse.utils.util;

import static junit.framework.Assert.assertEquals;

import com.yandex.mapkit.Time;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.Test;
import ru.yandex.yandexbus.inhouse.extensions.mapkit.TimeKt;
import ru.yandex.yandexbus.inhouse.utils.datetime.DateTime;

public class TimeUtilsConvertTimeToDateTimeTest {

    @Test
    public void name() throws Exception {
        // 01/16/2018 @ 11:00pm (UTC) in seconds
        final long value = 1516143600;

        // 2 hours in seconds
        final int tzOffset = 2 * 60 * 60;
        // 01/17/2018 @ 01:00pm (UTC+2)
        final Time time = new Time(value, tzOffset, "");

        final DateTime actual = TimeKt.toDateTime(time);

        assertEquals(16, actual.getDayOfYear());

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(actual.getLocalMillis());

        assertEquals(1, calendar.get(Calendar.HOUR));
    }
}
