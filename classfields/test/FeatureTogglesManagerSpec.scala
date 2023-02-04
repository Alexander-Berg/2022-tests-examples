package infra.feature_toggles.logic.test

import common.clients.grafana.testkit.TestGrafanaClient
import common.zio.logging.Logging
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import infra.feature_toggles.logic.FeatureTogglesManager
import infra.feature_toggles.logic.FeatureTogglesManager._
import infra.feature_toggles.model.{BooleanValue, FeatureTelemetry, FeatureToggle, FeatureType}
import infra.feature_toggles.storage.ydb.{YdbFeatureTogglesDao, YdbHistoryDao, YdbTelemetryDao}
import zio.duration._
import zio.magic._
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test.environment.TestClock
import zio.test.{assert, DefaultRunnableSpec, ZSpec}
import zio.{clock, ZLayer}

import java.time.temporal.ChronoUnit

object FeatureTogglesManagerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    (suite("FeatureToggles")(
      testM("Возвращает все фичи") {
        val service1 = "service1"
        val service2 = "service2"
        val neverUsedKey = "never-used"
        val usedKey = "used"
        val multipleConsumersKey = "multiple-consumers"
        val conflictingTypesKey = "conflicting-types"
        val staleKey = "staleKey"
        val value = BooleanValue(true)
        for {
          accessedAt1 <- clock.instant
          accessedAt2 = accessedAt1.plus(7, ChronoUnit.DAYS)
          accessedAt3 = accessedAt2.minus(1, ChronoUnit.HOURS)
          _ <- FeatureTogglesManager.set(service1, neverUsedKey, value, 1, "")
          _ <- FeatureTogglesManager.set(service1, usedKey, value, 1, "")
          _ <- FeatureTogglesManager.reportTelemetry(
            service1,
            Seq(
              FeatureTelemetry(service1, usedKey, service1, accessedAt2, Some(FeatureType.Bool)),
              FeatureTelemetry(service1, multipleConsumersKey, service1, accessedAt2, Some(FeatureType.Int64)),
              FeatureTelemetry(service1, conflictingTypesKey, service1, accessedAt2, Some(FeatureType.Int64)),
              FeatureTelemetry(service1, staleKey, service1, accessedAt1, Some(FeatureType.Int64))
            )
          )
          _ <- FeatureTogglesManager.reportTelemetry(
            service2,
            Seq(
              FeatureTelemetry(service1, multipleConsumersKey, service2, accessedAt3, Some(FeatureType.Int64)),
              FeatureTelemetry(service1, conflictingTypesKey, service2, accessedAt3, Some(FeatureType.Bool)),
              FeatureTelemetry(service1, staleKey, service2, accessedAt2, Some(FeatureType.Int64))
            )
          )
          _ <- FeatureTogglesManager.reportTelemetry(
            service2,
            Seq(
              FeatureTelemetry(service1, staleKey, service2, accessedAt3, Some(FeatureType.Int64))
            )
          )
          _ <- TestClock.adjust(7.days)
          list <- FeatureTogglesManager.listAll(service1)
          feature = FeatureToggle(usedKey, Some(value), accessedAt1, 1, "")
          expectedResults = Set(
            ListItem(StoredFeatureToggle(feature), Seq(Consumer(service1, accessedAt2)), Seq.empty),
            ListItem(StoredFeatureToggle(feature.copy(key = neverUsedKey)), Seq.empty, Seq(Warning.Unused)),
            ListItem(
              EphemeralFeatureToggle(multipleConsumersKey, Some(FeatureType.Int64)),
              Seq(Consumer(service1, accessedAt2), Consumer(service2, accessedAt3)),
              Seq.empty
            ),
            ListItem(
              EphemeralFeatureToggle(conflictingTypesKey, Some(FeatureType.Int64)),
              Seq(Consumer(service1, accessedAt2), Consumer(service2, accessedAt3)),
              Seq(Warning.TypeConflict)
            ),
            ListItem(
              EphemeralFeatureToggle(staleKey, Some(FeatureType.Int64)),
              Seq(Consumer(service2, accessedAt2)),
              Seq.empty
            )
          )
        } yield assert(list.toSet)(equalTo(expectedResults))
      }
    ) @@ sequential)
      .injectSomeShared(
        TestYdb.ydb,
        YdbFeatureTogglesDao.live,
        YdbHistoryDao.live,
        YdbTelemetryDao.live,
        Ydb.txRunner,
        Logging.live,
        TestGrafanaClient.test,
        ZLayer.succeed(FeatureTogglesManager.Config(Nil)),
        FeatureTogglesManager.live
      )
}
