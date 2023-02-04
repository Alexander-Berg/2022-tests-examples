package ru.yandex.vertis.telepony.time

import ru.yandex.vertis.telepony.SpecBase

/**
  * @author evans
  */
class DateTimeFormatsSpec extends SpecBase {
  "Date time format" should {
    "parse iso time with timezone" in {
      val dt = FullTimeFormatter.parseDateTime("2015-01-30T17:00:00.000+05:00")
      val actual = FullTimeFormatter.print(dt)
      val expected = "2015-01-30T15:00:00.000+03:00"
      actual shouldEqual expected
    }
    "parse iso time" in {
      val dt = FullTimeFormatter.parseDateTime("2015-01-30T17:00:00.000+03:00")
      val actual = FullTimeFormatter.print(dt)
      val expected = "2015-01-30T17:00:00.000+03:00"
      actual shouldEqual expected
    }
    "parse local time" in {
      val dt = FullTimeFormatter.parseDateTime("2015-01-30T17:00:00")
      val actual = FullTimeFormatter.print(dt)
      val expected = "2015-01-30T17:00:00.000+03:00"
      actual shouldEqual expected
    }
  }
}
