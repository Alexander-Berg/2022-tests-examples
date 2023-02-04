package ru.yandex.vertis.billing.howmuch.storage.ydb.test

import billing.common_model.Project
import billing.common_model.Project.AUTORU
import cats.data.NonEmptyList
import common.time.Interval
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.billing.common.money.Kopecks
import ru.yandex.vertis.billing.howmuch.model.core._
import ru.yandex.vertis.billing.howmuch.model.core.criteria._
import ru.yandex.vertis.billing.howmuch.storage.RuleLogDao
import ru.yandex.vertis.billing.howmuch.storage.ydb.YdbRuleLogDao
import ru.yandex.vertis.ydb.zio.YdbZioWrapper
import zio.ZIO
import zio.test.environment.TestEnvironment
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object YdbRuleLogDaoSpec extends DefaultRunnableSpec {

  private val testKey =
    RuleKey(
      MatrixId(AUTORU, "call"),
      Context(
        List(
          Criteria(CriteriaKey("region_id"), CriteriaValue("10174")),
          Criteria(CriteriaKey("mark"), CriteriaValue("AUDI"))
        ),
        List(CriteriaFallback(CriteriaKey("model")))
      )
    )

  private val beforeTestFrom = Instant.parse("2021-03-25T11:40:00Z")
  private val testFrom = Instant.parse("2021-03-26T11:40:00Z")
  private val afterTestFrom = Instant.parse("2021-03-28T11:40:00Z")

  private val testInterval = Interval(testFrom, to = None)

  private val testRule = Rule(testKey, testInterval, Kopecks(20000), Source.StartrekTicket("VSMONEY-2750"))

  private val testTo = Instant.parse("2021-03-29T00:00:00.123Z")
  private val afterTestTo = Instant.parse("2021-04-01T00:00:00.124Z")
  private val testRuleWithTo = testRule.copy(interval = testRule.interval.copy(to = Some(testTo)))

  override def spec: ZSpec[TestEnvironment, Any] = (suite("YdbRuleLogDaoSpec")(
    testM("select doesn't return rule on empty db") {
      for {
        result <- runTx(RuleLogDao.select(NonEmptyList.of(testKey), activeAt = afterTestFrom))
      } yield assert(result)(isEmpty)
    },
    testM("select returns upserted rule") {
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRule)))
        result <- runTx(RuleLogDao.select(NonEmptyList.of(testKey), activeAt = afterTestFrom))
      } yield assert(result)(equalTo(List(testRule)))
    },
    testM("select returns upserted rule created by service request") {
      val serviceRule = testRule.copy(source = Source.ServiceRequest("auction_auto_strategy"))
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(serviceRule)))
        result <- runTx(RuleLogDao.select(NonEmptyList.of(testKey), activeAt = afterTestFrom))
      } yield assert(result)(equalTo(List(serviceRule)))
    },
    testM("select returns upserted rule if to isn't empty") {
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRuleWithTo)))
        result <- runTx(RuleLogDao.select(NonEmptyList.of(testKey), activeAt = afterTestFrom))
      } yield assert(result)(equalTo(List(testRuleWithTo)))
    },
    testM("select returns upserted rule if activeAt == from") {
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRule)))
        result <- runTx(RuleLogDao.select(NonEmptyList.of(testKey), activeAt = testFrom))
      } yield assert(result)(equalTo(List(testRule)))
    },
    testM("select doesn't return upserted rule if activeAt < from") {
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRule)))
        result <- runTx(RuleLogDao.select(NonEmptyList.of(testKey), activeAt = beforeTestFrom))
      } yield assert(result)(isEmpty)
    },
    testM("upsert updates to") {
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRule)))
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRuleWithTo)))
        result <- runTx(RuleLogDao.select(NonEmptyList.of(testKey), activeAt = afterTestFrom))
      } yield assert(result)(equalTo(List(testRuleWithTo)))
    },
    testM("select doesn't return rule if activeAt == to") {
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRuleWithTo)))
        result <- runTx(RuleLogDao.select(NonEmptyList.of(testKey), activeAt = testTo))
      } yield assert(result)(isEmpty)
    },
    testM("select doesn't return rule if activeAt > to") {
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRuleWithTo)))
        result <- runTx(RuleLogDao.select(NonEmptyList.of(testKey), activeAt = afterTestTo))
      } yield assert(result)(isEmpty)
    },
    testM("select doesn't return rule if different project requested") {
      val otherKey = testKey.copy(matrixId = testKey.matrixId.copy(project = Project.REALTY))
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRule)))
        result <- runTx(RuleLogDao.select(NonEmptyList.of(otherKey), activeAt = afterTestFrom))
      } yield assert(result)(isEmpty)
    },
    testM("select doesn't return rule if different matrixId requested") {
      val otherKey = testKey.copy(matrixId = testKey.matrixId.copy(matrixId = "other"))
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRule)))
        result <- runTx(RuleLogDao.select(NonEmptyList.of(otherKey), activeAt = afterTestFrom))
      } yield assert(result)(isEmpty)
    },
    testM("select doesn't return rule if different context requested") {
      val otherKey = testKey.copy(
        context = testKey.context.copy(
          values = Criteria(CriteriaKey("region_id"), CriteriaValue("1")) :: testKey.context.values.tail
        )
      )
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRule)))
        result <- runTx(RuleLogDao.select(NonEmptyList.of(otherKey), activeAt = afterTestFrom))
      } yield assert(result)(isEmpty)
    },
    testM("upsert doesn't update initial rule if different project requested") {
      val otherKey = testKey.copy(matrixId = testKey.matrixId.copy(project = Project.REALTY))
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRule)))
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRuleWithTo.copy(key = otherKey))))
        result <- runTx(RuleLogDao.select(NonEmptyList.of(testKey), activeAt = afterTestFrom))
      } yield assert(result)(equalTo(List(testRule)))
    },
    testM("upsert doesn't update initial rule if different matrixId requested") {
      val otherKey = testKey.copy(matrixId = testKey.matrixId.copy(matrixId = "other"))
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRule)))
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRuleWithTo.copy(key = otherKey))))
        result <- runTx(RuleLogDao.select(NonEmptyList.of(testKey), activeAt = afterTestFrom))
      } yield assert(result)(equalTo(List(testRule)))
    },
    testM("upsert doesn't update initial rule if different context requested") {
      val otherKey = testKey.copy(
        context = testKey.context.copy(
          values = Criteria(CriteriaKey("region_id"), CriteriaValue("1")) :: testKey.context.values.tail
        )
      )
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRule)))
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRuleWithTo.copy(key = otherKey))))
        result <- runTx(RuleLogDao.select(NonEmptyList.of(testKey), activeAt = afterTestFrom))
      } yield assert(result)(equalTo(List(testRule)))
    },
    testM("upsert doesn't update initial rule if different `from` requested") {
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(testRule)))
        _ <- runTx(
          RuleLogDao.upsert(
            NonEmptyList.of(testRule.copy(interval = Interval(from = afterTestFrom.plusMillis(1), to = Some(testTo))))
          )
        )
        result <- runTx(RuleLogDao.select(NonEmptyList.of(testKey), activeAt = afterTestFrom))
      } yield assert(result)(equalTo(List(testRule)))
    },
    // этот тест упадёт, если select и upsert в dao сломались одновременно, в отличие от остальных тестов
    // и проверяет, что новые запросы обратно совместимы со старыми данными
    testM("select returns rule upserted via plain insert") {
      for {
        ydb <- ZIO.service[YdbZioWrapper]
        _ <- runTx(
          ydb.execute(
            """INSERT INTO rule_log (shard_id, project, matrix_id, context, from, price_kopecks, source, rule_id) VALUES
            |(1356711739, "AUTORU", "call", "region_id=10174&mark=AUDI&model->*", Timestamp("2021-03-26T11:40:00Z"),
            |20000, Json(@@{"startrek_ticket": {"id": "VSMONEY-2750"}}@@),
            |"a93272b7cef294270e8930d6472c1f9031960ac632d073a0c2574754e7eefc1a")""".stripMargin
          )
        )
        result <- runTx(RuleLogDao.select(NonEmptyList.of(testKey), activeAt = afterTestFrom))
      } yield assert(result)(equalTo(List(testRule)))
    }
  ) @@ sequential @@ before(runTx(YdbRuleLogDao.clean)))
    .provideCustomLayerShared {
      TestYdb.ydb >+> YdbRuleLogDao.live ++ Ydb.txRunner
    }
}
