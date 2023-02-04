package vertis.pica.services

import ru.yandex.vertis.proto.util.convert.ProtoConversions.RichInstant
import ru.yandex.vertis.pica.event.event.Action
import ru.yandex.vertis.pica.model.model.{Image, ImageServiceInfo}
import ru.yandex.vertis.ydb.Ydb
import vertis.pica.ProcessingStatus
import vertis.pica.dao.PicaYdbSpecBase
import vertis.pica.model.{ImageId, ImagePutRequest, ImageRecord, ProcessingStatuses, Url}
import vertis.pica.service.{
  BrokerQueueService,
  ErasureQueueService,
  QueueServices,
  ReloadImageQueueService,
  ReloadMetaQueueService
}
import vertis.pica.services.PicaApiException.{NoSuchImageException, PicaDBException}
import vertis.pica.util.PartitioningUtils
import vertis.ydb.YEnv
import vertis.ydb.partitioning.manual.ManualPartition
import vertis.zio.ServerEnv
import vertis.zio.test.ZioSpecBase
import zio.{RIO, ZIO}
import zio.clock.Clock
import zio.test.environment.{testEnvironment, TestClock}
import zio.duration.Duration.fromScala

import java.util.UUID
import scala.concurrent.duration._

class ImageServiceIntSpec extends ZioSpecBase with PicaYdbSpecBase {
  import Ydb.ops._

  private lazy val brokerQueueService = new BrokerQueueService(queueStorage, testNamespace)
  private lazy val erasureQueueService = new ErasureQueueService(queueStorage, testNamespace)

  private lazy val reloadImageQueueService: ReloadImageQueueService =
    new ReloadImageQueueService(queueStorage, testNamespace)

  private lazy val reloadMetaQueueService: ReloadMetaQueueService =
    new ReloadMetaQueueService(queueStorage, testNamespace)

  private lazy val imageService: ImageService =
    new ImageService(
      storage,
      QueueServices(brokerQueueService, erasureQueueService, reloadImageQueueService, reloadMetaQueueService)
    )

  private def queuePartition(imageId: ImageId): ManualPartition =
    PartitioningUtils.partitionByHost(queueStorage.partitioning)(imageId.url.host)

  private def ydbTestWithClock[E, A](io: ZIO[ServerEnv with YEnv with TestClock, E, A]): Unit =
    super.ydbTest(io.provideSomeLayer[ServerEnv with YEnv](testEnvironment))

  private def record(
      url: String,
      ttl: Option[FiniteDuration] = None,
      status: ProcessingStatus = ProcessingStatuses.Processed): ZIO[Clock, Throwable, (ImageRecord, ImageId)] = {
    for {
      now <- zio.clock.instant
      url <- Url.create(url)
      imageId = ImageId(url)
      imageName = UUID.randomUUID().toString
      expireTime = ttl.map(t => now.plus(t.length, t.unit.toChronoUnit))
      ir = ImageRecord(
        id = imageId.id,
        imageName = imageName,
        originalUrl = imageId.url.originalUrl,
        payload = Some("record_payload"),
        image = Some(Image("group", imageName, testNamespace.toString, "meta", true, None)),
        checkTs = now,
        expireTs = expireTime,
        ttl = ttl,
        highPriority = true,
        status = status,
        serviceInfo = ImageServiceInfo(
          metaUpdateTime = Some(now.toProtoTimestamp),
          processedTime = Some(now.toProtoTimestamp),
          headCheckTime = None,
          expireTime = expireTime.map(_.toProtoTimestamp)
        )
      )
    } yield ir -> imageId
  }

  private def request(
      url: String,
      payload: Option[String] = Some("request_payload"),
      ttl: Option[FiniteDuration] = None,
      force: Boolean = false,
      highPriority: Boolean = false): ZIO[Clock, Throwable, (ImagePutRequest, ImageId)] = {
    for {
      now <- zio.clock.currentDateTime
      url <- Url.create(url)
      imageId = ImageId(url)
      rq = ImagePutRequest(
        imageId = imageId,
        instant = now.toInstant,
        ttl = ttl,
        payload = payload,
        imageName = UUID.randomUUID().toString,
        force = force,
        highPriority = highPriority
      )
    } yield rq -> imageId
  }

