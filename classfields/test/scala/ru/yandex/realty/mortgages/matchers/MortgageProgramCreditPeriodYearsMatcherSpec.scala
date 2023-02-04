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
import ru.yandex.realty.mortgages.mortgage.matchers.MortgageProgramCreditPeriodYearsMatcher

@RunWith(classOf[JUnitRunner])
class MortgageProgramCreditPeriodYearsMatcherSpec extends WordSpec with MockFactory with Matchers {

  import MortgageProgramCreditPeriodYearsMatcherSpec._

  private val matcher = MortgageProgramCreditPeriodYearsMatcher

  private val matchTestCases = Seq(
    TestCase(
      conditionOperator = IfConditionOperatorType.LESS_OR_EQUAL,
      conditionPeriodYears = 11,
      mortgageProgramMinPeriodYears = Some(11)
    ),
    TestCase(
      conditionOperator = IfConditionOperatorType.LESS_OR_EQUAL,
      conditionPeriodYears = 11,
      mortgageProgramMaxPeriodYears = Some(11)
    ),
    TestCase(
      conditionOperator = IfConditionOperatorType.EQUAL,
      conditionPeriodYears = 11,
      mortgageProgramMinPeriodYears = Some(11),
      mortgageProgramMaxPeriodYears = Some(11)
    )
  )

  private val nonMatchTestCases = Seq(
    TestCase(
      conditionOperator = IfConditionOperatorType.LESS,
      conditionPeriodYears = 11,
      mortgageProgramMinPeriodYears = Some(11)
    ),
    TestCase(
      conditionOperator = IfConditionOperatorType.GREATER,
      conditionPeriodYears = 11,
      mortgageProgramMaxPeriodYears = Some(11)
    ),
    TestCase(
      conditionOperator = IfConditionOperatorType.EQUAL,
      conditionPeriodYears = 11,
      mortgageProgramMinPeriodYears = Some(12),
      mortgageProgramMaxPeriodYears = Some(13)
    ),
    TestCase(
      conditionOperator = IfConditionOperatorType.GREATER_OR_EQUAL,
      conditionPeriodYears = 11,
      mortgageProgramMinPeriodYears = Some(9),
      mortgageProgramMaxPeriodYears = Some(10)
    )
  )

  "MortgageProgramCreditPeriodYearsMatcher" should {

    matchTestCases.zipWithIndex.foreach {
      case (testCase, i) =>
        testCase match {
          case TestCase(
              conditionOperator,
              conditionPeriodYears,
              mortgageProgramMinPeriodYears,
              mortgageProgramMaxPeriodYears
              ) =>
            s"match mortgage program when ranges are intersected #$i" in {
              val mortgageProgram = mortgageProgramGen(
                minPeriodYears = mortgageProgramMinPeriodYears,
                maxPeriodYears = mortgageProgramMaxPeriodYears
              ).next
              val condition = buildCondition(conditionOperator, periodYears = conditionPeriodYears)

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
              conditionPeriodYears,
              mortgageProgramMinPeriodYears,
              mortgageProgramMaxPeriodYears
              ) =>
            s"not match mortgage program when ranges are not intersected #$i" in {
              val mortgageProgram = mortgageProgramGen(
                minPeriodYears = mortgageProgramMinPeriodYears,
                maxPeriodYears = mortgageProgramMaxPeriodYears
              ).next
              val condition = buildCondition(conditionOperator, periodYears = conditionPeriodYears)

              val result = matcher.doMatch(asMortgageProgramContext(mortgageProgram), condition)

              result.success.value should be(false)
            }
        }
    }

  }

  private case class TestCase(
    conditionOperator: IfConditionOperatorType,
    conditionPeriodYears: Int,
    mortgageProgramMinPeriodYears: Option[Int] = None,
    mortgageProgramMaxPeriodYears: Option[Int] = None
  )

}

object MortgageProgramCreditPeriodYearsMatcherSpec {

  def buildCondition(operatorType: IfConditionOperatorType, periodYears: Int): IfCondition = {
    buildIfCondition(MortgageParamType.CREDIT_PERIOD_YEARS, operatorType, periodYears)
  }

}
