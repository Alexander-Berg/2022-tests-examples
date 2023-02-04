package ru.yandex.vertis.etc.dust.logic.test.dialog

import cats.data.NonEmptyList
import common.zio.events_broker.testkit.TestBroker
import common.zio.logging.Logging
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.etc.dust.DustError.{ClusterTypeNotFound, DialogNotFound, DialogResultNotFound}
import ru.yandex.vertis.etc.dust.logic.dialog.DialogManager
import ru.yandex.vertis.etc.dust.model.Dialog.Phrase
import ru.yandex.vertis.etc.dust.model._
import ru.yandex.vertis.etc.dust.storage._
import ru.yandex.vertis.etc.dust.storage.ydb.{YdbClusterTypesDao, YdbClusteringResultsDao, YdbDialogDao, YdbStatusesDao}
import vertis.dust.model.FailPolicy
import vertis.dust.nirvana_events_model.NirvanaClusteringEvent
import zio.ZIO
import zio.clock.Clock
import zio.stream.ZSink
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect.{before, sequential}

import java.time.Instant

object DefaultDialogManagerSpec extends DefaultRunnableSpec {

  private val phrases = NonEmptyList.one(Phrase(Instant.now, "text", Dialog.Buyer))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    (suite("DefaultDialogManager")(
      testM("Start dialog clustering with dialog creation") {
        val dialog = Dialog(
          DialogId("clusterWithCreation"),
          "teleponycalls",
          phrases.head.timestamp,
          phrases,
          Dialog.CallPayload("+79999999999", "autoru_def")
        )
        val createInfo = CreateDialogInfo(dialog.startTime, dialog.phrases, dialog.dialogPayload)
        for {
          clusterTypes <- DialogManager
            .startDialogClustering(dialog.id, dialog.domain, Some(createInfo), Seq(ClusterType.CallScenario))
          dialogResult <- runTx(DialogDao.get(dialog.domain, dialog.id))
          statusResult <- runTx(StatusesDao.getStatus(dialog.id, dialog.domain, ClusterType.CallScenario))
        } yield assert(dialogResult)(isSome(equalTo(dialog))) && assert(statusResult.map(_.status))(
          isSome(equalTo(ClusteringStatus.Status.InProcess))
        ) &&
          assert(clusterTypes)(contains(ClusterType.CallScenario))
      },
      testM("Start dialog clustering with existing dialog") {
        val dialog = Dialog(
          DialogId("clusterWithExisting"),
          "teleponycalls",
          phrases.head.timestamp,
          phrases,
          Dialog.CallPayload("+79999999999", "autoru_def")
        )
        for {
          _ <- runTx(DialogDao.upsert(dialog))
          clusterTypes <- DialogManager
            .startDialogClustering(dialog.id, dialog.domain, None, Seq(ClusterType.CallScenario))
          dialogResult <- runTx(DialogDao.get(dialog.domain, dialog.id))
          statusResult <- runTx(StatusesDao.getStatus(dialog.id, dialog.domain, ClusterType.CallScenario))
        } yield assert(dialogResult)(isSome(equalTo(dialog))) && assert(statusResult.map(_.status))(
          isSome(equalTo(ClusteringStatus.Status.InProcess))
        ) &&
          assert(clusterTypes)(contains(ClusterType.CallScenario))
      },
      testM("Wont start dialog clustering if it is already in progress") {
        val dialog =
          Dialog(
            DialogId("inprogress"),
            "teleponycalls",
            phrases.head.timestamp,
            phrases,
            Dialog.CallPayload("+79999999999", "autoru_def")
          )
        for {
          _ <- runTx(DialogDao.upsert(dialog))
          now <- ZIO.accessM[Clock](_.get.instant)
          _ <- runTx(
            StatusesDao.upsertStatus(
              ClusteringStatus(
                dialog.id,
                ClusterType.CallScenario,
                now,
                dialog.domain,
                ClusteringStatus.Status.InProcess
              )
            )
          )
          clusterTypes <- DialogManager
            .startDialogClustering(dialog.id, dialog.domain, None, Seq(ClusterType.CallScenario))
        } yield assert(clusterTypes)(isEmpty)
      },
      testM("Start clustering dialog if it is already processed") {
        val dialog =
          Dialog(
            DialogId("processed"),
            "teleponycalls",
            phrases.head.timestamp,
            phrases,
            Dialog.CallPayload("+79999999999", "autoru_def")
          )
        for {
          _ <- runTx(DialogDao.upsert(dialog))
          now <- ZIO.accessM[Clock](_.get.instant)
          _ <- runTx(
            StatusesDao.upsertStatus(
              ClusteringStatus(
                dialog.id,
                ClusterType.CallScenario,
                now,
                dialog.domain,
                ClusteringStatus.Status.Processed
              )
            )
          )
          clusterTypes <- DialogManager
            .startDialogClustering(dialog.id, dialog.domain, None, Seq(ClusterType.CallScenario))
        } yield assert(clusterTypes)(contains(ClusterType.CallScenario))
      },
      testM("Won't start clustering if dialog not found") {
        val dialog =
          Dialog(
            DialogId("notfound"),
            "teleponycalls",
            phrases.head.timestamp,
            phrases,
            Dialog.CallPayload("+79999999999", "autoru_def")
          )
        for {
          error <- DialogManager
            .startDialogClustering(dialog.id, dialog.domain, None, Seq(ClusterType.CallScenario))
            .flip
        } yield assert(error)(equalTo(DialogNotFound(dialog.domain, dialog.id)))
      },
      testM("Get clustering results") {
        val dialog = Dialog(
          DialogId("clusteringResults"),
          "teleponycalls",
          phrases.head.timestamp,
          phrases,
          Dialog.CallPayload("+79999999999", "autoru_def")
        )
        val version = 1
        val clusteringResult =
          ClusteringResult(
            dialog.id,
            ClusterType.CallScenario,
            dialog.domain,
            version,
            ClusterId("cluster"),
            Some(ClusteringResult.CallScenarioPayload(Some(666)))
          )
        for {
          _ <- runTx(DialogDao.upsert(dialog))
          _ <- runTx(ClusterTypesDao.upsertInfo(ClusterTypeInfo(ClusterType.CallScenario, version, None)))
          now <- ZIO.accessM[Clock](_.get.instant)
          _ <- runTx(
            StatusesDao.upsertStatus(
              ClusteringStatus(
                dialog.id,
                ClusterType.CallScenario,
                now.minusSeconds(10),
                dialog.domain,
                ClusteringStatus.Status.Processed
              )
            )
          )
          _ <- runTx(ClusteringResultsDao.saveResults(Seq(clusteringResult)))
          results <- DialogManager
            .getClusteringResults(dialog.id, dialog.domain, Seq(ClusterType.CallScenario), FailPolicy.FAIL_NEVER)
        } yield assert(results)(contains(clusteringResult))
      },
      testM("Fail to get clustering results when dialog not found") {
        val dialog = Dialog(
          DialogId("clusteringResults"),
          "teleponycalls",
          phrases.head.timestamp,
          phrases,
          Dialog.CallPayload("+79999999999", "autoru_def")
        )
        val version = 1
        val clusteringResult =
          ClusteringResult(
            dialog.id,
            ClusterType.CallScenario,
            dialog.domain,
            version,
            ClusterId("cluster"),
            Some(ClusteringResult.CallScenarioPayload(Some(666)))
          )
        for {
          _ <- runTx(ClusterTypesDao.upsertInfo(ClusterTypeInfo(ClusterType.CallScenario, version, None)))
          now <- ZIO.accessM[Clock](_.get.instant)
          _ <- runTx(
            StatusesDao.upsertStatus(
              ClusteringStatus(
                dialog.id,
                ClusterType.CallScenario,
                now.minusSeconds(10),
                dialog.domain,
                ClusteringStatus.Status.Processed
              )
            )
          )
          _ <- runTx(ClusteringResultsDao.saveResults(Seq(clusteringResult)))
          error <- DialogManager
            .getClusteringResults(dialog.id, dialog.domain, Seq(ClusterType.CallScenario), FailPolicy.FAIL_NEVER)
            .flip
        } yield assert(error)(equalTo(DialogNotFound(dialog.domain, dialog.id)))
      },
      testM("Try to get clustering results using different policies when cluster type is unknown") {
        val dialog = Dialog(
          DialogId("clusteringResults"),
          "teleponycalls",
          phrases.head.timestamp,
          phrases,
          Dialog.CallPayload("+79999999999", "autoru_def")
        )
        val version = 1
        val clusteringResult =
          ClusteringResult(
            dialog.id,
            ClusterType.CallScenario,
            dialog.domain,
            version,
            ClusterId("cluster"),
            Some(ClusteringResult.CallScenarioPayload(Some(666)))
          )
        for {
          _ <- runTx(DialogDao.upsert(dialog))
          now <- ZIO.accessM[Clock](_.get.instant)
          _ <- runTx(
            StatusesDao.upsertStatus(
              ClusteringStatus(
                dialog.id,
                ClusterType.CallScenario,
                now.minusSeconds(10),
                dialog.domain,
                ClusteringStatus.Status.Processed
              )
            )
          )
          _ <- runTx(ClusteringResultsDao.saveResults(Seq(clusteringResult)))
          failFastError <- DialogManager
            .getClusteringResults(dialog.id, dialog.domain, Seq(ClusterType.CallScenario), FailPolicy.FAIL_FAST)
            .flip
          failNeverResult <- DialogManager
            .getClusteringResults(dialog.id, dialog.domain, Seq(ClusterType.CallScenario), FailPolicy.FAIL_NEVER)
        } yield assert(failFastError)(equalTo(ClusterTypeNotFound(ClusterType.CallScenario))) &&
          assert(failNeverResult)(isEmpty)
      },
      testM("Try to get clustering results using different policies when clustering results not found") {
        val dialog = Dialog(
          DialogId("clusteringResults"),
          "teleponycalls",
          phrases.head.timestamp,
          phrases,
          Dialog.CallPayload("+79999999999", "autoru_def")
        )
        val version = 1
        val clusteringResult =
          ClusteringResult(
            dialog.id,
            ClusterType.CallScenario,
            dialog.domain,
            version,
            ClusterId("cluster"),
            None
          )
        for {
          _ <- runTx(DialogDao.upsert(dialog))
          _ <- runTx(ClusterTypesDao.upsertInfo(ClusterTypeInfo(ClusterType.CallScenario, version, None)))
          _ <- runTx(ClusteringResultsDao.saveResults(Seq(clusteringResult)))
          failFastError <- DialogManager
            .getClusteringResults(dialog.id, dialog.domain, Seq(ClusterType.CallScenario), FailPolicy.FAIL_FAST)
            .flip
          failNeverResult <- DialogManager
            .getClusteringResults(dialog.id, dialog.domain, Seq(ClusterType.CallScenario), FailPolicy.FAIL_NEVER)
        } yield assert(failFastError)(
          equalTo(DialogResultNotFound(dialog.domain, dialog.id, ClusterType.CallScenario))
        ) &&
          assert(failNeverResult)(isEmpty)
      },
      testM("Get cluster dialogs") {
        val dialog1 =
          Dialog(
            DialogId("clusterDialog1"),
            "teleponycalls",
            phrases.head.timestamp,
            phrases,
            Dialog.CallPayload("+79999999999", "autoru_def")
          )
        val dialog2 =
          Dialog(
            DialogId("clusterDialog2"),
            "teleponycalls",
            phrases.head.timestamp,
            phrases,
            Dialog.CallPayload("+79999999999", "autoru_def")
          )
        val version = 1
        val clusteringResult1 =
          ClusteringResult(
            dialog1.id,
            ClusterType.CallScenario,
            dialog1.domain,
            version,
            ClusterId("cluster"),
            Some(ClusteringResult.CallScenarioPayload(Some(666)))
          )
        val clusteringResult2 =
          ClusteringResult(
            dialog2.id,
            ClusterType.CallScenario,
            dialog2.domain,
            version,
            ClusterId("cluster"),
            None
          )
        val clusterDialog1 = dialog1 -> clusteringResult1
        val clusterDialog2 = dialog2 -> clusteringResult2
        for {
          _ <- runTx(ClusterTypesDao.upsertInfo(ClusterTypeInfo(ClusterType.CallScenario, version, None)))
          _ <- runTx(DialogDao.upsert(dialog1))
          _ <- runTx(DialogDao.upsert(dialog2))
          _ <- runTx(ClusteringResultsDao.saveResults(Seq(clusteringResult1, clusteringResult2)))
          results <- DialogManager
            .getClusterDialogs(ClusterType.CallScenario, clusteringResult1.clusterId, None)
            .run(ZSink.collectAll)
        } yield assert(results)(contains(clusterDialog1)) && assert(results)(contains(clusterDialog2))
      },
      testM("Fail to get cluster dialogs for unknown cluster type") {
        for {
          error <- DialogManager.getClusterDialogs(ClusterType.CallScenario, ClusterId("empty"), None).runDrain.flip
        } yield assert(error)(equalTo(ClusterTypeNotFound(ClusterType.CallScenario)))
      }
    ) @@ sequential @@ before(cleanTables))
      .provideCustomLayerShared {
        val ydb = TestYdb.ydb
        val txRunner = ydb >>> Ydb.txRunner

        val dialogDao = ydb >>> YdbDialogDao.live
        val statusesDao = ydb >>> YdbStatusesDao.live
        val clusteringResultsDao = ydb >>> YdbClusteringResultsDao.live
        val versionsDao = ydb >>> YdbClusterTypesDao.live

        val daos = txRunner ++ dialogDao ++ statusesDao ++ clusteringResultsDao ++ versionsDao

        val broker = TestBroker.noOpTyped[NirvanaClusteringEvent]

        val clock = Clock.live

        val logging = Logging.live

        val dialogsManager =
          clock ++ logging ++ daos ++ broker >>> DialogManager.live

        ydb ++ clock ++ daos ++ dialogsManager
      }

  private def cleanTables =
    TestYdb.clean(YdbClusterTypesDao.ClusterTypesTable) *>
      TestYdb.clean(YdbDialogDao.TableName) *>
      TestYdb.clean(YdbStatusesDao.StatusesTable) *>
      TestYdb.clean(YdbClusteringResultsDao.ClusteringResultsTable)
}
