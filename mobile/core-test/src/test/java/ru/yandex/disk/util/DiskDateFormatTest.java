package ru.yandex.disk.util;

import org.junit.Test;
import org.robolectric.annotation.Config;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.utils.FixedSystemClock;

import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.hamcrest.Matchers.equalTo;

@Config(manifest = Config.NONE)
public class DiskDateFormatTest extends AndroidTestCase2 {

    private FixedSystemClock clock;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Locale.setDefault(Locale.US);
        clock = new FixedSystemClock(1483228800000L); // 01.01.2017
    }

    @Test
    public void testFormatDateRangeSameDates() {
        final long from = 1496620800000L;// 05.06.2017
        final long to = 0;
        assertEquals("5 June", DiskDateFormat.formatDateRange(from, to, clock));
    }

    @Test
    public void testFormatDateRangeFromOneMonth() {
        final long from = 1496620800000L;// 05.06.2017
        final long to = 1497484800000L;// 15.06.2017
        assertEquals("5 - 15 June", DiskDateFormat.formatDateRange(from, to, clock));
    }

    @Test
    public void testFormatDateRangeFromDifferentMonths() {
        final long from = 1496620800000L;// 05.06.2017
        final long to = 1500076800000L;// 15.07.2017
        assertEquals("5 June - 15 July", DiskDateFormat.formatDateRange(from, to, clock));
    }

    @Test
    public void testFormat() throws Exception {
        Locale.setDefault(Locale.forLanguageTag("ru-RU"));
        final long time = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US)
                .parse("2015/08/12 12:53:23").getTime();
        final String string = DiskDateFormat.format(getMockContext(), time).toString();
        assertThat(string, equalTo("12.08.2015 12:53 PM"));
    }
}
