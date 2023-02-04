package ru.yandex.qe.util;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author entropia
 */
public final class HumanReadableTest {
  @Test
  public void zero_bytes() {
    assertThat(HumanReadable.fileSize(0), equalTo("0"));
  }

  @Test
  public void one_byte() {
    assertThat(HumanReadable.fileSize(1), equalTo("1"));
  }

  @Test
  public void some_bytes() {
    assertThat(HumanReadable.fileSize(520), equalTo("520"));
  }

  @Test
  public void less_than_kilobyte_by_a_byte() {
    assertThat(HumanReadable.fileSize(999), equalTo("999"));
  }

  @Test
  public void one_kilobyte() {
    assertThat(HumanReadable.fileSize(1024), equalTo("1K"));
  }

  @Test
  public void more_than_one_kilobyte_by_a_byte() {
    assertThat(HumanReadable.fileSize(1024), equalTo("1K"));
  }

  @Test
  public void some_kilobytes() {
    assertThat(HumanReadable.fileSize(15620), equalTo("15.25K"));
  }

  @Test
  public void less_than_megabyte_by_a_byte() {
    assertThat(HumanReadable.fileSize(1024 * 1024 - 1), equalTo("1024K"));
  }

  @Test
  public void one_megabyte() {
    assertThat(HumanReadable.fileSize(1024 * 1024), equalTo("1M"));
  }

  @Test
  public void more_than_one_megabyte_by_a_byte() {
    assertThat(HumanReadable.fileSize(1024 * 1024 + 1), equalTo("1M"));
  }

  @Test
  public void some_megabytes() {
    assertThat(HumanReadable.fileSize(15 * 1024 * 1024), equalTo("15M"));
  }

  @Test
  public void some_exabytes() {
    final BigInteger exabyte = BigInteger.valueOf(1024).pow(6);

    assertThat(HumanReadable.fileSize(exabyte.multiply(BigInteger.valueOf(138))), equalTo("138E"));
  }

  @Test
  public void negative_size_is_illegal_long() {
    assertThrows(IllegalArgumentException.class, () -> {
        HumanReadable.fileSize(-1L);
    });
  }

  @Test
  public void negative_size_is_illegal_biginteger() {
    assertThrows(IllegalArgumentException.class, () -> {
        HumanReadable.fileSize(BigInteger.valueOf(-1L));
    });
  }

  @Test
  public void less_than_a_microsecond() {
    assertThat(HumanReadable.duration(500, TimeUnit.NANOSECONDS), equalTo("500ns"));
  }

  @Test
  public void almost_a_microsecond() {
    assertThat(HumanReadable.duration(999, TimeUnit.NANOSECONDS), equalTo("999ns"));
  }

  @Test
  public void a_bit_more_than_a_microsecond() {
    assertThat(HumanReadable.duration(1001, TimeUnit.NANOSECONDS), equalTo("1us"));
  }

  @Test
  public void millis_into_minutes_and_seconds() {
    assertThat(HumanReadable.duration(63000, TimeUnit.MILLISECONDS), equalTo("00:01:03.000"));
  }

  @Test
  public void minutes_into_hours_and_minutes() {
    assertThat(HumanReadable.duration(65, TimeUnit.MINUTES), equalTo("01:05:00.000"));
  }

  @Test
  public void millis_into_hours_minutes_and_seconds() {
    assertThat(HumanReadable.duration(3675025, TimeUnit.MILLISECONDS), equalTo("01:01:15.025"));
  }

  @Test
  public void millis_into_hours_and_seconds_minutes_are_zero_and_are_omitted() {
    assertThat(HumanReadable.duration(3615000, TimeUnit.MILLISECONDS), equalTo("01:00:15.000"));
  }

  @Test
  public void more_than_24_hours_are_printed_intact() {
    assertThat(HumanReadable.duration(26, TimeUnit.HOURS), equalTo("26:00:00.000"));
  }

  @Test
  public void complex_millis_format() {
    final long day = TimeUnit.SECONDS.toMillis(86400);
    final long hr = TimeUnit.HOURS.toMillis(5);
    final long min = TimeUnit.MINUTES.toMillis(23);
    final long sec = TimeUnit.SECONDS.toMillis(38);
    final long ms = TimeUnit.MILLISECONDS.toMillis(342);

    assertThat(HumanReadable.duration(day + hr + min + sec + ms, TimeUnit.MILLISECONDS), equalTo("29:23:38.342"));
  }

  @Test
  public void shorthand_nanos_almost_a_microsecond() {
    assertThat(HumanReadable.durationNanos(999), equalTo("999ns"));
  }

  @Test
  public void shorthand_nanos_almost_a_bit_more_than_a_microsecond() {
    assertThat(HumanReadable.durationNanos(1001), equalTo("1us"));
  }

  @Test
  public void shorthand_micros_almost_a_millisecond() {
    assertThat(HumanReadable.durationMicros(999), equalTo("999us"));
  }

  @Test
  public void shorthand_micros_almost_a_bit_more_than_a_millisecond() {
    assertThat(HumanReadable.durationMicros(1001), equalTo("1ms"));
  }

  @Test
  public void shorthand_millis_150_ms() {
    assertThat(HumanReadable.durationMillis(150), equalTo("150ms"));
  }

  @Test
  public void shorthand_millis_1h_3min() {
    assertThat(HumanReadable.durationMillis(63000), equalTo("00:01:03.000"));
  }

  @Test
  public void shorthand_millis_1h_1min_15s_25ms() {
    assertThat(HumanReadable.durationMillis(3675025), equalTo("01:01:15.025"));
  }

  @Test
  public void shorthand_millis_1h_0min_15s_0ms() {
    assertThat(HumanReadable.durationMillis(3615000), equalTo("01:00:15.000"));
  }

  @Test
  public void shorthand_seconds_simple() {
    assertThat(HumanReadable.durationSeconds(5), equalTo("00:00:05.000"));
  }

  @Test
  public void shorthand_seconds_more_than_a_minute() {
    assertThat(HumanReadable.durationSeconds(63), equalTo("00:01:03.000"));
  }

  @Test
  public void shorthand_minutes_an_hour_and_some_minutes() {
    assertThat(HumanReadable.durationMinutes(65), equalTo("01:05:00.000"));
  }

  @Test
  public void shorthand_hours() {
    assertThat(HumanReadable.durationHours(3), equalTo("03:00:00.000"));
  }

  @Test
  public void shorthand_hours_more_than_one_day() {
    assertThat(HumanReadable.durationHours(28), equalTo("28:00:00.000"));
  }

  @Test
  public void shorthand_days() {
    assertThat(HumanReadable.durationDays(2), equalTo("48:00:00.000"));
  }

  @Test
  public void custom_period_formatter_ignoring_millis() {
    final PeriodFormatter formatter = new PeriodFormatterBuilder()
        .printZeroAlways()
        .minimumPrintedDigits(2)
        .appendHours()
        .appendSeparator("_")
        .appendMinutes()
        .appendSeparator("/")
        .appendSeconds()
        .toFormatter();

    assertThat(HumanReadable.duration(3675025, TimeUnit.MILLISECONDS, formatter), equalTo("01_01/15"));
    assertThat(HumanReadable.duration(3615000, TimeUnit.MILLISECONDS, formatter), equalTo("01_00/15"));
  }
}
