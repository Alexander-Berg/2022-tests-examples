package vsquality.vacuum.storage.ydb.test

import cats.data.NonEmptyList
import com.google.protobuf.timestamp.Timestamp
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import vertis.vsquality.vacuum.event.ClusterChangeEvent
import vertis.vsquality.vacuum.model.{Cluster, Dialog, DialogId}
import vsquality.vacuum.storage.ExportDao
import vsquality.vacuum.storage.ydb.YdbExportDao
import zio.clock.Clock
import zio.test.Assertion.{equalTo, hasSize}
import zio.test.TestAspect.{sequential, shrinks}
import zio.test.{assert, _}

object YdbExportDaoSpec extends DefaultRunnableSpec {

  val testEvent: ClusterChangeEvent = ClusterChangeEvent(
    dialog = Some(
      Dialog(
        id = Some(DialogId("test_id", "test_domain")),
        timestamp = Some(Timestamp(seconds = 3))
      )
    ),
    newCluster = Some(Cluster())
  )

  override def spec =
    (suite("YdbExportDao")(
      testM("single push && pull") {

        for {
          _      <- runTx(ExportDao.push(testEvent))
          shard1 <- runTx(ExportDao.pull(YdbExportDao.shardId(testEvent), 3))
        } yield assert(shard1)(hasSize(equalTo(1)))

      },
      testM("single remove ") {
        for {
          _      <- YdbExportDao.clean
          _      <- runTx(ExportDao.push(testEvent))
          shard1 <- runTx(ExportDao.pull(YdbExportDao.shardId(testEvent), 2))
          _      <- runTx(ExportDao.remove(NonEmptyList.one(testEvent)))
          shard2 <- runTx(ExportDao.pull(YdbExportDao.shardId(testEvent), 2))
        } yield assert(shard1)(hasSize(equalTo(1))) && assert(shard2)(hasSize(equalTo(0)))
      }
    ) @@ sequential @@ shrinks(1))
      .provideCustomLayerShared {
        (TestYdb.ydb ++ Clock.live) >+> (YdbExportDao.live ++ Ydb.txRunner)
      }

}
