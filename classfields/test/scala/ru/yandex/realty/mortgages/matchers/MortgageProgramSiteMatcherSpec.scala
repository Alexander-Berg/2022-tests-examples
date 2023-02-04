package ru.yandex.realty.mortgages.matchers

import java.util
import java.util.EnumSet.complementOf

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.TryValues._
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import realty.palma.MortgageProgramConditionOuterClass.IfCondition
import realty.palma.MortgageProgramConditionOuterClass.IfConditionOperatorTypeNamespace.IfConditionOperatorType
import realty.palma.MortgageProgramConditionOuterClass.IfConditionOperatorTypeNamespace.IfConditionOperatorType.{
  EQUAL,
  NOT_EQUAL,
  UNRECOGNIZED
}
import realty.palma.MortgageProgramConditionOuterClass.MortgageParamTypeNamespace.MortgageParamType
import ru.yandex.extdata.core.gens.Producer._
import ru.yandex.realty.mortgages.MortgageProgramSpecUtils._
import ru.yandex.realty.mortgages.mortgage.matchers.MortgageProgramSiteMatcher

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class MortgageProgramSiteMatcherSpec extends WordSpec with MockFactory with Matchers {

  import MortgageProgramSiteMatcherSpec._

  private val matcher = MortgageProgramSiteMatcher

  private val matchTestCases = Seq(
    TestCase(
      conditionOperator = EQUAL,
      conditionSites = Seq(13429),
      targetSiteId = 13429
    ),
    TestCase(
      conditionOperator = NOT_EQUAL,
      conditionSites = Seq(13428),
      targetSiteId = 13429
    ),
    TestCase(
      conditionOperator = EQUAL,
      conditionSites = Seq(57362, 13429, 45574),
      targetSiteId = 13429
    )
  )

  private val nonMatchTestCases = Seq(
    TestCase(
      conditionOperator = EQUAL,
      conditionSites = Seq.empty,
      targetSiteId = 13429
    ),
    TestCase(
      conditionOperator = EQUAL,
      conditionSites = Seq(23429, 33429),
      targetSiteId = 13429
    ),
    TestCase(
      conditionOperator = NOT_EQUAL,
      conditionSites = Seq(13429, 23429, 33429),
      targetSiteId = 13429
    )
  )

  private val unsupportedOperators: Seq[IfConditionOperatorType] =
    complementOf(util.EnumSet.of(EQUAL, NOT_EQUAL)).asScala.filterNot(_ == UNRECOGNIZED).toSeq

  "MortgageProgramSiteMatcher" should {

    matchTestCases.zipWithIndex.foreach {
      case (testCase, i) =>
        testCase match {
          case TestCase(
              conditionOperator,
              conditionSites,
              targetSiteId
              ) =>
            s"match mortgage program when site is matched #$i" in {
              val mortgageProgram = mortgageProgramGen().next
              val condition = buildCondition(conditionOperator, conditionSites)

              val result =
                matcher.doMatch(asMortgageProgramContext(mortgageProgram, siteId = Some(targetSiteId)), condition)

              result.success.value should be(true)
            }
        }
    }

    nonMatchTestCases.zipWithIndex.foreach {
      case (testCase, i) =>
        testCase match {
          case TestCase(
              conditionOperator,
              conditionSites,
              targetSiteId
              ) =>
            s"not match mortgage program when site is not matched #$i" in {
              val mortgageProgram = mortgageProgramGen().next
              val condition = buildCondition(conditionOperator, conditionSites)

              val result =
                matcher.doMatch(asMortgageProgramContext(mortgageProgram, siteId = Some(targetSiteId)), condition)

              result.success.value should be(false)
            }
        }
    }

    unsupportedOperators.foreach { operator =>
      s"throw exception when operator is $operator" in {
        val mortgageProgram = mortgageProgramGen().next
        val condition = buildCondition(operator, Seq(45253))

        val result = matcher.doMatch(asMortgageProgramContext(mortgageProgram, siteId = Some(45253)), condition)

        result.failure.exception.getClass should be(classOf[UnsupportedOperationException])
      }
    }

  }

  private case class TestCase(
    conditionOperator: IfConditionOperatorType,
    conditionSites: Seq[Long],
    targetSiteId: Long
  )

}

object MortgageProgramSiteMatcherSpec {

  def buildCondition(operatorType: IfConditionOperatorType, conditionSites: Seq[Long]): IfCondition = {
    buildIfCondition(MortgageParamType.SITE_ID, operatorType, conditionSites)
  }

}
