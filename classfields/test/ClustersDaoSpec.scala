package ru.yandex.vertis.etc.dust.storage.ydb.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.etc.dust.model.{ClusterId, ClusterInfo, ClusterType, ClusterTypeInfo}
import ru.yandex.vertis.etc.dust.storage.{ClusterTypesDao, ClustersDao}
import ru.yandex.vertis.etc.dust.storage.ydb.{YdbClusterTypesDao, YdbClustersDao}
import ru.yandex.vertis.etc.dust.storage.ydb.test.ClusterTypesDaoSpec.{suite, testM}
import zio.clock.Clock
import zio.test.Assertion.{equalTo, isNone, isSome}
import zio.test.{assert, DefaultRunnableSpec, ZSpec}
import zio.test.TestAspect.{before, sequential}

object ClustersDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    (suite("YdbClustersDao")(
      testM("get unknown cluster") {
        for {
          result <- runTx(ClustersDao.getClusterInfo(ClusterType.CallScenario, ClusterId("nonexisted")))
        } yield assert(result)(isNone)
      },
      testM("insert and get cluster") {
        val info = ClusterInfo(ClusterType.CallScenario, ClusterId("real"), 1, Some("predicged"), Map("lol" -> 4))
        for {
          _ <- runTx(ClustersDao.upsertClusterInfo(info))
          result <- runTx(ClustersDao.getClusterInfo(info.clusterType, info.clusterId))
        } yield assert(result)(isSome(equalTo(info)))
      }
    ) @@ sequential @@ before(TestYdb.clean(YdbClustersDao.ClustersTable)))
      .provideCustomLayerShared {
        (TestYdb.ydb ++ Clock.live) >+> (YdbClustersDao.live ++ Ydb.txRunner)
      }
}
