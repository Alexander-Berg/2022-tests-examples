package vertis.statist.model

import org.joda.time.{DateTime, LocalDate}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import vertis.statist.model.DatesPeriod.merge

/** @author reimai
  */
class DatesPeriodSpec extends AnyWordSpec with Matchers {

  "DatesPeriod" should {
    "be splitted by months" in {
      monthSplit(
        from = "2022-05-01",
        until = "2022-08-07",
        expected = Seq(
          "2022-05-01" -> "2022-06-01",
          "2022-06-01" -> "2022-07-01",
          "2022-07-01" -> "2022-08-01",
          "2022-08-01" -> "2022-08-07"
        )
      )
    }
    "be splitted by month if less than month" in {
      monthSplit(from = "2022-02-21", until = "2022-02-24", expected = Seq("2022-02-21" -> "2022-02-24"))
    }

    "split with time" in {
      monthSplit(
        from = "2022-02-21T09:19:59.000",
        until = "2022-03-21",
        expected = Seq("2022-02-21T09:19:59.000" -> "2022-03-01", "2022-03-01" -> "2022-03-21")
      )
    }

    "fail on illegal period" in {
      intercept[IllegalArgumentException](monthSplit(from = "2022-01-01", until = "2021-01-01", expected = Seq.empty))
    }

    "merge period with no start date" in {
      merge(Seq(toPeriod("", "2022-03-02"), toPeriod("2022-01-02", "2022-04-12"))) shouldBe
        toPeriod("", "2022-04-12")
    }

    "merge period with no end date" in {
      merge(Seq(toPeriod("2022-03-02", ""), toPeriod("2022-01-02", "2022-04-12"))) shouldBe
        toPeriod("2022-01-02", "")
    }

    "merge into open period" in {
      merge(Seq(toPeriod("2022-03-02", ""), toPeriod("", "2022-04-12"))) shouldBe
        toPeriod("", "")
    }

    "not include until date" in {
      val period = toPeriod("2022-05-25", "2022-05-30")
      period.contains(LocalDate.parse("2022-05-25")) shouldBe true
      period.contains(LocalDate.parse("2022-05-29")) shouldBe true
      period.contains(LocalDate.parse("2022-05-30")) shouldBe false
    }
  }

  private def optDT(str: String): Option[DateTime] =
    Option(str).filter(_.nonEmpty).map(DateTime.parse)

  private def toPeriod(from: String, until: String): DatesPeriod =
    DatesPeriod(optDT(from), optDT(until))

  private def monthSplit(from: String, until: String, expected: Seq[(String, String)]) = {
    DatesPeriod.byMonths(DateTime.parse(from), DateTime.parse(until)) shouldBe expected.map((toPeriod _).tupled)
  }

}
