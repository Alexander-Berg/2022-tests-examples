package ru.yandex.qe.bus.javatime;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexei Zakharov (ayza)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LocalDateTimeParamConverterTest {
  private static final LocalDateTime OLYMPIC_1980 = LocalDateTime.of(1980, 7, 19, 9, 0, 0);
  private LocalDateTimeParamConverter converter;

  @BeforeAll
  public void init() {
    converter = new LocalDateTimeParamConverter();
  }

  @Test
  public void fromString_withoutTime() {
    LocalDateTime expected = LocalDateTime.of(1980, 1, 2, 0, 0);
    LocalDateTime actual1 = converter.fromString("1980-01-02");
    assertEquals(actual1, expected);
    LocalDateTime actual2 = converter.fromString("19800102");
    assertEquals(actual2, expected);
  }

  @Test
  public void fromString_withTime() {
    LocalDateTime expected = OLYMPIC_1980;
    LocalDateTime actual = converter.fromString("1980-07-19T09:00:00");
    assertEquals(actual, expected);
  }

  @Test
  public void toString_basic() {
    LocalDateTime olympic = OLYMPIC_1980;
    String actual = converter.toString(olympic);
    assertEquals(actual, "1980-07-19T09:00:00");
  }
}
