package ru.yandex.vertis.moderation.kafka.events.rules

import ru.yandex.vertis.moderation.model.events.rules.RuleStatistics
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer

case class DetectorTestCase(description: String, added: Int, removed: Int, detectorConfidence: Option[Double]) {
  def genRulesStatisticsWithRuleInstanceKey: RuleStatistics = {
    val statistics = RuleStatisticsGen.next
    val ruleInstanceKey = RuleStatisticsRuleInstanceKeyGen.next
    statistics.copy(
      key = ruleInstanceKey,
      signalsSummary =
        statistics.signalsSummary.copy(
          added = added,
          deleted = removed
        )
    )
  }

  def genRulesStatisticsWithRuleKey: RuleStatistics = {
    val statistics = RuleStatisticsGen.next
    val ruleKey = RuleStatisticsRuleKeyGen.next
    statistics.copy(
      key = ruleKey,
      signalsSummary =
        statistics.signalsSummary.copy(
          added = added,
          deleted = removed
        )
    )
  }
}
