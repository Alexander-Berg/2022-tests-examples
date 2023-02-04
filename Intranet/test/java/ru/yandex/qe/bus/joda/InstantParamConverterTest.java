package ru.yandex.qe.bus.joda;

import org.joda.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexei Zakharov (ayza)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InstantParamConverterTest {
  private final Instant OLYMPIC_1980 = Instant.parse("1980-07-19T09:00:00.0+03:00");
  private InstantParamConverter converter;

  @BeforeAll
  public void init() {
    converter = new InstantParamConverter();
  }

  @Test
  public void fromString() {
    assertEquals(converter.fromString("123456789"), new Instant(123456789000L));
    assertEquals(converter.fromString("1970-01-01T00:00:01.123Z"), new Instant(1123L));
  }

  @Test
  public void toString_test() {
    assertEquals(converter.toString(OLYMPIC_1980), "1980-07-19T06:00:00.000Z");
    assertEquals(converter.toString(new Instant(0).minus(1000 * (3600 * 24 * 3 - 7))), "1969-12-29T00:00:07.000Z");
  }

}
