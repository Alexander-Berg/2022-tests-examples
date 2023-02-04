package ru.yandex.vertis.billing.howmuch.storage.ydb.test

import billing.common_model.Project.AUTORU
import cats.data.NonEmptyList
import common.time.Interval
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.billing.common.money.Kopecks
import ru.yandex.vertis.billing.howmuch.model.core._
import ru.yandex.vertis.billing.howmuch.model.core.criteria._
import ru.yandex.vertis.billing.howmuch.storage.ActualRuleDao
import ru.yandex.vertis.billing.howmuch.storage.ydb.YdbActualRuleDao
import zio.clock.Clock
import zio.test.environment.TestEnvironment
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect.{before, sequential}

import java.time.Instant
import java.time.temporal.ChronoUnit.MICROS

object YdbActualRuleDaoSpec extends DefaultRunnableSpec {

  private val testKeys = NonEmptyList.of("call", "placement", "auction").map { matrixId =>
    RuleKey(
      MatrixId(AUTORU, matrixId),
      Context(
        List(
          Criteria(CriteriaKey("region_id"), CriteriaValue("10174")),
          Criteria(CriteriaKey("mark"), CriteriaValue("AUDI"))
        ),
        List(CriteriaFallback(CriteriaKey("model")))
      )
    )
  }

  private def testRules(from: Instant, to: Option[Instant]) =
    testKeys.map { testKey =>
      Rule(testKey, Interval(from, to), Kopecks(20000), Source.StartrekTicket("VSMONEY-2750"))
    }

  override def spec: ZSpec[TestEnvironment, Any] = (suite("YdbActualRuleDaoSpec")(
    testM("select returns all actual inserted rules") {
      for {
        now <- zio.clock.instant
        // YDB обрубает timestamp до микросекунд, поэтому здесь тоже обрубаем, чтобы hasSameElements сработал
        rules = testRules(from = now.minusSeconds(10).truncatedTo(MICROS), to = None)
        _ <- runTx(ActualRuleDao.upsert(rules))
        result <- ActualRuleDao.select(activeAt = now).runCollect
      } yield assert(result)(hasSameElements(rules.toList))
    },
    testM("select doesn't return rules from the past") {
      for {
        now <- zio.clock.instant
        rules = testRules(
          from = now.minusSeconds(60),
          to = Some(now.minusSeconds(10))
        )
        _ <- runTx(ActualRuleDao.upsert(rules))
        result <- ActualRuleDao.select(activeAt = now).runCollect
      } yield assert(result)(isEmpty)
    },
    testM("select doesn't return rules with to = activeAt") {
      for {
        now <- zio.clock.instant
        rules = testRules(
          from = now.minusSeconds(60),
          to = Some(now)
        )
        _ <- runTx(ActualRuleDao.upsert(rules))
        result <- ActualRuleDao.select(activeAt = now).runCollect
      } yield assert(result)(isEmpty)
    },
    testM("select doesn't return rule from the future") {
      for {
        now <- zio.clock.instant
        rules = testRules(from = now.plusSeconds(60000), to = None)
        _ <- runTx(ActualRuleDao.upsert(rules))
        result <- ActualRuleDao.select(activeAt = now).runCollect
      } yield assert(result)(isEmpty)
    }
  ) @@ sequential @@ before(runTx(YdbActualRuleDao.clean)))
    .provideCustomLayerShared {
      // передаём реальный Clock вместо тестового, чтобы случайно не попасть на TTL записей по to в YDB
      TestYdb.ydb >+> YdbActualRuleDao.live ++ Ydb.txRunner ++ Clock.live
    }
}
