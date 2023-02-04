package vsquality.vacuum.logic.test

import common.clients.hobo.testkit.TestHoboClient
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import etc.dust.api.testkit.TestDialogService
import vertis.dust.clustering_results_model.ClusteringResultEvent
import vertis.dust.model.{CallPayload, CallSpamPayload, ClusterResultPayload, Dialog, DialogId}
import vertis.vsquality.vacuum.model.{ClusterType, Markup}
import vsquality.vacuum.logic.DustCollector
import vsquality.vacuum.model.{Cluster, ClusterUid, VacuumConfig}
import vsquality.vacuum.storage.{ClusterDao, ExportDao}
import vsquality.vacuum.storage.ydb.{YdbClusterDao, YdbExportDao}
import zio.ZLayer
import zio.clock.Clock
import zio.test.TestAspect.sequential
import zio.test._
import zio.test.environment.TestEnvironment

object DustCollectorSpec extends DefaultRunnableSpec {

  val autoRuCallCluster: Cluster = Cluster(
    uid = ClusterUid("test", ClusterType.CALL_SCENARIO),
    state = Cluster.MarkedUp(
      Markup(
        Markup.Resolution.Violation(
          Markup.Violation()
        )
      )
    )
  )

  val autoRuSpamCluster: Cluster = Cluster(
    uid = ClusterUid("CALL_SPAM_AUTORU", ClusterType.CALL_SPAM_AUTORU),
    state = Cluster.MarkedUp(
      Markup(
        Markup.Resolution.Spam(
          Markup.Spam()
        )
      )
    )
  )

  val realtyDialogEvent: ClusteringResultEvent = ClusteringResultEvent(
    clusterType = vertis.dust.model.ClusterType.CALL_SCENARIO_REALTY,
    clusterId = "test",
    timestamp = Some(com.google.protobuf.timestamp.Timestamp()),
    id = Some(DialogId(domain = "test_domain", externalDialogId = "test_dialog_id")),
    dialogInfo = Some(
      Dialog(
        payload = Dialog.Payload.CallPayload(
          CallPayload(
            sourceNumber = "+79999999999"
          )
        )
      )
    )
  )

  def autoruSpamDialogEvent(score: Double): ClusteringResultEvent = ClusteringResultEvent(
    clusterType = vertis.dust.model.ClusterType.CALL_SPAM_AUTORU,
    clusterId = "CALL_SPAM_AUTORU",
    timestamp = Some(com.google.protobuf.timestamp.Timestamp()),
    id = Some(DialogId(domain = "test_domain", externalDialogId = s"test_dialog_id_$score")),
    dialogInfo = Some(
      Dialog(
        payload = Dialog.Payload.CallPayload(
          CallPayload(
            sourceNumber = "+79999999999"
          )
        )
      )
    ),
    payload = Some(
      ClusterResultPayload(
        ClusterResultPayload.Payload.CallSpam(
          CallSpamPayload(
            score = score
          )
        )
      )
    )
  )

  override def spec: ZSpec[TestEnvironment, Any] = suite("DustCollector")(
    testM("inherit auto <-> realty markup") {
      for {
        _ <- Ydb.runTx(ClusterDao.upsert(autoRuCallCluster))
        _ <- DustCollector.collect(realtyDialogEvent)
        realtyCluster <- Ydb.runTx(
          ClusterDao.get(autoRuCallCluster.uid.copy(`type` = ClusterType.CALL_SCENARIO_REALTY))
        )
      } yield assertTrue(realtyCluster.exists(_.state == autoRuCallCluster.state))
    },
    testM("process spam event with high score") {
      for {
        _          <- YdbExportDao.clean
        _          <- Ydb.runTx(ClusterDao.upsert(autoRuSpamCluster))
        _          <- DustCollector.collect(autoruSpamDialogEvent(0.9))
        queueStats <- Ydb.runTx(ExportDao.getQueueMetrics)
      } yield assertTrue(queueStats.size == 1)
    },
    testM("skip spam event with low score") {
      for {
        _          <- YdbExportDao.clean
        _          <- Ydb.runTx(ClusterDao.upsert(autoRuSpamCluster))
        _          <- DustCollector.collect(autoruSpamDialogEvent(0.7))
        queueStats <- Ydb.runTx(ExportDao.getQueueMetrics)
      } yield assertTrue(queueStats.size == 0)
    }
  ).provideCustomLayerShared {
    (TestYdb.ydb ++ Clock.live) >+> (YdbClusterDao.live ++ Ydb.txRunner) >+>
      (YdbClusterDao.live ++ YdbExportDao.live ++ TestDialogService.layer ++ TestHoboClient.layer) ++ ZLayer.succeed(
        VacuumConfig(0.85)
      )
  } @@ sequential
}
