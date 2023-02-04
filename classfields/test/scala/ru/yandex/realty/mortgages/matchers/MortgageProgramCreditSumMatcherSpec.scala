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
import ru.yandex.realty.mortgages.mortgage.matchers.MortgageProgramCreditSumMatcher

@RunWith(classOf[JUnitRunner])
class MortgageProgramCreditSumMatcherSpec extends WordSpec with MockFactory with Matchers {

  import MortgageProgramCreditSumMatcherSpec._

  private val matcher = MortgageProgramCreditSumMatcher

  private val matchTestCases = Seq(
    TestCase(
      conditionOperator = IfConditionOperatorType.LESS_OR_EQUAL,
      conditionCreditSum = 1000000,
      mortgageProgramMinCreditSum = Some(1000000)
    ),
    TestCase(
      conditionOperator = IfConditionOperatorType.LESS_OR_EQUAL,
      conditionCreditSum = 1000000,
      mortgageProgramMaxCreditSum = Some(1000000)
    ),
    TestCase(
      conditionOperator = IfConditionOperatorType.EQUAL,
      conditionCreditSum = 1000000,
      mortgageProgramMinCreditSum = Some(1000000),
      mortgageProgramMaxCreditSum = Some(1000000)
    )
  )

  private val nonMatchTestCases = Seq(
    TestCase(
      conditionOperator = IfConditionOperatorType.LESS,
      conditionCreditSum = 1000000,
      mortgageProgramMinCreditSum = Some(1000000)
    ),
    TestCase(
      conditionOperator = IfConditionOperatorType.GREATER,
      conditionCreditSum = 1000000,
      mortgageProgramMaxCreditSum = Some(1000000)
    ),
    TestCase(
      conditionOperator = IfConditionOperatorType.EQUAL,
      conditionCreditSum = 1000000,
      mortgageProgramMinCreditSum = Some(10000000),
      mortgageProgramMaxCreditSum = Some(30000000)
    ),
    TestCase(
      conditionOperator = IfConditionOperatorType.GREATER_OR_EQUAL,
      conditionCreditSum = 1000000,
      mortgageProgramMinCreditSum = Some(100000),
      mortgageProgramMaxCreditSum = Some(900000)
    )
  )

  "MortgageProgramCreditSumMatcher" should {

    matchTestCases.zipWithIndex.foreach {
      case (testCase, i) =>
        testCase match {
          case TestCase(
              conditionOperator,
              conditionCreditSum,
              mortgageProgramMinCreditSum,
              mortgageProgramMaxCreditSum
              ) =>
            s"match mortgage program when ranges are intersected #$i" in {
              val mortgageProgram = mortgageProgramGen(
                minCreditSum = mortgageProgramMinCreditSum,
                maxCreditSum = mortgageProgramMaxCreditSum
              ).next
              val condition = buildCondition(conditionOperator, creditSum = conditionCreditSum)

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
              conditionCreditSum,
              mortgageProgramMinCreditSum,
              mortgageProgramMaxCreditSum
              ) =>
            s"not match mortgage program when ranges are not intersected #$i" in {
              val mortgageProgram = mortgageProgramGen(
                minCreditSum = mortgageProgramMinCreditSum,
                maxCreditSum = mortgageProgramMaxCreditSum
              ).next
              val condition = buildCondition(conditionOperator, creditSum = conditionCreditSum)

              val result = matcher.doMatch(asMortgageProgramContext(mortgageProgram), condition)

              result.success.value should be(false)
            }
        }
    }

  }

  private case class TestCase(
    conditionOperator: IfConditionOperatorType,
    conditionCreditSum: Int,
    mortgageProgramMinCreditSum: Option[Long] = None,
    mortgageProgramMaxCreditSum: Option[Long] = None
  )

}

object MortgageProgramCreditSumMatcherSpec {

  def buildCondition(operatorType: IfConditionOperatorType, creditSum: Int): IfCondition = {
    buildIfCondition(MortgageParamType.CREDIT_SUM, operatorType, creditSum)
  }

}
