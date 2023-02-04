package ru.yandex.realty.seller.service.impl

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.proto.WeekDay
import ru.yandex.realty.seller.model.schedule.ProductScheduleItem
import ru.yandex.realty.seller.service.util.ScheduleUtils
import ru.yandex.vertis.util.time.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class ScheduleUtilsSpec extends SpecBase {

  def utilsWithNow(dateTime: DateTime): ScheduleUtils =
    new ScheduleUtils {
      override def getNow: DateTime = dateTime
    }

  private val DateFormatter =
    DateTimeFormat
      .forPattern("dd.MM.yy HH:mm")
      .withZone(DateTimeUtil.DefaultTimeZone)

  implicit private class RichString(s: String) {
    def toDate: DateTime = DateTime.parse(s, DateFormatter)
  }

  private val dummyDate = "02.01.20 10:00".toDate // четверг
    .withSecondOfMinute(13)
    .withMillisOfSecond(363)

  private def datesToSchedules(dates: Seq[String]): Seq[ProductScheduleItem] = {
    dates.map(_.toDate).map { st =>
      ProductScheduleItem(
        startTime = st,
        ScheduleUtils.AllDays
      )
    }
  }

  private val OnlyWeekEndsSchedule = Seq(
    ProductScheduleItem(
      startTime = "01.01.20 13:00".toDate,
      Seq(WeekDay.SATURDAY, WeekDay.SUNDAY)
    )
  )

  private val EveryDaySchedule = datesToSchedules(
    Seq(
      "01.01.20 10:00"
    )
  )

  private val utils = utilsWithNow(dummyDate)

  "Schedule utils" must {
    "correctly return time for now" in {
      utils.getTargetScheduleForNow(EveryDaySchedule) shouldBe "02.01.20 10:00".toDate
      // воскресенье
      utils.getTargetScheduleForNow(OnlyWeekEndsSchedule) shouldBe "29.12.19 13:00".toDate
    }

    "correctly return time after now" in {
      utils.getNextScheduleAfterNow(EveryDaySchedule) shouldBe Some("03.01.20 10:00".toDate)
      utils.getNextScheduleAfterNow(OnlyWeekEndsSchedule) shouldBe Some("04.01.20 13:00".toDate)
    }

  }

}
