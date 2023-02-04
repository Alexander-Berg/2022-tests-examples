package ru.yandex.realty.rent.backend.payment.periods

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.TestUtil
import ru.yandex.realty.rent.backend.payment.{ConditionPeriod, PaymentGeneratorTestHelper}
import ru.yandex.realty.rent.proto.model.payment.FullnessTypeNamespace.FullnessType
import ru.yandex.realty.rent.util.NowMomentProvider
import ru.yandex.realty.tracing.Traced

import scala.annotation.tailrec

@RunWith(classOf[JUnitRunner])
class RentPaymentPeriodGeneratorSpec extends AsyncSpecBase {
  import FullnessType._
  import TestUtil._
  import PaymentGeneratorTestHelper._

  "PaymentDatesGenerator" when {

    "endless condition period" should {

      "generate payments with payment day equal to rent start date" in {
        val cps = ConditionPeriodBuilder().next("2020-01-10").end()
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          period(cps.head, "2020-03-10", "2020-04-10"),
          period(cps.head, "2020-04-10", "2020-05-10")
        )
        check(cps, dt"2020-03-14", result)
      }

      "generate payments with payment day not equal to rent start date with tail payment inside month" in {
        val cps = ConditionPeriodBuilder().next("2020-01-10", paymentDayOfMonth = 25).end()
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-02-25", SHORT_INITIALLY_MOVED_PAYMENT_DAY_MONTH),
          period(cps.head, "2020-02-25", "2020-03-25"),
          period(cps.head, "2020-03-25", "2020-04-25")
        )
        check(cps, dt"2020-03-14", result)
      }