  "ImageService.getOrPut" should {

    "create image once" in ydbTest {
      for {
        (rq, imageId) <- request("https://yandex.ru/ImageService/getOrPut/new.jpg")
        result <- imageService.getOrPut(rq).eventually
        created <- runTx(storage.get(imageId).withAutoCommit)
        _ <- check("result")(result shouldBe ImageRecord.queued)
        _ <- check("created")(created.map(_.processingResult) shouldBe Some(ImageRecord.queued))

        partition = queuePartition(imageId)
        queue <- runTx(brokerQueueService.pollElements(rq.instant, partition, 1000).withAutoCommit)
        _ <- check("count")(queue.length shouldBe 1)
        event = queue.head
        _ <- check("element id")(event.payload.imageId shouldBe imageId.id)
        _ <- check("element action")(event.payload.action shouldBe Action.QUEUED)
      } yield ()
    }

    "get already queued image" in ydbTest {
      for {
        (rq, imageId) <- request("https://yandex.ru/ImageService/getOrPut/queued.jpg")
        _ <- runTx(storage.put(rq).withAutoCommit)
        stored <- runTx(storage.get(imageId).withAutoCommit)
        result <- imageService.getOrPut(rq)
        _ <- check(stored.map(_.processingResult) shouldBe Some(result))
      } yield ()
    }

    "get already processed image" in ydbTest {
      val url = "https://yandex.ru/ImageService/getOrPut/processed.jpg"
      for {
        (ir, _) <- record(url)
        _ <- runTx(storage.putImageRecord(ir).withAutoCommit)
        (rq, _) <- request(url)
        result <- imageService.getOrPut(rq)
        _ <- check(result shouldBe ir.processingResult)
      } yield ()
    }

    "replace expired image with queued" in ydbTestWithClock {
      val url = "https://yandex.ru/ImageService/getOrPut/expired.jpg"
      for {
        (ir, _) <- record(url, Some(1.second))
        _ <- runTx(storage.putImageRecord(ir).withAutoCommit)
        _ <- TestClock.adjust(fromScala(2.seconds))
        (newRq, imageId) <- request(url, ttl = Some(10.second))
        result <- imageService.getOrPut(newRq)
        _ <- check(result shouldBe ImageRecord.queued)

        partition = queuePartition(imageId)
        queue <- runTx(brokerQueueService.pollElements(newRq.instant, partition, 1000).withAutoCommit)
        _ <- check("count")(queue.length shouldBe 1)
        event = queue.head
        _ <- check("element id")(event.payload.imageId shouldBe imageId.id)
        _ <- check("element action")(event.payload.action shouldBe Action.QUEUED)
      } yield ()
    }
  }

