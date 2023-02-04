package infra.feature_toggles.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import infra.feature_toggles.model.pagination.LimitOffset
import infra.feature_toggles.model.FeatureToggle
import infra.feature_toggles.model.testkit.Gen
import infra.feature_toggles.storage.HistoryDao
import infra.feature_toggles.storage.ydb.YdbHistoryDao
import zio.ZIO
import zio.clock.Clock
import zio.test.TestAspect.{sequential, shrinks}
import zio.test.{Gen => _, _}

import java.time.Instant
import java.time.temporal.ChronoUnit

object HistoryDaoSpec extends DefaultRunnableSpec {

  def now = Instant.now.truncatedTo(ChronoUnit.MICROS)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("HistoryToggleDao")(
      testM("Возвращает историю изменений всех фича-флагов сервиса") {
        checkNM(1)(Gen.service, Gen.value, Gen.value) { (service, value1, value2) =>
          val now_ = now
          val history =
            List(
              FeatureToggle("key1", Some(value1), now_, 123, "123"),
              FeatureToggle("key2", Some(value1), now_, 456, "456"),
              FeatureToggle("key2", None, now_, 789, "789"),
              FeatureToggle("key3", Some(value2), now_, 789, "789")
            )

          for {
            _ <- ZIO.foreach_(history.zipWithIndex) { case (ft, version) =>
              TestYdb.runTx(HistoryDao.insert(service, ft, version))
            }
            _ <- TestYdb.runTx(
              HistoryDao.insert("service", FeatureToggle("key", Some(value1), now_, 123, "desc"), 0)
            )
            actual <- TestYdb.runTx(HistoryDao.list(service, LimitOffset(3, 0)))
            count <- TestYdb.runTx(HistoryDao.count(service))
          } yield assertTrue(actual == history.reverse.take(3)) && assertTrue(count == 4)
        }
      }
    ) @@ shrinks(0) @@ sequential).provideCustomLayerShared {
      TestYdb.ydb >>> (YdbHistoryDao.live ++ Ydb.txRunner) ++ Clock.live
    }
  }
}
