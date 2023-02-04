package ru.yandex.vertis.billing.shop.storage.ydb.test

import billing.log_model.{EventType, ProductEvent}
import cats.data.NonEmptyList
import com.google.protobuf.timestamp.Timestamp
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.billing.shop.storage.ExportDao
import ru.yandex.vertis.billing.shop.storage.ydb.YdbExportDao
import zio.clock.Clock
import zio.test.Assertion.{equalTo, hasSize}
import zio.test.TestAspect.{sequential, shrinks}
import zio.test.{assert, _}

object YdbExportDaoSpec extends DefaultRunnableSpec {

  val testRecord: ProductEvent = ProductEvent(
    eventCreated = Some(Timestamp.of(253L, 0)),
    productId = "test_id",
    activated = Some(Timestamp.of(100L, 0)),
    eventType = EventType.ACTIVATION
  )

  val testRecord1: ProductEvent = ProductEvent(
    eventCreated = Some(Timestamp.of(125L, 1)),
    productId = "test_id1",
    activated = Some(Timestamp.of(120L, 1)),
    eventType = EventType.DEACTIVATION
  )

  val testRecord2: ProductEvent = ProductEvent(
    eventCreated = Some(Timestamp.of(1130L, 0)),
    productId = "test_id2",
    activated = Some(Timestamp.of(1120L, 0)),
    eventType = EventType.REFUND
  )

  override def spec =
    (suite("YdbExportDao")(
      testM("single push && pull") {

        for {
          _ <- runTx(ExportDao.push(testRecord))
          _ <- runTx(ExportDao.push(testRecord1))
          _ <- runTx(ExportDao.push(testRecord2))
          shard1 <- runTx(ExportDao.pull(YdbExportDao.shardNum(testRecord), 3))
          shard2 <- runTx(ExportDao.pull(YdbExportDao.shardNum(testRecord2), 3))
        } yield assert(shard1)(hasSize(equalTo(2))) && assert(shard2)(hasSize(equalTo(1)))

      },
      testM("single remove ") {
        for {
          _ <- runTx(YdbExportDao.clean)
          _ <- runTx(ExportDao.push(testRecord))
          _ <- runTx(ExportDao.push(testRecord1))
          _ <- runTx(ExportDao.push(testRecord2))
          _ <- runTx(ExportDao.remove(NonEmptyList.one(testRecord1)))
          _ <- runTx(ExportDao.remove(NonEmptyList.one(testRecord2)))
          shard1 <- runTx(ExportDao.pull(YdbExportDao.shardNum(testRecord1), 2))
          shard2 <- runTx(ExportDao.pull(YdbExportDao.shardNum(testRecord2), 2))
        } yield assert(shard1)(hasSize(equalTo(1))) && assert(shard2)(hasSize(equalTo(0)))
      },
      testM("batch remove") {
        for {
          _ <- runTx(YdbExportDao.clean)
          _ <- runTx(ExportDao.push(testRecord))
          _ <- runTx(ExportDao.push(testRecord1))
          _ <- runTx(ExportDao.push(testRecord2))
          _ <- runTx(ExportDao.remove(NonEmptyList.of(testRecord, testRecord2)))
          shard1 <- runTx(ExportDao.pull(YdbExportDao.shardNum(testRecord), 3))
          shard2 <- runTx(ExportDao.pull(YdbExportDao.shardNum(testRecord2), 3))
        } yield assert(shard1)(hasSize(equalTo(1))) && assert(shard2)(hasSize(equalTo(0)))
      }
    ) @@ sequential @@ shrinks(1))
      .provideCustomLayerShared {
        (TestYdb.ydb ++ Clock.live) >+> (YdbExportDao.live ++ Ydb.txRunner)
      }

}
