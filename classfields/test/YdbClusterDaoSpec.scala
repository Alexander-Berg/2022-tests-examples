package vsquality.vacuum.storage.ydb.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import vertis.vsquality.vacuum.model.{ClusterType, DialogId}
import vsquality.vacuum.model.{Cluster, ClusterUid}
import vsquality.vacuum.storage.ClusterDao
import vsquality.vacuum.storage.ydb.YdbClusterDao
import zio.clock.Clock
import zio.test.Assertion.equalTo
import zio.test.TestAspect.{sequential, shrinks}
import zio.test._
import zio.test.environment.TestEnvironment

object YdbClusterDaoSpec extends DefaultRunnableSpec {

  val cluster = Cluster(
    uid = ClusterUid("test_id", ClusterType.CALL_SCENARIO),
    state = Cluster.New(knownDialogIds =
      Set(
        DialogId(externalDialogId = "1"),
        DialogId(externalDialogId = "2"),
        DialogId(externalDialogId = "3")
      )
    )
  )

  override def spec: ZSpec[TestEnvironment, Any] = {
    (suite("YdbClusterDao")(
      testM("upsert") {
        for {
          _          <- YdbClusterDao.clean
          _          <- runTx(ClusterDao.upsert(cluster))
          purchaseDb <- runTx(ClusterDao.get(cluster.uid))
        } yield assert(purchaseDb.get)(equalTo(cluster))
      }
    ) @@ sequential @@ shrinks(1))
      .provideCustomLayerShared {
        (TestYdb.ydb ++ Clock.live) >+> (YdbClusterDao.live ++ Ydb.txRunner)
      }
  }
}