  "ImageService.getOrPut [force]" should {

    "get already queued image [force]" in ydbTest {
      for {
        (rq, imageId) <- request("https://yandex.ru/ImageService/getOrPut/queued_force.jpg", force = true)
        _ <- runTx(storage.put(rq).withAutoCommit)
        stored <- runTx(storage.get(imageId).withAutoCommit)
        result <- imageService.getOrPut(rq)
        _ <- check(stored.map(_.processingResult) shouldBe Some(result))
      } yield ()
    }

    "get reloading image [force]" in ydbTest {
      val url = "https://yandex.ru/ImageService/getOrPut/reloading_force.jpg"
      for {
        (ir, _) <- record(url, status = ProcessingStatuses.Reloading)
        _ <- runTx(storage.putImageRecord(ir).withAutoCommit)
        (rq, imageId) <- request(url, force = true)
        stored <- runTx(storage.get(imageId).withAutoCommit)
        result <- imageService.getOrPut(rq)
        _ <- check(stored.map(_.processingResult) shouldBe Some(result))
      } yield ()
    }

    def checkForceReloading(url: String, initStatus: ProcessingStatus): RIO[RequestEnv, Unit] = {
      for {
        (ir, imageId) <- record(url, status = initStatus)
        _ <- runTx(storage.putImageRecord(ir).withAutoCommit)
        (rq, _) <- request(url, force = true)

        result <- imageService.getOrPut(rq)
        stored <- runTx(storage.get(imageId).withAutoCommit)
        storedIr = stored.get
        _ <- check(storedIr.processingResult shouldBe result)
        _ <- check(storedIr.status shouldBe ProcessingStatuses.Reloading)

        partition = queuePartition(imageId)
        brokerQueue <- runTx(brokerQueueService.pollElements(rq.instant, partition, 1000).withAutoCommit)
        _ <- check("broker queue count")(brokerQueue.length shouldBe 1)
        brokerEvent = brokerQueue.head
        _ <- check("broker event id")(brokerEvent.payload.imageId shouldBe imageId.id)
        _ <- check("broker event action")(brokerEvent.payload.action shouldBe Action.QUEUED)

        reloadQueue <- runTx(reloadImageQueueService.pollElements(rq.instant, partition, 1000).withAutoCommit)
        _ <- check("reload queue count")(reloadQueue.length shouldBe 1)
        reloadEvent = reloadQueue.head
        _ <- check("reload event id")(reloadEvent.payload.header.imageId shouldBe storedIr.id)
        _ <- check("new image name")(reloadEvent.payload.newImageName should not be storedIr.image.get.name)
        _ <- check("reload event image")(reloadEvent.payload.image shouldBe storedIr.image.get)
      } yield ()
    }

    "update image options during force reloading [force]" in ydbTest {
      val url = "https://yandex.ru/ImageService/getOrPut/processed_force_update.jpg"
      for {
        (ir, _) <- record(url, status = ProcessingStatuses.Processed)
        _ <- runTx(
          storage
            .putImageRecord(ir.copy(ttl = None, expireTs = None, highPriority = false, payload = Some("old_payload")))
            .withAutoCommit
        )

        newPayload = Some("new_payload")
        newTtl = Some(2.days)
        newHighPriority = true
        (rq, imageId) <- request(
          url = url,
          payload = newPayload,
          highPriority = newHighPriority,
          ttl = newTtl,
          force = true
        )

        _ <- imageService.getOrPut(rq)
        // clear queue
        partition = queuePartition(imageId)
        _ <- runTx(brokerQueueService.pollElements(rq.instant, partition, 1000).withAutoCommit)
        _ <- runTx(reloadImageQueueService.pollElements(rq.instant, partition, 1000).withAutoCommit)

        stored <- runTx(storage.get(imageId).withAutoCommit)
        storedIr = stored.get
        _ <- check(storedIr.status shouldBe ProcessingStatuses.Reloading)
        _ <- check(storedIr.highPriority shouldBe newHighPriority)
        _ <- check(storedIr.payload shouldBe newPayload)
        _ <- check(storedIr.ttl shouldBe newTtl)
      } yield ()
    }

    "enqueue reloading event for processed image [force]" in ydbTest {
      checkForceReloading(
        url = "https://yandex.ru/ImageService/getOrPut/processed_force.jpg",
        initStatus = ProcessingStatuses.Processed
      )
    }

    "enqueue reloading event for waiting meta image [force]" in ydbTest {
      checkForceReloading(
        url = "https://yandex.ru/ImageService/getOrPut/waiting_meta_force.jpg",
        initStatus = ProcessingStatuses.WaitingMeta
      )
    }

    "replace failed image with queued [force]" in ydbTest {
      val url = "https://yandex.ru/ImageService/getOrPut/failed_force.jpg"
      for {
        (ir, _) <- record(url, status = ProcessingStatuses.Failed)
        _ <- runTx(storage.putImageRecord(ir).withAutoCommit)
        (rq, imageId) <- request(url, force = true)
        result <- imageService.getOrPut(rq)
        _ <- check(result shouldBe ImageRecord.queued)

        partition = queuePartition(imageId)
        queue <- runTx(brokerQueueService.pollElements(rq.instant, partition, 1000).withAutoCommit)
        _ <- check("queue count")(queue.length shouldBe 1)
        event = queue.head
        _ <- check("queue element id")(event.payload.imageId shouldBe imageId.id)
        _ <- check("queue element action")(event.payload.action shouldBe Action.QUEUED)
      } yield ()
    }

    "replace expired image with queued [force]" in ydbTestWithClock {
      val url = "https://yandex.ru/ImageService/getOrPut/expired_force.jpg"
      for {
        (ir, _) <- record(url, Some(1.second))
        _ <- runTx(storage.putImageRecord(ir).withAutoCommit)
        _ <- TestClock.adjust(fromScala(2.seconds))
        (newRq, imageId) <- request(url, ttl = Some(10.second), force = true)
        result <- imageService.getOrPut(newRq)
        _ <- check(result shouldBe ImageRecord.queued)

        partition = queuePartition(imageId)
        queue <- runTx(brokerQueueService.pollElements(newRq.instant, partition, 1000).withAutoCommit)
        _ <- check("queue count")(queue.length shouldBe 1)
        event = queue.head
        _ <- check("queue element id")(event.payload.imageId shouldBe imageId.id)
        _ <- check("queue element action")(event.payload.action shouldBe Action.QUEUED)
      } yield ()
    }
  }

