package ru.yandex.vertis.moderation.util

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase

import scala.concurrent.duration._

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class DateTimeUtilSpec extends SpecBase {

  "parse" should {

    val positiveCases: Seq[(String, DateTime)] =
      Seq(
        ("1498991881000", new DateTime(1498991881000L, DateTimeUtil.DefaultZone)),
        ("2017-03-20T15:15:00.000+03:00", DateTime.parse("2017-03-20T15:15:00.000+03:00")),
        ("2017-03-21", DateTime.parse("2017-03-21T00:00:00.000+03:00"))
      )
    positiveCases.foreach { case (input, expectedResult) =>
      s"return correct value $input" in {
        val actualResult = DateTimeUtil.parse(input).get
        actualResult shouldBe expectedResult
      }
    }

    val negativeCases: Seq[String] =
      Seq(
        "2017-03-21+03:00",
        "2017-03-20T24:15:00.000+03:00"
      )
    negativeCases.foreach { input =>
      s"return error on invalid input: $input" in {
        val result = DateTimeUtil.parse(input)
        assertThrows[IllegalArgumentException](result.get)
      }
    }
  }

  "closestGreaterShiftByAnIntegerSteps" should {
    val now = DateTime.now()
    val step = 30.days

    val cases =
      Seq(
        ("shift datetime by one step from shiftFrom", now - 10.days, now + 10.days, now + 20.days),
        (
          "not shift datetime if there is exactly one step between datetime and shiftFrom",
          now - 20.days,
          now + 10.days,
          now + 10.days
        ),
        ("shift datetime by a few steps from shiftFrom", now - 20.days, now + 110.days, now + 130.days),
        (
          "not shift datetime if there is an integer count of steps between datetime and shiftFrom",
          now - 20.days,
          now + 100.days,
          now + 100.days
        ),
        ("not shift datetime if it equals to shiftFrom", now, now, now)
      )

    cases.foreach { case (description, shiftFrom, datetime, expected) =>
      description in {
        val result = DateTimeUtil.closestGreaterShiftByAnIntegerSteps(datetime, shiftFrom, step)
        result shouldBe expected
      }

    }

  }
}
