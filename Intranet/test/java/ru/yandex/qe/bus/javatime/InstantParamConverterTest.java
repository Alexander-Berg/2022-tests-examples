package ru.yandex.qe.bus.javatime;

import java.time.Instant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexei Zakharov (ayza)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InstantParamConverterTest {
  private InstantParamConverter converter;
  private Instant OLYMPIC_1980 = ZonedDateTimeParamConverterTest.OLYMPIC_1980.toInstant();

  @BeforeAll
  public void init() {
    converter = new InstantParamConverter();
  }

  @Test
  public void fromString() {
    assertEquals(converter.fromString("123456789"), Instant.ofEpochSecond(123456789));
    assertEquals(converter.fromString("1970-01-01T00:00:01.123Z"), Instant.EPOCH.plusMillis(1123));
  }

  @Test
  public void toString_test() {
    assertEquals(converter.toString(OLYMPIC_1980), "1980-07-19T06:00:00Z");
    assertEquals(converter.toString(Instant.EPOCH.minusSeconds(3600 * 24 * 3 - 7)), "1969-12-29T00:00:07Z");
  }
}
