package ru.yandex.vertis.punisher.dao

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.util.DateTimeUtils
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval

import java.time.ZonedDateTime

@RunWith(classOf[JUnitRunner])
class YtFolderSupportSpec extends BaseSpec {
  "dayTable" should {
    "return correct table" in {
      val zdt = ZonedDateTime.of(2020, 1, 10, 12, 0, 0, 0, DateTimeUtils.DefaultZoneId)
      val table = YtFolderSupport.dayTable(zdt)
      table shouldBe "2020-01-10"
    }
  }

  "intervalDaysTables" should {
    "return correct tables" in {
      val from = ZonedDateTime.of(2020, 1, 10, 12, 0, 0, 0, DateTimeUtils.DefaultZoneId)
      val to = ZonedDateTime.of(2020, 1, 12, 15, 44, 0, 0, DateTimeUtils.DefaultZoneId)
      val tables = YtFolderSupport.intervalDaysTables(TimeInterval(from, to))
      tables.sorted shouldBe List("2020-01-10", "2020-01-11", "2020-01-12")
    }

    "return one table for one day interval" in {
      val from = ZonedDateTime.of(2020, 1, 10, 12, 0, 0, 0, DateTimeUtils.DefaultZoneId)
      val to = ZonedDateTime.of(2020, 1, 10, 15, 44, 0, 0, DateTimeUtils.DefaultZoneId)
      val tables = YtFolderSupport.intervalDaysTables(TimeInterval(from, to))
      tables shouldBe List("2020-01-10")
    }
  }

  "intervalHoursTables" should {
    "return correct tables" in {
      val from = ZonedDateTime.of(2020, 1, 10, 12, 0, 0, 0, DateTimeUtils.DefaultZoneId)
      val to = ZonedDateTime.of(2020, 1, 10, 15, 44, 0, 0, DateTimeUtils.DefaultZoneId)
      val tables = YtFolderSupport.intervalHoursTables(TimeInterval(from, to))
      tables.sorted shouldBe List(
        "2020-01-10T12:00:00",
        "2020-01-10T13:00:00",
        "2020-01-10T14:00:00",
        "2020-01-10T15:00:00"
      )
    }

    "return one table for one hour interval" in {
      val from = ZonedDateTime.of(2020, 1, 10, 12, 0, 0, 0, DateTimeUtils.DefaultZoneId)
      val to = ZonedDateTime.of(2020, 1, 10, 12, 1, 0, 0, DateTimeUtils.DefaultZoneId)
      val tables = YtFolderSupport.intervalHoursTables(TimeInterval(from, to))
      tables shouldBe List("2020-01-10T12:00:00")
    }
  }
}