  "ImageService.get" should {
    "fail with NoSuchImageException on expired" ignore ydbTestWithClock {
      for {
        (rq, imageId) <- request("https://yandex.ru/ImageService/get/expired.jpg", ttl = Some(1.second))
        _ <- runTx(storage.put(rq).withAutoCommit)
        _ <- TestClock.adjust(fromScala(2.seconds))
        _ <- checkFailed[RequestEnv, Any, NoSuchImageException](imageService.get(imageId))
      } yield ()
    }

    "fail with NoSuchImageException if nothing found" in ydbTest {
      for {
        url <- Url.create("https://yandex.ru/ImageService/get/none.jpg")
        imageId = ImageId(url)
        _ <- checkFailed[RequestEnv, Any, NoSuchImageException](imageService.get(imageId))
      } yield ()
    }

    "return queued result" in ydbTest {
      for {
        (rq, imageId) <- request("https://yandex.ru/ImageService/get/queued.jpg")
        _ <- runTx(storage.put(rq).withAutoCommit)
        stored <- runTx(storage.get(imageId).withAutoCommit)
        result <- imageService.get(imageId)
        _ <- check(stored.get.processingResult shouldBe result)
      } yield ()
    }

    "return processed result" in ydbTest {
      for {
        (ir, imageId) <- record("https://yandex.ru/ImageService/get/processed.jpg")
        _ <- runTx(storage.putImageRecord(ir).withAutoCommit)
        result <- imageService.get(imageId)
        _ <- check(result shouldBe ir.processingResult)
      } yield ()
    }

  }

  "ImageService.delete" should {

    "delete from storage and send to queues" in ydbTest {
      for {
        (ir, imageId) <- record("https://yandex.ru/ImageService/delete/ok.jpg")
        _ <- runTx(storage.putImageRecord(ir).withAutoCommit)
        _ <- imageService.delete(imageId)
        result <- runTx(storage.get(imageId).withAutoCommit)
        _ <- check(result shouldBe None)

        now <- zio.clock.instant

        partition = queuePartition(imageId)
        brokerQueue <- runTx(brokerQueueService.pollElements(now, partition, 1000).withAutoCommit)
        _ <- check("broker queue count")(brokerQueue.length shouldBe 1)
        brokerEvent = brokerQueue.head
        _ <- check("broker queue element id")(brokerEvent.payload.imageId shouldBe imageId.id)
        _ <- check("broker queue element action")(brokerEvent.payload.action shouldBe Action.DELETED)

        partition = queuePartition(imageId)
        erasureQueue <- runTx(erasureQueueService.pollElements(now, partition, 1000).withAutoCommit)
        _ <- check("erasure queue count")(erasureQueue.length shouldBe 1)
        erasureEvent = erasureQueue.head
        _ <- check("erasure queue element id")(erasureEvent.payload.header.imageId shouldBe imageId.id)
      } yield ()
    }

    "fail with NoSuchImageException if no one images found" in ydbTest {
      for {
        url <- Url.create("https://yandex.ru/ImageService/delete/none.jpg")
        imageId = ImageId(url)
        _ <- checkFailed[RequestEnv, Any, NoSuchImageException](imageService.delete(imageId))
      } yield ()
    }
  }

