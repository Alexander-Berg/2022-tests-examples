package ru.yandex.vertis.etc.dust.storage.ydb.test

import common.zio.ydb.Ydb
import zio.test._
import zio.test.Assertion._
import zio.clock.Clock
import zio.test.TestAspect.{before, sequential}
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.etc.dust.model._
import ru.yandex.vertis.etc.dust.storage.ClusterTypesDao
import ru.yandex.vertis.etc.dust.storage.ydb.YdbClusterTypesDao

object ClusterTypesDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    (suite("YdbClusterTypesDao")(
      testM("get unknown clusterType version") {
        for {
          result <- runTx(ClusterTypesDao.get(ClusterType.CallScenario))
        } yield assert(result)(isNone)
      },
      testM("insert and get clustering result") {
        val info = ClusterTypeInfo(ClusterType.CallScenario, 1, None)
        for {
          _ <- runTx(ClusterTypesDao.upsertInfo(info))
          result <- runTx(ClusterTypesDao.get(ClusterType.CallScenario))
        } yield assert(result)(isSome(equalTo(info)))
      }
    ) @@ sequential @@ before(TestYdb.clean(YdbClusterTypesDao.ClusterTypesTable)))
      .provideCustomLayerShared {
        (TestYdb.ydb ++ Clock.live) >+> (YdbClusterTypesDao.live ++ Ydb.txRunner)
      }
}
