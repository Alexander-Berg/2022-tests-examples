package ru.yandex.vertis.punisher.model

import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.moderation.proto.Model.Reason
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.model.Rule.RuleWithContext
import ru.yandex.vertis.punisher.model.RuleTree.Implicits._
import ru.yandex.vertis.punisher.model.RuleTreeSpec._

/**
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class RuleTreeSpec extends BaseSpec {

  private type U = AutoruUser
  private type C = TaskContext.Batch

  private val userCluster: UserCluster[U] = mock[UserCluster[U]]
  implicit private val taskContext: C = mock[C]

  private val (matchedRule1, matchedVerdict1) = nextRule(shouldPunish = true)
  private val (matchedRule2, matchedVerdict2) = nextRule(shouldPunish = true)
  private val (matchedRule3, matchedVerdict3) = nextRule(shouldPunish = true)
  private val (matchedRule4, matchedVerdict4) = nextRule(shouldPunish = true)
  private val (mismatchedRule1, mismatchedVerdict1) = nextRule(shouldPunish = false)
  private val (mismatchedRule2, mismatchedVerdict2) = nextRule(shouldPunish = false)
  private val (mismatchedRule3, mismatchedVerdict3) = nextRule(shouldPunish = false)

  private val testCases: Seq[TestCase[U, C]] =
    Seq(
      TestCase(
        description = "apply only first matched verdict for rule chain",
        ruleTree = mismatchedRule1 ~> matchedRule1 ~> matchedRule2,
        expectedVerdicts = Seq(matchedVerdict1)
      ),
      TestCase(
        description = "apply all verdicts for rule set",
        ruleTree = matchedRule1 | mismatchedRule1 | matchedRule2,
        expectedVerdicts = Seq(matchedVerdict1, mismatchedVerdict1, matchedVerdict2)
      ),
      TestCase(
        description = "apply more complex rule group as well (1)",
        ruleTree = (matchedRule1 ~> matchedRule2 | mismatchedRule1) | (matchedRule3 ~> matchedRule4),
        expectedVerdicts = Seq(matchedVerdict1, mismatchedVerdict1, matchedVerdict3)
      ),
      TestCase(
        description = "apply more complex rule group as well (2)",
        ruleTree = (mismatchedRule1 | mismatchedRule2) ~> (mismatchedRule3 | matchedRule1) ~> matchedRule2,
        expectedVerdicts = Seq(mismatchedVerdict3, matchedVerdict1)
      )
    )

  "RuleTree" should {
    testCases.foreach { case TestCase(description, ruleTree, expectedVerdicts) =>
      description in {
        ruleTree.apply(userCluster) shouldBe expectedVerdicts
      }
    }
  }

  private def nextRule(shouldPunish: Boolean): (RuleTree[U, C], Verdict[U]) = {
    val rule: RuleWithContext[U, C] = mock[RuleWithContext[U, C]]
    val rulePunishment =
      if (shouldPunish) {
        Some(RulePunishment(rule, AutoruPunishment.Ban(Reason.DO_NOT_EXIST, None)))
      } else {
        None
      }
    val verdict = spy(Verdict(userCluster, rulePunishment))
    when(rule.apply(userCluster)(taskContext.asInstanceOf[rule.Context])).thenReturn(verdict)
    (rule.tree, verdict)
  }
}

object RuleTreeSpec {

  case class TestCase[T <: User, C <: TaskContext](description: String,
                                                   ruleTree: RuleTree[T, C],
                                                   expectedVerdicts: Seq[Verdict[T]]
                                                  )

}
