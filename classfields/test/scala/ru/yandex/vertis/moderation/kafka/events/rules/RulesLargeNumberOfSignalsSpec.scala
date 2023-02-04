package ru.yandex.vertis.moderation.kafka.events.rules

import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.feature.EmptyFeatureRegistry
import ru.yandex.vertis.moderation.kafka.events.rules.RulesSupervisorDecider.{Decision, DecisionInfo}

/**
  * @author molokovskikh
  */
class RulesLargeNumberOfSignalsSpec extends SpecBase {

  private val signalsThreshold = 500
  implicit val featureRegistry: FeatureRegistry = EmptyFeatureRegistry
  private val largeNumberOfSignalsDetector = new RulesLargeNumberOfSignalsDetector

  private val testCases: Seq[DetectorTestCase] =
    Seq(
      DetectorTestCase(
        description = "make empty decision if not enough signals",
        added = signalsThreshold - 1,
        removed = signalsThreshold - 100,
        detectorConfidence = None
      ),
      DetectorTestCase(
        description = "make confident decision.largeNumber about if added > threshold",
        added = signalsThreshold + 1,
        removed = signalsThreshold - 100,
        detectorConfidence = Some(1)
      ),
      DetectorTestCase(
        description = "make confident decision.largeNumber about if removed > threshold",
        added = signalsThreshold - 100,
        removed = signalsThreshold + 1,
        detectorConfidence = Some(1)
      )
    )

  "RulesLargeNumberOfSignalsDetector" should {
    testCases.foreach { case testCase @ DetectorTestCase(description, added, removed, detectorConfidence) =>
      description in {
        val actualDecision = largeNumberOfSignalsDetector(testCase.genRulesStatisticsWithRuleKey).futureValue
        val maxNumberOfSignals = math.max(added, removed)
        detectorConfidence match {
          case Some(confidence) =>
            actualDecision shouldBe Map(Decision.LargeNumberOfSignals(maxNumberOfSignals) -> DecisionInfo(confidence))
          case None => actualDecision shouldBe empty
        }
      }
    }
  }
}