  "ImageService.reloadMeta" should {

    "fail with NoSuchImageException if no one images found" in ydbTest {
      for {
        url <- Url.create("https://yandex.ru/ImageService/reloadMeta/none.jpg")
        imageId = ImageId(url)
        _ <- checkFailed[RequestEnv, Any, NoSuchImageException](imageService.reloadMeta(imageId))
      } yield ()
    }

    def checkOnUnexpected(url: String, status: ProcessingStatus): RIO[RequestEnv, Unit] = {
      for {
        (ir, imageId) <- record(url, status = status)
        _ <- runTx(storage.putImageRecord(ir).withAutoCommit)
        now <- zio.clock.instant
        partition = queuePartition(imageId)

        _ <- checkFailed[RequestEnv, Any, PicaDBException](imageService.reloadMeta(imageId))
        reloadQueue <- runTx(reloadMetaQueueService.pollElements(now.plusSeconds(100), partition, 1000).withAutoCommit)
        brokerQueue <- runTx(reloadMetaQueueService.pollElements(now.plusSeconds(100), partition, 1000).withAutoCommit)
        _ <- check(reloadQueue shouldBe empty)
        _ <- check(brokerQueue shouldBe empty)
      } yield ()
    }

    "fail if Queued" in ydbTest {
      checkOnUnexpected("https://yandex.ru/ImageService/reloadMeta/queued.jpg", ProcessingStatuses.Queued)
    }

    "fail if Failed" in ydbTest {
      checkOnUnexpected("https://yandex.ru/ImageService/reloadMeta/failed.jpg", ProcessingStatuses.Failed)
    }

    def checkDoNothing(url: String, status: ProcessingStatus): RIO[RequestEnv, Unit] = {
      for {
        (ir, imageId) <- record(url, status = status)
        _ <- runTx(storage.putImageRecord(ir).withAutoCommit)
        result <- imageService.reloadMeta(imageId)
        _ <- check(result shouldBe ir.processingResult)
        now <- zio.clock.instant
        partition = queuePartition(imageId)
        reloadQueue <- runTx(reloadMetaQueueService.pollElements(now.plusSeconds(100), partition, 1000).withAutoCommit)
        brokerQueue <- runTx(reloadMetaQueueService.pollElements(now.plusSeconds(100), partition, 1000).withAutoCommit)
        _ <- check(reloadQueue shouldBe empty)
        _ <- check(brokerQueue shouldBe empty)
      } yield ()
    }

    "do nothing with WaitingMeta" in ydbTest {
      checkDoNothing("https://yandex.ru/ImageService/reloadMeta/waiting_meta.jpg", ProcessingStatuses.WaitingMeta)
    }

    "do nothing with Reloading" in ydbTest {
      checkDoNothing("https://yandex.ru/ImageService/reloadMeta/reloading.jpg", ProcessingStatuses.Reloading)
    }

    "enqueue reloading meta event for Processed" in ydbTest {
      val url = "https://yandex.ru/ImageService/reloadMeta/processed.jpg"
      for {
        (ir, imageId) <- record(url)
        _ <- runTx(storage.putImageRecord(ir).withAutoCommit)
        result <- imageService.reloadMeta(imageId)
        _ <- check(result shouldBe ir.copy(status = ProcessingStatuses.Reloading).processingResult)

        now <- zio.clock.instant
        partition = queuePartition(imageId)
        reloadQueue <- runTx(reloadMetaQueueService.pollElements(now.plusSeconds(100), partition, 1000).withAutoCommit)
        brokerQueue <- runTx(brokerQueueService.pollElements(now.plusSeconds(100), partition, 1000).withAutoCommit)

        _ <- check(brokerQueue.length shouldBe 1)
        brokerEvent = brokerQueue.head
        _ <- check(brokerEvent.payload.imageId shouldBe imageId.id)
        _ <- check(brokerEvent.payload.action shouldBe Action.QUEUED)

        _ <- check(reloadQueue.length shouldBe 1)
        reloadEvent = reloadQueue.head
        _ <- check(reloadEvent.payload.header.imageId shouldBe imageId.id)
        _ <- check(reloadEvent.payload.errors shouldBe empty)
      } yield ()
    }
  }
}
