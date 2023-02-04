package ru.yandex.vertis.billing.task

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.KeyValueDao
import ru.yandex.vertis.billing.task.ForcedIntervalTask._
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.billing.util.DateTimeUtils.{now, IsoDateFormatter}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.{Failure, Success}

/**
  * Tests for [[ForcedIntervalTask]]
  *
  * @author alesavin
  */
class ForcedIntervalTaskSpec extends AnyWordSpec with Matchers with MockitoSupport {

  val taskName = "fake-task"

  val keyValueDao = {
    val m = mock[KeyValueDao]
    stub(m.getT _) {
      case TaskStartKey => Success("2015-02-28")
      case TaskEndKey => Success("2015-03-02")
      case TaskNameKey => Success(taskName)
    }
    m
  }

  val failMessage = "fail on 2015-03-01"

  val task = {
    val m = mock[Task]
    stub(m.execute(_: DateTimeInterval)) {
      case interval if interval == daily("2015-02-28") =>
        Success(())
      case interval if interval == daily("2015-03-01") =>
        Failure(throw new IllegalArgumentException(failMessage))
    }
    m
  }

  val ForcedStatisticsUpdate =
    new ForcedIntervalTask(keyValueDao, Map(taskName -> task)) with LoggedTask {
      override def taskName = "PrecisedStatisticsUpdate"
    }

  import DateTimeInterval._

  "ForcedStatisticsUpdateTask" should {
    "provide correct intervals list" in {
      val Now = now()
      val startOfNowDay =
        DateTimeInterval.dayIntervalFrom(Now.withTimeAtStartOfDay())

      intercept[IllegalArgumentException] {
        asDailyIntervals(Now, Now.minusDays(1))
      }
      intercept[IllegalArgumentException] {
        asDailyIntervals(Now.plusDays(1), Now)
      }

      asDailyIntervals(now(), now()).toList match {
        case `startOfNowDay` :: Nil => info("Done")
        case other => fail(s"Unexpected $other")
      }

      asDailyIntervals(Now.withTimeAtStartOfDay().minusDays(5), Now.withTimeAtStartOfDay()).size should be(6)
    }
    "provide correct intervals list for iso dates" in {
      asDailyIntervals(
        IsoDateFormatter.parseDateTime("2015-01-01"),
        IsoDateFormatter.parseDateTime("2015-01-01")
      ).size should be(
        1
      )
      asDailyIntervals(
        IsoDateFormatter.parseDateTime("2015-01-01"),
        IsoDateFormatter.parseDateTime("2015-01-02")
      ).size should be(
        2
      )
      asDailyIntervals(
        IsoDateFormatter.parseDateTime("2015-02-28"),
        IsoDateFormatter.parseDateTime("2015-03-01")
      ).size should be(
        2
      )
      asDailyIntervals(
        IsoDateFormatter.parseDateTime("2015-01-01"),
        IsoDateFormatter.parseDateTime("2015-03-01")
      ).size should be(
        60
      )
      asDailyIntervals(
        IsoDateFormatter.parseDateTime("2015-02-28"),
        IsoDateFormatter.parseDateTime("2015-03-02")
      ).toList should
        be(
          List(
            daily("2015-02-28"),
            daily("2015-03-01"),
            daily("2015-03-02")
          )
        )
      asDailyIntervals(
        IsoDateFormatter.parseDateTime("2014-12-30"),
        IsoDateFormatter.parseDateTime("2015-01-02")
      ).toList should
        be(
          List(
            daily("2014-12-30"),
            daily("2014-12-31"),
            daily("2015-01-01"),
            daily("2015-01-02")
          )
        )
    }
    "run with specified params" in {
      ForcedStatisticsUpdate.execute() match {
        case Failure(e) if e.getMessage == failMessage =>
          info("Done")
        case other => fail(s"Unexpected $other")
      }

    }
  }

  def daily(isoDate: String) =
    DateTimeInterval.dayIntervalFrom(IsoDateFormatter.parseDateTime(isoDate))
}
