package ru.yandex.vertis.etc.dust.storage.ydb.test

import common.zio.ydb.Ydb
import zio.test._
import zio.test.Assertion._
import zio.clock.Clock
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.etc.dust.model.{ClusterId, ClusterType, ClusteringResult, DialogId}
import ru.yandex.vertis.etc.dust.storage.ClusteringResultsDao
import ru.yandex.vertis.etc.dust.storage.ydb.YdbClusteringResultsDao

object ClusteringResultsDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("YdbClusteringResultsDao")(
      testM("get unknown clustering result") {
        for {
          result <- runTx(ClusteringResultsDao.get(DialogId("dddd"), "autorucalls", ClusterType.CallScenario, 0))
        } yield assert(result)(isNone)
      },
      testM("insert and get clustering result") {
        val clusteringResult1 =
          ClusteringResult(
            DialogId("insertGet1"),
            ClusterType.CallScenario,
            "autorucalls",
            1,
            ClusterId("default"),
            Some(ClusteringResult.CallScenarioPayload(Some(1)))
          )
        val clusteringResult2 =
          ClusteringResult(
            DialogId("insertGet2"),
            ClusterType.CallScenario,
            "autorucalls",
            1,
            ClusterId("default"),
            Some(ClusteringResult.CallScenarioPayload(Some(2)))
          )
        for {
          _ <- runTx(ClusteringResultsDao.saveResults(Seq(clusteringResult1, clusteringResult2)))
          result1 <- runTx(ClusteringResultsDao.get(DialogId("insertGet1"), "autorucalls", ClusterType.CallScenario, 1))
          result2 <- runTx(ClusteringResultsDao.get(DialogId("insertGet2"), "autorucalls", ClusterType.CallScenario, 1))
        } yield assert(result1)(isSome(equalTo(clusteringResult1))) &&
          assert(result2)(isSome(equalTo(clusteringResult2)))
      },
      testM("insert and get results from cluster") {
        val clusteringResult1 =
          ClusteringResult(
            DialogId("cluster1"),
            ClusterType.CallScenario,
            "autorucalls",
            1,
            ClusterId("cluster"),
            Some(ClusteringResult.CallScenarioPayload(Some(5)))
          )
        val clusteringResult2 =
          ClusteringResult(
            DialogId("cluster2"),
            ClusterType.CallScenario,
            "autorucalls",
            1,
            ClusterId("cluster"),
            Some(ClusteringResult.CallScenarioPayload(Some(4)))
          )
        val clusteringResult3 =
          ClusteringResult(
            DialogId("cluster3"),
            ClusterType.CallScenario,
            "autorucalls",
            1,
            ClusterId("another"),
            None
          )
        val clusteringResult4 =
          ClusteringResult(
            DialogId("cluster4"),
            ClusterType.CallScenario,
            "autorucalls",
            0,
            ClusterId("cluster"),
            None
          )
        val clusteringResult5 =
          ClusteringResult(
            DialogId("cluster4"),
            ClusterType.CallScenario,
            "autorucalls",
            2,
            ClusterId("cluster"),
            Some(ClusteringResult.CallScenarioPayload(Some(1)))
          )
        for {
          _ <- runTx(
            ClusteringResultsDao.saveResults(
              Seq(clusteringResult1, clusteringResult2, clusteringResult3, clusteringResult4, clusteringResult5)
            )
          )
          allCluster <- runTx(
            ClusteringResultsDao.getClusterResults(ClusterType.CallScenario, ClusterId("cluster"), 5, 0)
          )
          firstResult <- runTx(
            ClusteringResultsDao.getClusterResults(ClusterType.CallScenario, ClusterId("cluster"), 2, 0)
          )
          secondResult <- runTx(
            ClusteringResultsDao.getClusterResults(ClusterType.CallScenario, ClusterId("cluster"), 2, 2)
          )
        } yield assert(allCluster)(hasSize(equalTo(3))) &&
          assert(firstResult)(hasSize(equalTo(2))) &&
          assert(secondResult)(hasSize(equalTo(1))) &&
          assert(firstResult.headOption)(not(equalTo(secondResult.headOption))) &&
          assert(firstResult ++ secondResult)(hasSameElements(allCluster))
      }
    ).provideCustomLayerShared {
      (TestYdb.ydb ++ Clock.live) >+> (YdbClusteringResultsDao.live ++ Ydb.txRunner)
    }
}
