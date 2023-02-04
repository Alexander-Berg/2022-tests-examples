package ru.yandex.realty.canonical.base.params

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json._
import ru.yandex.realty.canonical.base.params.ParameterType.ParameterType
import ru.yandex.realty.canonical.base.params.ParametersRegistry.UnexpectedValueException

import scala.util._

@RunWith(classOf[JUnitRunner])
class ParamRegistrySpec extends WordSpec with Matchers {

  private val MaxTries = 3

  private def fixupValueByError(error: UnexpectedValueException): JsValue =
    error match {
      case _: ParametersRegistry.YesNoExpectedException =>
        JsString("YES")
      case ParametersRegistry.EnumValueExpectedException(expected, _) =>
        if (expected.isEmpty) fail(s"Empty expected list!")
        withClue(s"using one of expected ${expected.mkString(", ")}") {
          JsString(expected.minBy(_.length))
        }
      case _: ParametersRegistry.LongExpectedException | _: ParametersRegistry.IntExpectedException =>
        JsString(1.toString)
      case _: ParametersRegistry.StringExpectedException =>
        JsString("")
      case _: ParametersRegistry.FinishedValueExpected =>
        JsString("FINISHED")
    }

  private def validateParameter(pt: ParameterType): Unit = {
    def validateInternal(pt: ParameterType, jsValue: JsValue, triesLeft: Int): Unit = {
      if (triesLeft <= 0) fail(s"Tries limit exceeded! Registry is broken!")

      ParametersRegistry.parseWithType(pt, jsValue) match {
        case Failure(exception: UnexpectedValueException) =>
          val fixedValue = fixupValueByError(exception)
          withClue(s"with fixed value `$fixedValue`") {
            validateInternal(pt, fixedValue, triesLeft - 1)
          }
        case Failure(e) =>
          fail(s"Registry is broken!", e)
        case Success(value) =>
          value.`type` shouldBe pt
      }

    }

    validateInternal(pt, JsString(""), MaxTries)
  }

  "ParamRegistry" should {
    "be defined for all parameters" in {
      ParameterType.values
        .foreach { pt =>
          ParametersRegistry.registry.keys should contain(pt)
        }
    }

    "be correctly configured" in {
      ParameterType.values.foreach { pt =>
        withClue(s"on $pt") {
          validateParameter(pt)
        }
      }
    }
  }
}
