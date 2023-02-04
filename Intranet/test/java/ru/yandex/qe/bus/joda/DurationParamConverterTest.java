package ru.yandex.qe.bus.joda;

import org.joda.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexei Zakharov (ayza)
 */
public class DurationParamConverterTest {

  static long MILLIS_PER_MINUTE = 60 * 1000;
  static long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;
  static long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;
  static long MILLIS_PER_WEEK = 7 * MILLIS_PER_DAY;

  @Test
  public void testFromString() {
    DurationParamConverter converter = new DurationParamConverter();

    Duration actual = converter.fromString("P3W3DT10H03M");
    Duration actual2 = converter.fromString("PT586H3M"); // same duration but different representation
    Duration expected = new Duration(0, 3 * MILLIS_PER_MINUTE + 10 * MILLIS_PER_HOUR + 3 * MILLIS_PER_DAY + 3 * MILLIS_PER_WEEK);
    assertEquals(actual, expected);
    assertEquals(actual2, expected);


    actual = converter.fromString("PT70M");
    expected = new Duration(0, 70 * MILLIS_PER_MINUTE);
    assertEquals(actual, expected);

    actual = converter.fromString("PT0S");
    expected = new Duration(0, 0);
    assertEquals(actual, expected);
  }

  @Test
  public void testToString() {
    DurationParamConverter converter = new DurationParamConverter();

    Duration test = new Duration(0, 3 * MILLIS_PER_MINUTE + 10 * MILLIS_PER_HOUR + 3 * MILLIS_PER_DAY + 3 * MILLIS_PER_WEEK);
    assertEquals(converter.toString(test), "PT586H3M");

    Duration test2 = new Duration(0, 97 * MILLIS_PER_MINUTE);
    assertEquals(converter.toString(test2), "PT1H37M");

  }

}
