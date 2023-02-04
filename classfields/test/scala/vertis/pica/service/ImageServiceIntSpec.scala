package vertis.pica.service

import ru.yandex.vertis.pica.event.event.{Action, ImageProcessingEvent}
import ru.yandex.vertis.ydb.Ydb
import vertis.core.utils.NoWarnFilters
import vertis.pica.Gens.{asProducer, ImageRecordGen}
import vertis.pica.ProcessingStatus
import vertis.pica.dao.PicaYdbSpecBase
import vertis.pica.model.{ImageId, ImageRecord, ProcessingStatuses, Url}
import vertis.ydb.partitioning.manual.ManualPartition
import vertis.ydb.queue.storage.QueueElement
import vertis.zio.test.ZioSpecBase
import zio.Task

import java.time.temporal.ChronoUnit
import scala.annotation.nowarn

class ImageServiceIntSpec extends ZioSpecBase with PicaYdbSpecBase {
  import Ydb.ops._

  private lazy val brokerQueueService = new BrokerQueueService(queueStorage, testNamespace)
  private lazy val imageService: ImageService = new ImageService(storage, brokerQueueService)

  private def record(): Task[(ImageId, ManualPartition, ImageRecord)] = {
    val imageRecord = ImageRecordGen.next
    Url.create(imageRecord.originalUrl).map { url =>
      (ImageId(url), getPartition(url), imageRecord)
    }
  }

  @nowarn(NoWarnFilters.OtherMatchAnalysis)
  private def checkBrokerQueue(
      queue: Seq[QueueElement[ImageProcessingEvent]],
      imageId: ImageId,
      recordStatusOpt: Option[ProcessingStatus] = None) = {
    for {
      _ <- check(queue.length shouldBe 1)
      event = queue.head
      _ <- check(event.payload.imageId shouldBe imageId.id)
      expectedAction = recordStatusOpt match {
        case Some(ProcessingStatuses.Queued) => Action.QUEUED
        case Some(ProcessingStatuses.Failed) => Action.FAILED
        case Some(ProcessingStatuses.Reloading) => Action.QUEUED
        case Some(ProcessingStatuses.Processed) => Action.PROCESSED
        case Some(ProcessingStatuses.WaitingMeta) => Action.WAITING_META

        case None => Action.DELETED
      }
      _ <- check(event.payload.action shouldBe expectedAction)
    } yield ()
  }

  "ImageService" should {
    "upsert should create new image record" in ydbTest {
      for {
        now <- zio.clock.instant
        (imageId, partition, imageRecord) <- record()
        _ <- imageService.upsert(imageRecord)

        storedOpt <- runTx(storage.get(imageId).withAutoCommit)
        _ <- check(storedOpt.get shouldBe imageRecord)

        queue <- runTx(brokerQueueService.pollElements(now.plusSeconds(100), partition, 100).withAutoCommit)
        _ <- checkBrokerQueue(queue, imageId, Some(imageRecord.status))
      } yield ()
    }

    "upsert should replace stored image record" in ydbTest {
      for {
        now <- zio.clock.instant
        (_, _, prevRecord) <- record()
        _ <- runTx(storage.putImageRecord(prevRecord).withAutoCommit)

        (imageId, partition, newRecord) <- record()
        _ <- imageService.upsert(newRecord)

        storedOpt <- runTx(storage.get(imageId).withAutoCommit)
        _ <- check(storedOpt.get shouldBe newRecord)

        queue <- runTx(brokerQueueService.pollElements(now.plusSeconds(100), partition, 100).withAutoCommit)
        _ <- checkBrokerQueue(queue, imageId, Some(newRecord.status))
      } yield ()
    }

    "delete if expired" in ydbTest {
      def modifyWithPartition(imageRecord: ImageRecord): Task[(ImageId, ManualPartition, ImageRecord)] = {
        val originalUrl = imageRecord.originalUrl
        val newUrlWithTheSameHost = s"$originalUrl/new/segment"
        for {
          newUrl <- Url.create(newUrlWithTheSameHost)
          newImageId = ImageId(newUrl)
          modifiedImage = imageRecord.copy(originalUrl = newUrl.originalUrl, id = newImageId.id)
        } yield (newImageId, getPartition(newUrl), modifiedImage)
      }

      for {
        now <- zio.clock.instant
        (imageId, partition, imageRecord) <- record()
        expiredImageRecord = imageRecord.copy(expireTs = Some(now))
        (newImageId, samePartition, anotherRecord) <- modifyWithPartition(imageRecord)
        nonExpiredRecord = anotherRecord.copy(expireTs = Some(now.plusSeconds(1000).truncatedTo(ChronoUnit.MICROS)))
        _ <- check("assert same partition.")(samePartition shouldBe partition)
        _ <- runTx(storage.putImageRecord(expiredImageRecord).withAutoCommit)
        _ <- runTx(storage.putImageRecord(nonExpiredRecord).withAutoCommit)

        _ <- imageService.deleteIfExpired(partition, Seq(imageId, newImageId), now.plusSeconds(100))

        none <- runTx(storage.get(imageId).withAutoCommit)
        _ <- check(none shouldBe empty)

        storedOpt <- runTx(storage.get(newImageId).withAutoCommit)
        _ <- check(storedOpt.get shouldBe nonExpiredRecord)

        queue <- runTx(brokerQueueService.pollElements(now.plusSeconds(100), partition, 100).withAutoCommit)
        _ <- checkBrokerQueue(queue, imageId)
      } yield ()
    }
  }

}
