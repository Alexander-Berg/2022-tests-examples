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
import ru.yandex.realty.mortgages.mortgage.matchers.MortgageProgramDeveloperMatcher

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class MortgageProgramDeveloperMatcherSpec extends WordSpec with MockFactory with Matchers {

  import MortgageProgramDeveloperMatcherSpec._

  private val matcher = MortgageProgramDeveloperMatcher

  private val matchTestCases = Seq(
    TestCase(
      conditionOperator = EQUAL,
      conditionDevelopers = Seq(5367346719L),
      targetDeveloperId = 5367346719L
    ),
    TestCase(
      conditionOperator = NOT_EQUAL,
      conditionDevelopers = Seq(5367346718L),
      targetDeveloperId = 5367346719L
    ),
    TestCase(
      conditionOperator = EQUAL,
      conditionDevelopers = Seq(956254596L, 5367346719L, 825596362L),
      targetDeveloperId = 5367346719L
    )
  )

  private val nonMatchTestCases = Seq(
    TestCase(
      conditionOperator = EQUAL,
      conditionDevelopers = Seq.empty,
      targetDeveloperId = 5367346719L
    ),
    TestCase(
      conditionOperator = EQUAL,
      conditionDevelopers = Seq(6367346719L, 7367346719L),
      targetDeveloperId = 5367346719L
    ),
    TestCase(
      conditionOperator = NOT_EQUAL,
      conditionDevelopers = Seq(5367346719L, 6367346719L, 7367346719L),
      targetDeveloperId = 5367346719L
    )
  )

  private val unsupportedOperators: Seq[IfConditionOperatorType] =
    complementOf(util.EnumSet.of(EQUAL, NOT_EQUAL)).asScala.filterNot(_ == UNRECOGNIZED).toSeq

  "MortgageProgramDeveloperMatcher" should {

    matchTestCases.zipWithIndex.foreach {
      case (testCase, i) =>
        testCase match {
          case TestCase(
              conditionOperator,
              conditionDevelopers,
              targetDeveloperId
              ) =>
            s"match mortgage program when developer is matched #$i" in {
              val mortgageProgram = mortgageProgramGen().next
              val condition = buildCondition(conditionOperator, conditionDevelopers)

              val result = matcher.doMatch(
                asMortgageProgramContext(mortgageProgram, developerId = Some(targetDeveloperId)),
                condition
              )

              result.success.value should be(true)
            }
        }
    }

    nonMatchTestCases.zipWithIndex.foreach {
      case (testCase, i) =>
        testCase match {
          case TestCase(
              conditionOperator,
              conditionDevelopers,
              targetDeveloperId
              ) =>
            s"not match mortgage program when developer is not matched #$i" in {
              val mortgageProgram = mortgageProgramGen().next
              val condition = buildCondition(conditionOperator, conditionDevelopers)

              val result = matcher.doMatch(
                asMortgageProgramContext(mortgageProgram, developerId = Some(targetDeveloperId)),
                condition
              )

              result.success.value should be(false)
            }
        }
    }

    unsupportedOperators.foreach { operator =>
      s"throw exception when operator is $operator" in {
        val mortgageProgram = mortgageProgramGen().next
        val condition = buildCondition(operator, Seq(45253))

        val result = matcher.doMatch(asMortgageProgramContext(mortgageProgram, developerId = Some(45253)), condition)

        result.failure.exception.getClass should be(classOf[UnsupportedOperationException])
      }
    }

  }

  private case class TestCase(
    conditionOperator: IfConditionOperatorType,
    conditionDevelopers: Seq[Long],
    targetDeveloperId: Long
  )

}

object MortgageProgramDeveloperMatcherSpec {

  def buildCondition(operatorType: IfConditionOperatorType, conditionDevelopers: Seq[Long]): IfCondition = {
    buildIfCondition(MortgageParamType.DEVELOPER_ID, operatorType, conditionDevelopers)
  }

}