      "generate payments with payment day not equal to rent start date with tail payment on months bound" in {
        val cps = ConditionPeriodBuilder().next("2020-01-10", paymentDayOfMonth = 5).end()
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-05", SHORT_INITIALLY_MOVED_PAYMENT_DAY_MONTH),
          period(cps.head, "2020-03-05", "2020-04-05"),
          period(cps.head, "2020-04-05", "2020-05-05")
        )
        check(cps, dt"2020-03-14", result)
      }

      "generate payments with payment day equal to rent start date in month end" in {
        val cps = ConditionPeriodBuilder().next("2020-01-31").end()
        val result = List(
          period(cps.head, "2020-01-31", "2020-02-29"),
          period(cps.head, "2020-02-29", "2020-03-31"),
          period(cps.head, "2020-03-31", "2020-04-30"),
          period(cps.head, "2020-04-30", "2020-05-31")
        )
        check(cps, dt"2020-04-04", result)
      }

      "generate payments with payment day not equal to rent start date in month end" in {
        val cps = ConditionPeriodBuilder().next("2020-01-31", paymentDayOfMonth = 1).end()
        val result = List(
          period(cps.head, "2020-01-31", "2020-02-29"),
          period(cps.head, "2020-02-29", "2020-03-01", SHORT_INITIALLY_MOVED_PAYMENT_DAY_MONTH),
          period(cps.head, "2020-03-01", "2020-04-01"),
          period(cps.head, "2020-04-01", "2020-05-01")
        )
        check(cps, dt"2020-03-04", result)
      }
    }

    "multiple condition periods" should {

      "generate payment with moved payment day with tail payment inside month" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-01-10")
          .next("2020-03-10", paymentDayOfMonth = 25)
          .end()
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          period(cps.last, "2020-03-10", "2020-03-25", SHORT_MOVED_PAYMENT_DAY_OF_MONTH),
          period(cps.last, "2020-03-25", "2020-04-25"),
          period(cps.last, "2020-04-25", "2020-05-25")
        )
        check(cps, dt"2020-04-10", result)
      }

      "generate payment with moved payment day with tail payment on months bound" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-01-10")
          .next("2020-03-10", paymentDayOfMonth = 5)
          .end()
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          period(cps.last, "2020-03-10", "2020-04-05", SHORT_MOVED_PAYMENT_DAY_OF_MONTH),
          period(cps.last, "2020-04-05", "2020-05-05"),
          period(cps.last, "2020-05-05", "2020-06-05")
        )
        check(cps, dt"2020-04-10", result)
      }
    }

    "termination" when {

      "termination date in end of the period" in {
        val cps = ConditionPeriodBuilder().next("2020-01-10").end("2020-05-10")
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          period(cps.head, "2020-03-10", "2020-04-10"),
          period(cps.head, "2020-04-10", "2020-05-10")
        )
        check(cps, dt"2020-04-14", result)
      }

      "termination date in middle of the unpaid period" in {
        val cps = ConditionPeriodBuilder().next("2020-01-10").end("2020-04-26")
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          period(cps.head, "2020-03-10", "2020-04-10"),
          period(cps.head, "2020-04-10", "2020-04-26", SHORT_TERMINATION)
        )
        check(cps, dt"2020-04-14", result)
      }

      "termination date in middle of initially short period" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-01-10", paymentDayOfMonth = 25)
          .end("2020-02-16")
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-02-16", SHORT_TERMINATION)
        )
        check(cps, dt"2020-03-10", result)
      }

      "termination date in the end of initially short period" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-01-10", paymentDayOfMonth = 25)
          .end("2020-02-25")
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-02-25", SHORT_TERMINATION)
        )
        check(cps, dt"2020-03-10", result)
      }
    }

    "termination and multiple condition periods" when {

      "termination date after payment with moved payment day in the end of full period" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-01-10")
          .next("2020-03-10", paymentDayOfMonth = 25)
          .end("2020-04-25")
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          period(cps.last, "2020-03-10", "2020-03-25", SHORT_MOVED_PAYMENT_DAY_OF_MONTH),
          period(cps.last, "2020-03-25", "2020-04-25")
        )
        check(cps, dt"2020-04-10", result)
      }

      "termination date after payment with moved payment day in the middle of full period" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-01-10")
          .next("2020-03-10", paymentDayOfMonth = 25)
          .end("2020-04-21")
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          period(cps.last, "2020-03-10", "2020-03-25", SHORT_MOVED_PAYMENT_DAY_OF_MONTH),
          period(cps.last, "2020-03-25", "2020-04-21", SHORT_TERMINATION)
        )
        check(cps, dt"2020-04-10", result)
      }

      "termination date after payment with moved payment day in the end of tail period" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-01-10")
          .next("2020-03-10", paymentDayOfMonth = 25)
          .end("2020-03-25")
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          period(cps.last, "2020-03-10", "2020-03-25", SHORT_TERMINATION)
        )
        check(cps, dt"2020-04-10", result)
      }

      "termination date after payment with moved payment day inside tail period" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-01-10")
          .next("2020-03-10", paymentDayOfMonth = 25)
          .end("2020-03-21")
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          period(cps.last, "2020-03-10", "2020-03-21", SHORT_TERMINATION)
        )
        check(cps, dt"2020-04-10", result)
      }
    }

    "broken payments" when {

      "existed payment in two periods at same time" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-01-10")
          .next("2020-02-25", paymentDayOfMonth = 25)
          .end()
        val pre = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10")
        )
        val periods = generateDates(cps, dt"2020-03-1", pre)
        periods shouldBe pre ::: List(
          period(cps.last, "2020-03-10", "2020-03-25", SHORT_MOVED_PAYMENT_DAY_OF_MONTH),
          period(cps.last, "2020-03-25", "2020-04-25")
        )
      }

      "unaligned payment after aligned payment" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-01-10")
          .end()
        val pre = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          period(cps.head, "2020-03-10", "2020-04-01")
        )
        val periods = generateDates(cps, dt"2020-03-30", pre)
        periods shouldBe pre ::: List(
          period(cps.last, "2020-04-01", "2020-04-10", SHORT_MOVED_PAYMENT_DAY_OF_MONTH),
          period(cps.last, "2020-04-10", "2020-05-10")
        )
      }

      "payment after end date" in {
        val cps = ConditionPeriodBuilder().next("2020-01-10").end("2020-04-01")
        val pre = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          period(cps.head, "2020-03-10", "2020-04-10")
        )
        val periods = generateDates(cps, dt"2020-04-05", pre)
        periods shouldBe pre
      }

    }
    "short termination" when {

      "owner allowed the check-out without additional payments" in {
        Array(true, false).foreach { flag =>
          val cps = ConditionPeriodBuilder()
            .next("2020-01-10")
            .end(
              endDate = "2020-03-21",
              notificationDate = "2020-03-15",
              checkOutWithoutAdditionalPayments = true,
              tenantRefusedPayFor30Days = flag
            )
          val result = List(
            period(cps.head, "2020-01-10", "2020-02-10"),
            period(cps.head, "2020-02-10", "2020-03-10"),
            period(cps.last, "2020-03-10", "2020-03-21", SHORT_TERMINATION)
          )
          check(cps, dt"2020-04-10", result)
        }
      }

      "tenant refused pay for 30 days" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-01-10")
          .end("2020-03-21", notificationDate = "2020-03-15", tenantRefusedPayFor30Days = true)
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          period(cps.last, "2020-03-10", "2020-03-21", SHORT_TERMINATION),
          period(cps.last, "2020-03-21", "2020-04-10", SHORT_TERMINATION),
          period(cps.last, "2020-04-10", "2020-04-15", SHORT_TERMINATION)
        )
        check(cps, dt"2020-04-10", result)
      }

      "tenant pay for 30 days" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-01-10")
          .end("2020-03-21", notificationDate = "2020-03-15")
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          period(cps.last, "2020-03-10", "2020-04-10"),
          period(cps.last, "2020-04-10", "2020-04-15", SHORT_TERMINATION)
        )
        check(cps, dt"2020-04-10", result)
      }

      "termination date in paid period" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-01-10")
          .end("2020-03-21", notificationDate = "2020-03-20") // оповещение в день сьезда
        val pre = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          period(cps.head, "2020-03-10", "2020-04-10")
        )
        val periods = generateDates(cps, dt"2020-04-20", pre)
        periods shouldBe pre ::: List(
          period(cps.head, "2020-04-10", "2020-04-20", SHORT_TERMINATION)
        )
      }

      "termination date in paid period and tenant refused pay for 30 days" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-01-10")
          .end("2020-03-21", notificationDate = "2020-03-20", tenantRefusedPayFor30Days = true) // оповещение в день сьезда
        val pre = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          period(cps.head, "2020-03-10", "2020-04-10")
        )
        val periods = generateDates(cps, dt"2020-04-20", pre)
        periods shouldBe pre ::: List(
          period(cps.head, "2020-04-10", "2020-04-20", SHORT_TERMINATION)
        )
      }

      "february" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-12-31")
          .end("2021-02-01", notificationDate = "2021-01-31") // оповещение в день сьезда
        val pre = List(
          period(cps.head, "2020-12-31", "2021-01-31")
        )
        val periods = generateDates(cps, dt"2021-04-20", pre)
        periods shouldBe pre ::: List(
          period(cps.head, "2021-01-31", "2021-02-28"),
          period(cps.head, "2021-02-28", "2021-03-03", SHORT_TERMINATION)
        )
      }

      "termination date before notification date and tenant refused pay for 30 days" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-01-10")
          .end("2020-03-10", notificationDate = "2020-03-30", tenantRefusedPayFor30Days = true)
        val result = List(
          period(cps.head, "2020-01-10", "2020-02-10"),
          period(cps.head, "2020-02-10", "2020-03-10"),
          //after end
          period(cps.head, "2020-03-10", "2020-04-10", SHORT_TERMINATION),
          period(cps.head, "2020-04-10", "2020-04-30", SHORT_TERMINATION)
        )
        check(cps, dt"2020-06-10", result)
      }

      "termination date(31) before notification date and tenant refused pay for 30 days" in {
        val cps = ConditionPeriodBuilder()
          .next("2020-10-31")
          .end("2021-02-10", notificationDate = "2021-04-20", tenantRefusedPayFor30Days = true)
        val result = List(
          period(cps.head, "2020-10-31", "2020-11-30"),
          period(cps.head, "2020-11-30", "2020-12-31"),
          period(cps.head, "2020-12-31", "2021-01-31"),
          period(cps.head, "2021-01-31", "2021-02-10", SHORT_TERMINATION),
          //after end
          period(cps.head, "2021-02-10", "2021-02-28", SHORT_TERMINATION),
          period(cps.head, "2021-02-28", "2021-03-31", SHORT_TERMINATION),
          period(cps.head, "2021-03-31", "2021-04-30", SHORT_TERMINATION),
          period(cps.head, "2021-04-30", "2021-05-21", SHORT_TERMINATION)
        )
        check(cps, dt"2021-06-10", result)
      }
    }
  }

  private def check(cps: List[ConditionPeriod], now: DateTime, result: List[RentPaymentPeriod]): Unit = {
    for (pre <- sublists(result)) {
      val periods = generateDates(cps, now, pre)
      periods shouldBe result
    }
  }

  //List(1, 2, 3) -> List(List(), List(1), List(1, 2), List(1, 2, 3))
  @tailrec
  private def sublists[T](list: List[T], res: List[List[T]] = List(Nil)): List[List[T]] = list match {
    case Nil => res.reverse
    case head :: tail => sublists(tail, (res.head :+ head) :: res)
  }

  private def period(
    cp: ConditionPeriod,
    start: String,
    end: String,
    fullnessType: FullnessType = FullnessType.FULL
  ): RentPaymentPeriod =
    GenericRentPaymentPeriod(dt(start), dt(end), fullnessType, cp)

  private def generateDates(
    periods: List[ConditionPeriod],
    now: DateTime,
    existing: List[RentPaymentPeriod] = Nil
  ): Seq[RentPaymentPeriod] = {
    val nowMomentProvider = NowMomentProvider(now)
    NewRentPaymentPeriodGenerator.generateDates(periods, existing)(Traced.empty, nowMomentProvider)
  }
}
