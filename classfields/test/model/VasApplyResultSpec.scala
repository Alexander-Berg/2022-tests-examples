package auto.dealers.multiposting.clients.avito.test.model

import auto.dealers.multiposting.clients.avito.model.{VasApplyResult, VasInfo}
import io.circe.parser._
import zio.test._
import zio.test.Assertion._
import zio.test.DefaultRunnableSpec

import java.time.OffsetDateTime

object VasApplyResultSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("VasApplyResult")(
    decodeProductWithNormalOffsetDateTime,
    decodeProductWithNormalOffsetDateTimeWithSchedule,
    decodeProductWithFallbackToLocalDateTime,
    decodeProductWillFail
  )

  val decodeProductWithNormalOffsetDateTime = test("parse normal OffsetDateTime") {
    val json =
      """
        | {
        |   "amount": 1.0,
        |   "vas": {
        |     "vas_id": "test",
        |     "finish_time": "2022-01-25T20:10:43.329600+03:00",
        |     "schedule": []
        |   }
        | }
        |""".stripMargin

    val decoded = decode[VasApplyResult](json)

    val expectedVasApplyResult = VasApplyResult(
      amount = 1.0,
      vas = VasInfo(
        vasId = "test",
        finishTime = OffsetDateTime.parse("2022-01-25T20:10:43.329600+03:00"),
        schedule = Seq.empty
      )
    )

    assert(decoded)(equalTo(Right(expectedVasApplyResult)))
  }

  val decodeProductWithNormalOffsetDateTimeWithSchedule = test("parse normal OffsetDateTime with schedule") {
    val json =
      """
        | {
        |   "amount": 1.0,
        |   "vas": {
        |     "vas_id": "test",
        |     "finish_time": "2022-01-25T20:10:43.329600+03:00",
        |     "schedule": ["2022-01-25T20:10:43.329600+03:00"]
        |   }
        | }
        |""".stripMargin

    val decoded = decode[VasApplyResult](json)

    val expectedVasApplyResult = VasApplyResult(
      amount = 1.0,
      vas = VasInfo(
        vasId = "test",
        finishTime = OffsetDateTime.parse("2022-01-25T20:10:43.329600+03:00"),
        schedule = Seq(OffsetDateTime.parse("2022-01-25T20:10:43.329600+03:00"))
      )
    )

    assert(decoded)(equalTo(Right(expectedVasApplyResult)))
  }

  val decodeProductWithFallbackToLocalDateTime =
    test("parse fallback to LocalDateTime and then convert to OffsetDateTime") {
      val json =
        """
        | {
        |   "amount": 1.0,
        |   "vas": {
        |     "vas_id": "test",
        |     "finish_time": "2022-01-25T20:10:43"
        |   }
        | }
        |""".stripMargin

      val decoded = decode[VasApplyResult](json)

      val expectedVasApplyResult = VasApplyResult(
        amount = 1.0,
        vas = VasInfo(
          vasId = "test",
          finishTime = OffsetDateTime.parse("2022-01-25T20:10:43Z"),
          schedule = Seq.empty
        )
      )

      assert(decoded)(equalTo(Right(expectedVasApplyResult)))
    }

  val decodeProductWillFail =
    test("parse and fail") {
      val json =
        """
        | {
        |   "amount": 1.0,
        |   "vas": {
        |     "vas_id": "test",
        |     "finish_time": "2022-01-25 20:10:43"
        |   }
        | }
        |""".stripMargin

      val decoded = decode[VasApplyResult](json)

      assert(decoded)(isLeft)
    }
}
