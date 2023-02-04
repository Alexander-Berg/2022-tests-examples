package ru.auto.salesman.test

import org.joda.time.DateTime
import org.scalatest.matchers.{MatchResult, Matcher}
import scala.concurrent.duration._

trait CustomMatchers {

  class DateTimeToleranceMatcher(expected: DateTime) extends Matcher[DateTime] {
    import org.scalactic.Tolerance._

    def apply(left: DateTime): MatchResult =
      MatchResult(
        (expected.getMillis +- 1.minute.toMillis) === left.getMillis,
        s"$left not equals to $expected",
        s"$left equals to $expected"
      )
  }

  def ~=(expected: DateTime) = new DateTimeToleranceMatcher(expected)
}
