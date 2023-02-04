package ru.yandex.vertis.moderation.rule.service

import org.joda.time.DateTime
import ru.yandex.vertis.moderation.SpecBase
import DateTimeInterpreter.Context
import ru.yandex.vertis.moderation.rule.service.DateTimeInterpreterSpec.{FailureTestCase, SuccessTestCase, TestCase}
import ru.yandex.vertis.moderation.util.DateTimeUtil

import scala.util.Success

/**
  * @author potseluev
  */
class DateTimeInterpreterSpec extends SpecBase {

  private val interpreter = DateTimeInterpreter

  private val testCases: Seq[TestCase] =
    Seq(
      SuccessTestCase(
        expression = "$now+ 10min",
        expectedResult = _.currentTime.plusMinutes(10)
      ),
      SuccessTestCase(
        expression = "$now-1h",
        expectedResult = _.currentTime.minusHours(1)
      ),
      SuccessTestCase(
        expression = "$now  +   3d",
        expectedResult = _.currentTime.plusDays(3)
      ),
      SuccessTestCase(
        expression = "123",
        expectedResult = _ => DateTimeUtil.fromMillis(123)
      ),
      SuccessTestCase(
        expression = "$now + 0s",
        expectedResult = _.currentTime
      ),
      FailureTestCase("now + 1s"),
      FailureTestCase("$now * 1s"),
      FailureTestCase("$now"),
      FailureTestCase("$now + 10")
    )

  "DateTimeInterpreter" should {
    testCases.foreach { testCase =>
      testCase.description in {
        testCase match {
          case test @ SuccessTestCase(expression, expectedResult) =>
            interpreter(expression)(test.ctx) shouldBe Success(expectedResult(test.ctx))
          case FailureTestCase(expression) =>
            interpreter(expression) should be('failure)
        }
      }
    }
  }
}

object DateTimeInterpreterSpec {

  sealed trait TestCase {
    def description: String
  }

  case class SuccessTestCase(expression: String, expectedResult: Context => DateTime) extends TestCase {
    val ctx: Context = Context()

    override def description: String = s"interpret $expression correctly"
  }

  case class FailureTestCase(expression: String) extends TestCase {
    override def description: String = s"fail to interpret $expression"
  }

}
