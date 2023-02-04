package ru.yandex.vertis.general.gost.storage.test

import java.time.Instant

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.gost.model.sheduler.Stage.StageId
import ru.yandex.vertis.general.gost.model.testkit.OfferGen
import ru.yandex.vertis.general.gost.storage.StageQueueDao
import ru.yandex.vertis.general.gost.storage.StageQueueDao.{Entry, OfferEntry, QueueMetrics}
import ru.yandex.vertis.general.gost.storage.ydb.scheduler.YdbStageQueueDao
import zio.ZIO
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object YdbStageQueueDaoSpec extends DefaultRunnableSpec {

  override def spec = {
    suite("YdbStageQueueDao")(
      testM("Добавление записей в очередь их просмотр") {
        (checkNM(1): CheckVariants.CheckNM)(Gen.listOfN(15)(OfferGen.anyOfferId).noShrink) { offerIds =>
          val firstStageFirstShardOfferIds = offerIds.take(5)
          val firstStageSecondShardOfferIds = offerIds.slice(5, 10)
          val secondStageOfferIds = offerIds.drop(10)
          val firstStageFirstShardEntries = firstStageFirstShardOfferIds.zipWithIndex.map { case (offerId, idx) =>
            Entry(offerId, Instant.ofEpochMilli(idx))
          }
          val firstStageSecondShardEntries = firstStageSecondShardOfferIds.zipWithIndex.map { case (offerId, idx) =>
            Entry(offerId, Instant.ofEpochMilli(idx))
          }
          val secondStageEntries = secondStageOfferIds.zipWithIndex.map { case (offerId, idx) =>
            Entry(offerId, Instant.ofEpochMilli(idx))
          }
          val firstStageId = StageId("a")
          val secondStageId = StageId("b")
          for {
            _ <- ZIO.foreach_(firstStageFirstShardEntries) { entry =>
              runTx(StageQueueDao.offer(1, firstStageId, entry.offerId, entry.timestamp))
            }
            _ <- runTx(
              StageQueueDao.offer(
                firstStageSecondShardEntries.map(e => OfferEntry(2, firstStageId, e.offerId, e.timestamp))
              )
            )
            _ <- ZIO.foreach_(secondStageEntries) { entry =>
              runTx(StageQueueDao.offer(1, secondStageId, entry.offerId, entry.timestamp))
            }
            firstBatch <- runTx(StageQueueDao.peek(1, firstStageId, 3, Instant.ofEpochMilli(4)))
            secondBatch <- runTx(StageQueueDao.peek(1, firstStageId, 10, Instant.ofEpochMilli(4)))
          } yield assert(firstBatch)(equalTo(firstStageFirstShardEntries.take(3))) &&
            assert(secondBatch)(equalTo(firstStageFirstShardEntries.take(4)))
        }
      },
      testM("Перезапись таймстампа и просмотр таймстампа") {
        (checkNM(1): CheckVariants.CheckNM)(OfferGen.anyOfferId) { offerId =>
          val stageId = StageId("second_test")
          val shardId = 3
          for {
            initialTimestamp <- runTx(StageQueueDao.getTimestamp(shardId, stageId, offerId))
            _ <- runTx(StageQueueDao.offer(shardId, stageId, offerId, Instant.ofEpochMilli(1)))
            _ <- runTx(StageQueueDao.offer(shardId, stageId, offerId, Instant.ofEpochMilli(3)))
            _ <- runTx(StageQueueDao.offer(shardId, stageId, offerId, Instant.ofEpochMilli(1)))
            _ <- runTx(StageQueueDao.offer(shardId, stageId, offerId, Instant.ofEpochMilli(2)))
            timestampAfterRewrites <- runTx(StageQueueDao.getTimestamp(shardId, stageId, offerId))
          } yield assert(initialTimestamp)(isNone) &&
            assert(timestampAfterRewrites)(isSome(equalTo(Instant.ofEpochMilli(2))))
        }
      },
      testM("Удаление записи из очереди") {
        (checkNM(1): CheckVariants.CheckNM)(OfferGen.anyOfferId) { offerId =>
          val stageId = StageId("third_test")
          val shardId = 4
          for {
            _ <- runTx(StageQueueDao.offer(shardId, stageId, offerId, Instant.ofEpochMilli(1)))
            initialTimestamp <- runTx(StageQueueDao.getTimestamp(shardId, stageId, offerId))
            initialQueue <- runTx(StageQueueDao.peek(shardId, stageId, 2, Instant.ofEpochMilli(2)))
            _ <- runTx(StageQueueDao.delete(shardId, stageId, offerId))
            timestampAfterDeletion <- runTx(StageQueueDao.getTimestamp(shardId, stageId, offerId))
            queueAfterDeletion <- runTx(StageQueueDao.peek(shardId, stageId, 2, Instant.ofEpochMilli(2)))
          } yield assert(initialTimestamp)(isSome(equalTo(Instant.ofEpochMilli(1)))) &&
            assert(initialQueue)(equalTo(List(Entry(offerId, Instant.ofEpochMilli(1))))) &&
            assert(timestampAfterDeletion)(isNone) &&
            assert(queueAfterDeletion)(isEmpty)
        }
      },
      testM("Получение метрик очередей") {
        (checkNM(1): CheckVariants.CheckNM)(OfferGen.anyOfferId) { offerId =>
          val firstStageId = StageId("fourth_test_1")
          val secondStageId = StageId("fourth_test_2")
          for {
            _ <- runTx(StageQueueDao.offer(1, firstStageId, offerId, Instant.ofEpochMilli(2)))
            _ <- runTx(StageQueueDao.offer(2, firstStageId, offerId, Instant.ofEpochMilli(1)))
            _ <- runTx(StageQueueDao.offer(1, secondStageId, offerId, Instant.ofEpochMilli(3)))
            result <- runTx(StageQueueDao.getQueueMetrics)
          } yield assert(result.get(firstStageId))(isSome(equalTo(QueueMetrics(2, Instant.ofEpochMilli(1))))) &&
            assert(result.get(secondStageId))(isSome(equalTo(QueueMetrics(1, Instant.ofEpochMilli(3)))))
        }
      }
    ) @@ shrinks(1) @@ sequential
  }.provideCustomLayerShared {
    TestYdb.ydb >>> (YdbStageQueueDao.live ++ Ydb.txRunner) ++ Clock.live
  }
}
