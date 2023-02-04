package infra.feature_toggles.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import infra.feature_toggles.model.pagination.LimitOffset
import infra.feature_toggles.model.FeatureToggle
import infra.feature_toggles.model.testkit.Gen
import infra.feature_toggles.storage.FeatureTogglesDao
import infra.feature_toggles.storage.ydb.YdbFeatureTogglesDao
import zio.ZIO
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, shrinks}
import zio.test.{Gen => _, _}

import java.time.Instant
import java.time.temporal.ChronoUnit

object FeatureTogglesDaoSpec extends DefaultRunnableSpec {

  def now = Instant.now.truncatedTo(ChronoUnit.MICROS)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("FeatureToggleDao")(
      testM("Возвращает фича-флаг, если он есть") {
        checkNM(1)(Gen.service, Gen.value) { (service, value) =>
          val key = "key"
          val updatedAt = now
          val author = 123
          val description = "Desc"

          for {
            _ <- TestYdb.runTx(FeatureTogglesDao.set(service, key, value, updatedAt, author, description))
            actual <- TestYdb.runTx(FeatureTogglesDao.get(service, key))
          } yield assertTrue(actual.get == FeatureToggle(key, Some(value), updatedAt, author, description))
        }
      },
      testM("Не возвращает фича-флаг, если его нет") {
        checkNM(1)(Gen.service) { service =>
          for {
            actual <- TestYdb.runTx(FeatureTogglesDao.get(service, "unknown"))
          } yield assert(actual)(isNone)
        }
      },
      testM("Не возвращает фича-флаг, если он удален") {
        checkNM(1)(Gen.service, Gen.value) { (service, value) =>
          val key = "key"
          for {
            _ <- TestYdb.runTx(FeatureTogglesDao.set(service, key, value, now, 123, "set"))
            _ <- TestYdb.runTx(FeatureTogglesDao.delete(service, key))
            actual <- TestYdb.runTx(FeatureTogglesDao.get(service, key))
          } yield assert(actual)(isNone)
        }
      },
      testM("Возвращает все фича-флаги сервиса") {
        checkNM(1)(Gen.service, Gen.value, Gen.value) { (service, value1, value2) =>
          val featureToggles =
            List(
              FeatureToggle("key1", Some(value1), now, 123, "desc"),
              FeatureToggle("key2", Some(value1), now, 456, "desc"),
              FeatureToggle("key3", Some(value2), now, 789, "desc")
            )

          for {
            _ <- ZIO.foreach_(featureToggles) { ft =>
              TestYdb.runTx(FeatureTogglesDao.set(service, ft.key, ft.value.get, ft.updatedAt, ft.author, "desc"))
            }
            _ <- TestYdb.runTx(FeatureTogglesDao.set("service", "key", value1, now, 123, "desc"))
            actual <- TestYdb.runTx(FeatureTogglesDao.list(service))
          } yield assertTrue(actual == featureToggles)
        }
      }
    ) @@ shrinks(0) @@ sequential).provideCustomLayerShared {
      TestYdb.ydb >>> (YdbFeatureTogglesDao.live ++ Ydb.txRunner) ++ Clock.live
    }
  }
}
