package ru.yandex.vertis.moderation.kafka.events.rules

import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.feature.EmptyFeatureRegistry
import ru.yandex.vertis.moderation.kafka.events.rules.RulesSupervisorDecider.{Decision, DecisionInfo}

/**
  * @author potseluev
  */
class RulesCycleDetectorSpec extends SpecBase {

  private val signalsThreshold = 100
  private val rulesCycleDetector: RulesSupervisorDecider =
    new RulesCycleDetector(signalsThreshold)(EmptyFeatureRegistry)

  private val testCases: Seq[DetectorTestCase] =
    Seq(
      DetectorTestCase(
        description = "make empty decision if not enough signals",
        added = signalsThreshold - 1,
        removed = signalsThreshold - 1,
        detectorConfidence = None
      ),
      DetectorTestCase(
        description = "make confident decision.cyclic about if added == removed",
        added = signalsThreshold,
        removed = signalsThreshold,
        detectorConfidence = Some(1)
      ),
      DetectorTestCase(
        description = "make assumption about decision.cyclic if added and removed are similar",
        added = signalsThreshold,
        removed = 2 * signalsThreshold,
        detectorConfidence = Some(0.5)
      )
    )

  "RulesCycleDetector" should {
    testCases.foreach { case testCase @ DetectorTestCase(description, _, _, cycleConfidence) =>
      description in {
        val actualDecision = rulesCycleDetector(testCase.genRulesStatisticsWithRuleInstanceKey).futureValue
        cycleConfidence match {
          case Some(confidence) => actualDecision shouldBe Map(Decision.Cyclic -> DecisionInfo(confidence))
          case None             => actualDecision shouldBe empty
        }
      }
    }
  }
}
