package ru.yandex.qe.bus.joda;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateTimeParamConverterTest {

    @Test
    public void convert_basic_date_and_iso_without_time() {
        DateTimeParamConverter converter = new DateTimeParamConverter();
        DateTime expected = converter.fromString("2014-01-01");
        DateTime actual = converter.fromString("20140101");
        assertEquals(expected, actual);
    }

    @Test
    public void convert_iso_with_time() {
        DateTimeParamConverter converter = new DateTimeParamConverter();
        DateTime expected = new DateTime("2014-06-26T14:33:29.073+04:00");
        DateTime actual = converter.fromString("2014-06-26T14:33:29.073+04:00");
        assertEquals(expected, actual);
    }

}