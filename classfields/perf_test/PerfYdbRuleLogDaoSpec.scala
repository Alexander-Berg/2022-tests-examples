package ru.yandex.vertis.billing.howmuch.storage.ydb.perf_test

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
import zio.ZIO
import zio.ZIO.{fail, foreachParN_, foreach_}
import zio.clock.Clock
import zio.duration.Duration
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.environment.TestEnvironment
import zio.test._

import java.time.Instant

object PerfYdbRuleLogDaoSpec extends DefaultRunnableSpec {

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

  private val testFrom = Instant.parse("2021-03-26T11:40:00Z")

  private val testRules =
    (1 to 20000).view
      .map { i =>
        Rule(
          testKey,
          Interval(testFrom.plusSeconds(i), Some(testFrom.plusSeconds(i + 1))),
          Kopecks(20000 + i),
          Source.StartrekTicket("VSMONEY-2750")
        )
      }
      .grouped(1000)
      .map(_.toList)
      .map(NonEmptyList.fromListUnsafe)
      .to(Iterable)

  override def spec: ZSpec[TestEnvironment, Any] = suite("YdbRuleLogDaoSpec")(
    testM("select returns rule fast even if it has large history") {
      for {
        _ <- foreach_(testRules)(rules => runTx(RuleLogDao.upsert(rules)))
        _ <- foreachParN_(10)(1 to 200) { _ =>
          for {
            (duration, actual) <- runTx(
              RuleLogDao.select(NonEmptyList.one(testKey), testFrom.plusSeconds(100000))
            ).timed
            _ <-
              fail(
                TestTimeoutException(s"Can't select rule in one second, actual duration = ${duration.toMillis} ms")
              ).when(duration.toMillis > 1000)
          } yield actual
        }
      } yield assertCompletes
    }
  )
    .provideCustomLayerShared {
      TestYdb.ydb >+> YdbRuleLogDao.live ++ Ydb.txRunner ++ Clock.live
    }
}
