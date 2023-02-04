package ru.yandex.realty.util

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.util.TimeUtils._

import java.time.{LocalDate, LocalDateTime, LocalTime}

@RunWith(classOf[JUnitRunner])
class TimeUtilSpec extends FlatSpec with Matchers {

  it should "serialize month correctly" in {
    val dt = DateTime.parse("2021-01-01T00:00:00+03:00")

    val shorts = for (i <- 0 until 12) yield dt.plusMonths(i).toShortRussianString
    shorts.toList shouldBe List(
      "1 янв.",
      "1 февр.",
      "1 мар.",
      "1 апр.",
      "1 мая",
      "1 июн.",
      "1 июл.",
      "1 авг.",
      "1 сент.",
      "1 окт.",
      "1 нояб.",
      "1 дек."
    )

    val longs = for (i <- 0 until 12) yield dt.plusMonths(i).toLongRussianString
    longs.toList shouldBe List(
      "1 января",
      "1 февраля",
      "1 марта",
      "1 апреля",
      "1 мая",
      "1 июня",
      "1 июля",
      "1 августа",
      "1 сентября",
      "1 октября",
      "1 ноября",
      "1 декабря"
    )
  }

  it should "correctly basic Time format" in {
    BasicTimeFormatter.format(LocalTime.of(4, 5, 6)) shouldBe "04:05"
    BasicTimeFormatter.format(LocalTime.of(4, 25, 6)) shouldBe "04:25"
    BasicTimeFormatter.format(LocalTime.of(14, 5, 6)) shouldBe "14:05"
  }

  it should "correctly basic Date format" in {
    val date = LocalDate.of(2007, 4, 23)
    BasicDateFormatter.format(date) shouldBe "2007-04-23"
  }

  it should "correctly basic LocalDateTime format" in {
    val dateTime = LocalDateTime.of(2023, 3, 11, 9, 15)
    BasicDateTimeFormatter.format(dateTime) shouldBe "2023-03-11 09:15"
  }

}
