package ru.yandex.vertis.moderation.service

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.events.rules.RuleStatistics
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer._
import ru.yandex.vertis.moderation.util.DateTimeUtil.OrderedDateTime
import ru.yandex.vertis.moderation.util.{DateTimeUtil, Interval}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author potseluev
  */
trait ModerationRuleStatisticsServiceSpecBase extends SpecBase {

  import ModerationRuleStatisticsServiceSpecBase._

  implicit protected val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  protected def ruleStatisticsService: ModerationRuleStatisticsService

  "ModerationRuleStatisticService" should {

    "correctly save and get statistics" in {
      val ruleId = Gen.chooseNum(0, 100).next
      val statisticsGen = RuleStatisticsGen.map(_.copy(key = RuleStatistics.Key.Rule(ruleId)))
      val statisticsBatch =
        Seq(
          statisticsGen.next.copy(timeInterval = timeInterval(0, 10)),
          statisticsGen.next.copy(timeInterval = timeInterval(5, 20)),
          statisticsGen.next.copy(timeInterval = timeInterval(17, 30))
        )
      Future.traverse(statisticsBatch)(ruleStatisticsService.saveStatistics).futureValue

      val actual1 = ruleStatisticsService.getStatistics(ruleId, timeInterval(0, 25)).futureValue
      val expected1 = statisticsBatch.slice(0, 2).reduce(_ + _)
      actual1 shouldBe expected1

      val actual2 = ruleStatisticsService.getStatistics(ruleId, timeInterval(20, 40)).futureValue
      val expected2 = RuleStatistics.empty(RuleStatistics.Key.Rule(ruleId), timeInterval(20, 40))
      actual2 shouldBe expected2
    }

    "update statistics and time interval on duplicate (ruleId, timeFrom)" in {
      val ruleId = Gen.chooseNum(200, 300).next
      val statisticsGen = RuleStatisticsGen.map(_.copy(key = RuleStatistics.Key.Rule(ruleId)))
      val statistics = statisticsGen.next.copy(timeInterval = timeInterval(0, 10))
      ruleStatisticsService.saveStatistics(statistics).futureValue
      val statisticsUpdate = statisticsGen.next.copy(timeInterval = timeInterval(0, 50))
      ruleStatisticsService.saveStatistics(statisticsUpdate).futureValue
      val actual = ruleStatisticsService.getStatistics(ruleId, timeInterval(0, 50)).futureValue
      actual shouldBe statisticsUpdate
    }
  }
}

object ModerationRuleStatisticsServiceSpecBase {

  private def timeInterval(from: Long, to: Long): Interval[DateTime] = Interval(from, to).map(DateTimeUtil.fromMillis)
}
