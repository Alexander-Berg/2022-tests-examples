package ru.yandex.qe.bus.javatime;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexei Zakharov (ayza)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DurationParamConverterTest {
  private DurationParamConverter converter;

  @BeforeAll
  public void init() {
    converter = new DurationParamConverter();
  }

  @Test
  public void fromString() {
    assertEquals(converter.fromString("PT5S"), Duration.ofSeconds(5));
    assertEquals(converter.fromString("P1D"), Duration.ofDays(1));
    assertEquals(converter.fromString("PT5H17M"), Duration.ofHours(5).plus(Duration.ofMinutes(17)));
  }

  @Test
  public void toString_test() {
    assertEquals(converter.toString(Duration.ofSeconds(100)), "PT1M40S");
    assertEquals(converter.toString(Duration.ofMinutes(1).plus(Duration.ofSeconds(40))), "PT1M40S");
    assertEquals(converter.toString(Duration.ofDays(11).plus(Duration.ofHours(7))), "PT271H");
    assertEquals(converter.toString(Duration.ofMillis(0)), "PT0S");
  }

}
