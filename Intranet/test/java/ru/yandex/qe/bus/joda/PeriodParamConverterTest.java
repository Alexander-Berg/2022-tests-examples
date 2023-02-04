package ru.yandex.qe.bus.joda;

import org.joda.time.Period;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static ru.yandex.qe.bus.joda.DurationParamConverterTest.MILLIS_PER_DAY;
import static ru.yandex.qe.bus.joda.DurationParamConverterTest.MILLIS_PER_HOUR;
import static ru.yandex.qe.bus.joda.DurationParamConverterTest.MILLIS_PER_MINUTE;
import static ru.yandex.qe.bus.joda.DurationParamConverterTest.MILLIS_PER_WEEK;

/**
 * @author Alexei Zakharov (ayza)
 */
public class PeriodParamConverterTest {
  @Test
  public void testFromString() {
    PeriodParamConverter converter = new PeriodParamConverter();

    Period actual = converter.fromString("P3W3DT10H03M");
    Period expected = new Period(0, 3 * MILLIS_PER_MINUTE + 10 * MILLIS_PER_HOUR + 3 * MILLIS_PER_DAY + 3 * MILLIS_PER_WEEK);
    assertEquals(actual, expected);
    Period sameAsActualButDifferent = converter.fromString("PT586H3M"); // same duration but different representation
    assertNotEquals(sameAsActualButDifferent, expected);

    actual = converter.fromString("PT1M40S");
    expected = new Period(0, 100 * 1000);
    assertEquals(actual, expected);

    actual = converter.fromString("PT0S");
    expected = new Period(0, 0);
    assertEquals(actual, expected);
  }

  @Test
  public void testToString() {
    PeriodParamConverter converter = new PeriodParamConverter();

    Period test = new Period(0, 3 * MILLIS_PER_MINUTE + 10 * MILLIS_PER_HOUR + 3 * MILLIS_PER_DAY + 3 * MILLIS_PER_WEEK);
    assertEquals(converter.toString(test), "P3W3DT10H3M");

    Period test2 = new Period(0, 111 * MILLIS_PER_MINUTE);
    assertEquals(converter.toString(test2), "PT1H51M");
  }
}
