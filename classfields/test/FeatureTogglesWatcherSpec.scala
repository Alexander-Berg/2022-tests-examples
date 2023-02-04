package infra.feature_toggles.logic.test

import common.clients.grafana.testkit.TestGrafanaClient
import common.zio.logging.Logging
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import infra.feature_toggles.logic.{FeatureTogglesManager, FeatureTogglesWatcher}
import infra.feature_toggles.model.BooleanValue
import infra.feature_toggles.model.testkit.Gen
import infra.feature_toggles.storage.ydb.{YdbFeatureTogglesDao, YdbHistoryDao, YdbTelemetryDao}
import zio.duration._
import zio.magic._
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, shrinks}
import zio.test._
import zio.test.environment.TestClock
import zio.{Promise, ZLayer}

object FeatureTogglesWatcherSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("FeatureTogglesWatcher")(
      testM("Отслеживает изменения") {
        checkNM(1)(Gen.service, Gen.key, Gen.key) { case (service, key, key2) =>
          val value1 = BooleanValue(true)
          val value2 = BooleanValue(false)
          for {
            _ <- FeatureTogglesManager.set(service, key, value1, 1, "set 1")
            initialized <- Promise.make[Nothing, Unit]
            fiber <- FeatureTogglesWatcher.watch(service).take(2).tap(_ => initialized.succeed(())).runCollect.fork
            _ <- initialized.await // wait until watcher launched and initial value was retrieved
            _ <- FeatureTogglesManager.set(service, key, value2, 1, "set 2")
            _ <- FeatureTogglesManager.set(service, key2, value1, 1, "set 1")
            _ <- TestClock.adjust(1.second) // schedule next fetch
            versions <- fiber.join
            actualResults = versions.map(_.version).toList
            expectedResults = List(1, 3)
          } yield assert(actualResults)(equalTo(expectedResults))
        }
      }
    ) @@ shrinks(0) @@ sequential).injectSomeShared(
      TestYdb.ydb,
      YdbFeatureTogglesDao.live,
      YdbHistoryDao.live,
      YdbTelemetryDao.live,
      Ydb.txRunner,
      Logging.live,
      TestGrafanaClient.test,
      ZLayer.succeed(FeatureTogglesManager.Config(Nil)),
      FeatureTogglesManager.live,
      FeatureTogglesWatcher.live(100.milliseconds)
    )
  }
}
