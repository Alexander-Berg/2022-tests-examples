package ru.yandex.vertis.general.hammer.storage.test

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneOffset}

import common.zio.ydb.Ydb
import common.zio.ydb.Ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import general.hammer.queue_model.{EmailAggregate, NotificationAggregate}
import ru.yandex.vertis.general.hammer.model.NotificationsBatchQueueRecord
import ru.yandex.vertis.general.hammer.storage.NotificationBatchQueueDao
import ru.yandex.vertis.general.hammer.storage.NotificationBatchQueueDao.BatchGroupId
import ru.yandex.vertis.general.hammer.storage.ydb.YdbNotificationBatchQueueDao
import ru.yandex.vertis.ydb.Ydb.ops.ResultSetOps
import ru.yandex.vertis.ydb.zio.{Tx, TxUIO, TxURIO}
import zio.clock.Clock
import zio.test.Assertion.{equalTo, hasSameElements}
import zio.test.TestAspect.{shrinks, _}
import zio.test.{assert, DefaultRunnableSpec, ZSpec}
import zio.{Has, Task, ZIO, ZLayer}

import scala.util.Random

object NotificationBatchQueueDaoTest extends DefaultRunnableSpec {

  private object SupportDao {
    type SupportDao = Has[Service]

    trait Service {
      def listAll(): TxUIO[Seq[NotificationsBatchQueueRecord]]
    }

    def listAll(): TxURIO[SupportDao, Seq[NotificationsBatchQueueRecord]] = {
      Tx.accessM(_.get.listAll())
    }

    val live: ZLayer[Ydb, Nothing, SupportDao] =
      (for {
        ydb <- ZIO.environment[Ydb]
        madeLive = makeLive(ydb)
      } yield madeLive).toLayer

    def makeLive(ydb: Ydb): Service = {
      new Service {
        override def listAll(): TxUIO[Seq[NotificationsBatchQueueRecord]] = {
          for {
            ydbResult <- ydb.get.execute(s"SELECT * FROM notifications_batch_queue".stripMargin)
            res <- Tx.fromEffect(
              Task.effectTotal(ydbResult.resultSet.toSeq(YdbNotificationBatchQueueDao.fromStorage))
            )
          } yield res
        }
      }
    }

  }

  private def rndRecord = NotificationsBatchQueueRecord(
    destination = "destination_" + Random.nextString(10),
    destinationType = "destinationType_" + Random.nextString(10),
    eventName = "eventName_" + Random.nextString(10),
    bucketId = Random.nextInt(100),
    createdAt = java.time.LocalDateTime.of(2021, 12, 31, 5, 1, 23, 123452).toInstant(ZoneOffset.ofHours(3)),
    aggregate =
      NotificationAggregate(NotificationAggregate.Aggregate.EmailAggregate(EmailAggregate(5, Set("Плохое фото"))))
  )

  private def truncateNano(record: NotificationsBatchQueueRecord) = {
    record.copy(createdAt = record.createdAt.truncatedTo(ChronoUnit.MILLIS))
  }

  private def toBatchGroupId(record: NotificationsBatchQueueRecord): BatchGroupId = {
    BatchGroupId(
      destination = record.destination,
      destinationType = record.destinationType,
      eventName = record.eventName
    )
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("NotificationBatchQueueDaoTest")(
      testM("upsert and find") {
        val records = Seq.fill(5)(rndRecord)
        for {
          _ <- runTx(NotificationBatchQueueDao.upsertBatch(records))
          returnedOffers <- ZIO.foreachPar(records)(record =>
            runTx(
              NotificationBatchQueueDao.find(
                record.destination,
                record.destinationType,
                record.eventName,
                record.bucketId
              )
            )
          )
        } yield assert(returnedOffers.flatten)(equalTo(records.map(truncateNano)))
      },
      testM("listOld should return only records which are older than defined interval") {
        val now = Instant.now()

        val record10minOld = rndRecord.copy(createdAt = now.minus(10, ChronoUnit.MINUTES))
        val record30minOld = rndRecord.copy(createdAt = now.minus(30, ChronoUnit.MINUTES))
        val record61minOld = rndRecord.copy(createdAt = now.minus(61, ChronoUnit.MINUTES))
        val record120minOld = rndRecord.copy(createdAt = now.minus(120, ChronoUnit.MINUTES))
        val records = Seq(record10minOld, record30minOld, record61minOld, record120minOld)

        val thresholdOfBecomingOld = now.minus(60, ChronoUnit.MINUTES)

        for {
          _ <- runTx(NotificationBatchQueueDao.upsertBatch(records))
          oldRecords <- ZIO
            .foreach(Seq.range(0, YdbNotificationBatchQueueDao.shardsCount)) { shardId =>
              runTx(NotificationBatchQueueDao.listOld(shardId, 10, thresholdOfBecomingOld))
            }
            .map(_.flatten)
        } yield assert(oldRecords)(hasSameElements(Seq(record61minOld, record120minOld).map(toBatchGroupId)))
      },
      testM("listOld should return only the one record of each group") {
        val now = Instant.now()
        val someOldTime = now.minus(120, ChronoUnit.MINUTES)

        val randomRecord1 = rndRecord.copy(createdAt = someOldTime)
        val randomRecord2 = rndRecord.copy(createdAt = someOldTime)

        val records = Seq(
          randomRecord1.copy(bucketId = 1),
          randomRecord1.copy(bucketId = 2),
          randomRecord2.copy(bucketId = 1),
          randomRecord2.copy(bucketId = 2)
        )

        val thresholdOfBecomingOld = now.minus(1, ChronoUnit.MINUTES)

        for {
          _ <- runTx(NotificationBatchQueueDao.upsertBatch(records))
          oldRecords <- ZIO
            .foreach(Seq.range(0, YdbNotificationBatchQueueDao.shardsCount)) { shardId =>
              runTx(NotificationBatchQueueDao.listOld(shardId, 10, thresholdOfBecomingOld))
            }
            .map(_.flatten)
        } yield assert(oldRecords)(hasSameElements(Seq(randomRecord1, randomRecord2).map(toBatchGroupId)))
      },
      testM("listGroup") {
        val record = rndRecord
        val records = Seq.range(0, 32).map(bucketId => record.copy(bucketId = bucketId))
        for {
          _ <- runTx(NotificationBatchQueueDao.upsertBatch(records))
          groupRecords <- runTx(
            NotificationBatchQueueDao.listGroup(record.destination, record.destinationType, record.eventName)
          )
        } yield assert(groupRecords)(hasSameElements(records.map(truncateNano)))
      },
      testM("removeGroup") {
        val record = rndRecord
        val groupRecords = Seq.range(0, 32).map(bucketId => record.copy(bucketId = bucketId))
        val anotherRecords = Seq.fill(5)(rndRecord)
        for {
          _ <- runTx(NotificationBatchQueueDao.upsertBatch(groupRecords ++ anotherRecords))
          _ <- runTx(
            NotificationBatchQueueDao.removeGroup(record.destination, record.destinationType, record.eventName)
          )
          leftRecords <- runTx(SupportDao.listAll())
        } yield assert(leftRecords)(hasSameElements(anotherRecords.map(truncateNano)))
      }
    ).provideCustomLayer {
      TestYdb.ydb >>> (YdbNotificationBatchQueueDao.live ++ SupportDao.live ++ Ydb.txRunner) ++ Clock.live
    } @@ sequential @@ shrinks(0)
  }
}
