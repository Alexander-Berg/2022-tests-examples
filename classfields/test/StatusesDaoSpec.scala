package ru.yandex.vertis.etc.dust.storage.ydb.test

import common.zio.ydb.Ydb
import zio.test._
import zio.test.Assertion._
import zio.clock.Clock
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.etc.dust.model._
import ru.yandex.vertis.etc.dust.storage.StatusesDao
import ru.yandex.vertis.etc.dust.storage.ydb.YdbStatusesDao

import java.time.Instant
import java.time.temporal.{ChronoUnit, TemporalUnit}

object StatusesDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("YdbStatusesDao")(
      testM("Get unknown dialog status") {
        for {
          result <- runTx(StatusesDao.getStatus(DialogId("unknown"), "autorucalls", ClusterType.CallScenario))
        } yield assert(result)(isNone)
      },
      testM("Upsert and get dialog status") {
        val status = ClusteringStatus(
          DialogId("upserted"),
          ClusterType.CallScenario,
          Instant.now.truncatedTo(ChronoUnit.MILLIS),
          "autorucalls",
          ClusteringStatus.Status.InProcess
        )
        for {
          _ <- runTx(StatusesDao.upsertStatus(status))
          result <- runTx(StatusesDao.getStatus(status.dialogId, status.domain, status.clusterType))
        } yield assert(result)(isSome(equalTo(status)))
      },
      testM("Upsert update existing status") {
        val status = ClusteringStatus(
          DialogId("updating"),
          ClusterType.CallScenario,
          Instant.now.truncatedTo(ChronoUnit.MILLIS),
          "autorucalls",
          ClusteringStatus.Status.InProcess
        )
        for {
          _ <- runTx(StatusesDao.upsertStatus(status))
          updated = status.copy(status = ClusteringStatus.Status.Processed)
          _ <- runTx(StatusesDao.upsertStatus(updated))
          result <- runTx(StatusesDao.getStatus(status.dialogId, status.domain, status.clusterType))
        } yield assert(result)(isSome(equalTo(updated)))
      },
      testM("Delete existing status") {
        val status = ClusteringStatus(
          DialogId("deleting"),
          ClusterType.CallScenario,
          Instant.now.truncatedTo(ChronoUnit.MILLIS),
          "autorucalls",
          ClusteringStatus.Status.InProcess
        )
        for {
          _ <- runTx(StatusesDao.upsertStatus(status))
          _ <- runTx(StatusesDao.deleteStatus(status.dialogId, status.domain, status.clusterType))
          result <- runTx(StatusesDao.getStatus(status.dialogId, status.domain, status.clusterType))
        } yield assert(result)(isNone)
      },
      testM("Count inprogress") {
        val status1 = ClusteringStatus(
          DialogId("count1"),
          ClusterType.CallScenario,
          Instant.now.truncatedTo(ChronoUnit.MILLIS).minusSeconds(100),
          "autorucalls",
          ClusteringStatus.Status.InProcess
        )
        val status2 = ClusteringStatus(
          DialogId("count2"),
          ClusterType.CallScenario,
          Instant.now.truncatedTo(ChronoUnit.MILLIS).minusSeconds(100),
          "autorucalls",
          ClusteringStatus.Status.InProcess
        )
        val status3 = ClusteringStatus(
          DialogId("count3"),
          ClusterType.CallScenario,
          Instant.now.truncatedTo(ChronoUnit.MILLIS),
          "autorucalls",
          ClusteringStatus.Status.InProcess
        )
        val status4 = ClusteringStatus(
          DialogId("count4"),
          ClusterType.CallScenario,
          Instant.now.truncatedTo(ChronoUnit.MILLIS).minusSeconds(100),
          "autorucalls",
          ClusteringStatus.Status.Processed
        )
        for {
          _ <- runTx(StatusesDao.upsertStatus(status1))
          _ <- runTx(StatusesDao.upsertStatus(status2))
          _ <- runTx(StatusesDao.upsertStatus(status3))
          _ <- runTx(StatusesDao.upsertStatus(status4))
          result <- runTx(
            StatusesDao.countUpdatedInStatus(ClusteringStatus.Status.InProcess, Instant.now.minusSeconds(20))
          )
        } yield assert(result)(equalTo(2))
      }
    ).provideCustomLayerShared {
      (TestYdb.ydb ++ Clock.live) >+> (YdbStatusesDao.live ++ Ydb.txRunner)
    }
}
