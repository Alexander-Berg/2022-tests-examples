package ru.yandex.vertis.telepony.settings

import com.typesafe.config.{Config, ConfigFactory}
import pureconfig.ConfigSource
import ru.yandex.vertis.telepony.SpecBase
import pureconfig.generic.auto._
import ru.yandex.vertis.telepony.model.{AntiFraudOptions, BlockReasons}
import ru.yandex.vertis.telepony.settings.DomainBanNumbersSettings.{BanNumbersByOneTargetRule, BanNumbersByUniqueTargetsRule}

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._

class DomainBanNumbersRuleSpec extends SpecBase {

  val expectedBanNumbersByUniqueTargetsRule = BanNumbersByUniqueTargetsRule(
    limit = 5,
    window = 10.minutes,
    banInterval = 10.minutes,
    reason = BlockReasons.ManyCallsIn10min,
    antiFraudOption = AntiFraudOptions.CallsCounter
  )

  val expectedBanNumbersByOneTargetRule = BanNumbersByOneTargetRule(
    limit = 10,
    window = 10.minutes,
    banInterval = 10.minutes,
    reason = BlockReasons.ManyCallsIn10min,
    antiFraudOption = AntiFraudOptions.CallsCounter
  )

  val expectedConfigs = Seq(expectedBanNumbersByUniqueTargetsRule, expectedBanNumbersByOneTargetRule)

  val banNumbersByUniqueTargetsRuleMap = Map(
    "type" -> "ban-numbers-by-unique-targets-rule",
    "limit" -> expectedBanNumbersByUniqueTargetsRule.limit.toString,
    "window" -> expectedBanNumbersByUniqueTargetsRule.window.toString,
    "ban-interval" -> expectedBanNumbersByUniqueTargetsRule.banInterval.toString,
    "reason" -> expectedBanNumbersByUniqueTargetsRule.reason.toString,
    "anti-fraud-option" -> expectedBanNumbersByUniqueTargetsRule.antiFraudOption.toString
  )

  val banNumbersByOneTargetRuleMap = Map(
    "type" -> "ban-numbers-by-one-target-rule",
    "limit" -> expectedBanNumbersByOneTargetRule.limit.toString,
    "window" -> expectedBanNumbersByOneTargetRule.window.toString,
    "ban-interval" -> expectedBanNumbersByOneTargetRule.banInterval.toString,
    "reason" -> expectedBanNumbersByOneTargetRule.reason.toString,
    "anti-fraud-option" -> expectedBanNumbersByOneTargetRule.antiFraudOption.toString
  )

  "DomainBanNumbersSettings" should {
    "parse BanNumbersByUniqueTargetsRule" in {
      val testConfig: Config = ConfigFactory.parseMap(banNumbersByUniqueTargetsRuleMap.asJava)

      val parsed = ConfigSource.fromConfig(testConfig).loadOrThrow[BanNumbersByUniqueTargetsRule]
      parsed shouldBe expectedBanNumbersByUniqueTargetsRule
    }
    "parse BanNumbersByOneTargetRule" in {
      val testConfig: Config = ConfigFactory.parseMap(banNumbersByOneTargetRuleMap.asJava)

      val parsed = ConfigSource.fromConfig(testConfig).loadOrThrow[BanNumbersByOneTargetRule]
      parsed shouldBe expectedBanNumbersByOneTargetRule
    }
  }

}
