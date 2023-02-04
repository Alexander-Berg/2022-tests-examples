package ru.yandex.realty.mortgages.matchers

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.TryValues._
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import realty.palma.MortgageProgramConditionOuterClass.IfCondition
import realty.palma.MortgageProgramConditionOuterClass.IfConditionOperatorTypeNamespace.IfConditionOperatorType
import realty.palma.MortgageProgramConditionOuterClass.MortgageParamTypeNamespace.MortgageParamType
import ru.yandex.extdata.core.gens.Producer._
import ru.yandex.realty.mortgages.MortgageProgramSpecUtils._
import ru.yandex.realty.mortgages.mortgage.matchers.MortgageProgramDownPaymentMatcher

@RunWith(classOf[JUnitRunner])
class MortgageProgramDownPaymentMatcherSpec extends WordSpec with MockFactory with Matchers {

  import MortgageProgramDownPaymentMatcherSpec._

  private val matcher = MortgageProgramDownPaymentMatcher

  private val matchTestCases = Seq(
    TestCase(
      conditionOperator = IfConditionOperatorType.LESS_OR_EQUAL,
      conditionDownPaymentPercents = 10.75f,
      mortgageProgramMinDownPaymentPercents = Some(10.75f)
    ),
    TestCase(
      conditionOperator = IfConditionOperatorType.EQUAL,
      conditionDownPaymentPercents = 10.75f,
      mortgageProgramMinDownPaymentPercents = Some(10.75f)
    )
  )

  private val nonMatchTestCases = Seq(
    TestCase(
      conditionOperator = IfConditionOperatorType.LESS,
      conditionDownPaymentPercents = 10.75f,
      mortgageProgramMinDownPaymentPercents = Some(10.75f)
    ),
    TestCase(
      conditionOperator = IfConditionOperatorType.EQUAL,
      conditionDownPaymentPercents = 10.75f,
      mortgageProgramMinDownPaymentPercents = Some(13.15f)
    )
  )

  "MortgageProgramDownPaymentMatcher" should {

    matchTestCases.zipWithIndex.foreach {
      case (testCase, i) =>
        testCase match {
          case TestCase(
              conditionOperator,
              conditionDownPayment,
              mortgageProgramMinDownPayment
              ) =>
            s"match mortgage program when ranges are intersected #$i" in {
              val mortgageProgram = mortgageProgramGen(
                minDownPaymentPercents = mortgageProgramMinDownPayment
              ).next
              val condition = buildCondition(conditionOperator, downPayment = conditionDownPayment)

              val result = matcher.doMatch(asMortgageProgramContext(mortgageProgram), condition)

              result.success.value should be(true)
            }
        }
    }

    nonMatchTestCases.zipWithIndex.foreach {
      case (testCase, i) =>
        testCase match {
          case TestCase(
              conditionOperator,
              conditionDownPayment,
              mortgageProgramMinDownPayment
              ) =>
            s"not match mortgage program when ranges are not intersected #$i" in {
              val mortgageProgram = mortgageProgramGen(
                minDownPaymentPercents = mortgageProgramMinDownPayment
              ).next
              val condition = buildCondition(conditionOperator, downPayment = conditionDownPayment)

              val result = matcher.doMatch(asMortgageProgramContext(mortgageProgram), condition)

              result.success.value should be(false)
            }
        }
    }

  }

  private case class TestCase(
    conditionOperator: IfConditionOperatorType,
    conditionDownPaymentPercents: Float,
    mortgageProgramMinDownPaymentPercents: Option[Float] = None
  )

}

object MortgageProgramDownPaymentMatcherSpec {

  def buildCondition(operatorType: IfConditionOperatorType, downPayment: Float): IfCondition = {
    buildIfCondition(MortgageParamType.DOWN_PAYMENT, operatorType, downPayment)
  }

}
