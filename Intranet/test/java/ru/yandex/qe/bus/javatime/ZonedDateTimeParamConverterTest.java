package ru.yandex.qe.bus.javatime;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.qe.bus.javatime.ZonedDateTimeParamConverter.DEFAULT_ZONE;

/**
 * @author Alexei Zakharov (ayza)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ZonedDateTimeParamConverterTest {
  static final ZonedDateTime OLYMPIC_1980 = ZonedDateTime.of(1980, 7, 19, 9, 0, 0, 0, ZoneId.of("Europe/Moscow"));
  static final ZonedDateTime OLYMPIC_1980_DEFAULT_TZ = ZonedDateTime.of(1980, 7, 19, 9, 0, 0, 0, DEFAULT_ZONE);
  private ZonedDateTimeParamConverter converter;

  @BeforeAll
  public void init() {
    converter = new ZonedDateTimeParamConverter();
  }

  @Test
  public void fromString_withoutTime() {
    ZonedDateTime expected = OLYMPIC_1980_DEFAULT_TZ.withHour(0);
    ZonedDateTime actual1 = converter.fromString("1980-07-19");
    assertEquals(actual1, expected);
    ZonedDateTime actual2 = converter.fromString("19800719");
    assertEquals(actual2, expected);
  }

  @Test
  public void fromString_withTime() {
    ZonedDateTime expected = OLYMPIC_1980;
    ZonedDateTime actual = converter.fromString("1980-07-19T09:00:00+03:00");
    assertEquals(actual, expected.withFixedOffsetZone());
  }

  @Test
  public void toString_basic() {
    ZonedDateTime olympic = OLYMPIC_1980;
    String actual = converter.toString(olympic);
    assertEquals(actual, "1980-07-19T09:00:00+03:00");
  }
}

