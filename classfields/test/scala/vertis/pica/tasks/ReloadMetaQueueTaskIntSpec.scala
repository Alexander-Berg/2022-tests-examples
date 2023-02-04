package vertis.pica.tasks

import common.clients.avatars.model.{AvatarsCoordinates, AvatarsError, ImageMeta, OrigSize}
import common.clients.avatars.model.AvatarsError.ErrorResponse
import ru.yandex.vertis.proto.util.convert.ProtoConversions.RichInstant
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.pica.event.event.{ImageEventHeader, ImageReloadingEvent}
import ru.yandex.vertis.pica.model.model
import ru.yandex.vertis.pica.model.model.{Image, ImageServiceInfo}
import ru.yandex.vertis.ydb.Ydb
import vertis.pica.ProcessingStatus
import vertis.pica.dao.PicaYdbSpecBase
import vertis.pica.model.{ImageId, ImageRecord, ProcessingStatuses, Url}
import vertis.pica.service.avatars.{AvatarsService, TestAvatarsClient}
import vertis.pica.service.{BrokerQueueService, ImageService, ReloadMetaQueueService}
import vertis.zio.test.ZioSpecBase
import zio.{clock, IO, UIO}
import zio.duration.Duration

import java.time.Instant

class ReloadMetaQueueTaskIntSpec extends ZioSpecBase with PicaYdbSpecBase {
  import Ydb.ops._

  private lazy val reloadMetaService = new ReloadMetaQueueService(queueStorage, testNamespace)
  private lazy val brokerService = new BrokerQueueService(queueStorage, testNamespace)
  private lazy val imageService = new ImageService(storage, brokerService)

  private val avatarsClient = new TestAvatarsClient {

    override def getMeta(coordinates: AvatarsCoordinates): IO[AvatarsError, ImageMeta] =
      UIO(ImageMeta(OrigSize(10, 10), None, NewMeta, isFinished = true, expireAt = None))

    override def deleteMeta(coordinates: AvatarsCoordinates): IO[AvatarsError, String] =
      UIO("ok")
  }

  private def failingAvatarsClient(error: AvatarsError) = new TestAvatarsClient {

    override def getMeta(coordinates: AvatarsCoordinates): IO[AvatarsError, ImageMeta] =
      UIO(ImageMeta(OrigSize(10, 10), None, NewMeta, isFinished = true, expireAt = None))

    override def deleteMeta(coordinates: AvatarsCoordinates): IO[AvatarsError, String] =
      IO.fail(error)
  }

  private def getTask(avatarsService: AvatarsService) = {
    val downloader = new LoadMetaTaskProcessor with LoadMetaAvatarsErrorProcessor {
      override def avatars: AvatarsService = avatarsService

      override def policy: ReschedulingPolicy = new ReschedulingPolicy {
        override def retryCount: Int = 3
        override protected def fixedDelay: Duration = java.time.Duration.ofSeconds(10)
        override protected def rescheduleAfter(failureNumber: Int): Duration = java.time.Duration.ofSeconds(10)
      }
      override protected def qualifier: String = testNamespace.toString

      override protected def prometheusRegistry: PrometheusRegistry =
        ReloadMetaQueueTaskIntSpec.this.prometheusRegistry
    }

    new ReloadMetaQueueTask(
      reloadMetaService,
      prometheusRegistry,
      downloader,
      imageService,
      testNamespace
    )
  }

  private val OldMeta = "old_meta"
  private val NewMeta = "new_meta"

  private def createImage(
      imageId: ImageId,
      now: Instant,
      status: ProcessingStatus = ProcessingStatuses.Reloading): ImageRecord = {
    val imageName = "ok_name"
    val image = Image("111", imageName, testNamespace.toString, OldMeta, metaFinished = true)
    ImageRecord(
      id = imageId.id,
      imageName = imageName,
      originalUrl = imageId.url.originalUrl,
      payload = Some("image_payload"),
      image = Some(image),
      checkTs = now,
      expireTs = None,
      ttl = None,
      highPriority = false,
      status = status,
      serviceInfo = ImageServiceInfo(),
      errors = Seq.empty
    )
  }

  private def avaError = ErrorResponse(434, "error")
  private def avaProtoError = model.AvatarsError(avaError.body, avaError.status)

  "ReloadMetaQueueTask" should {

    "reload meta then drop from queue" in ydbTest {
      for {
        now <- clock.instant
        protoTs = now.toProtoTimestamp

        originalUrl = "http://www.yandex.ru/ok.jpeg"
        url <- Url.create(originalUrl)
        imageId = ImageId(url)
        ir = createImage(imageId, now)

        _ <- imageService.upsert(ir)

        event = ImageReloadingEvent(
          ImageEventHeader(protoTs, protoNamespace, ir.id, url.originalUrl),
          ir.image.get
        )
        _ <- runTx(reloadMetaService.addElement(event).withAutoCommit)

        partition <- reloadMetaService.partition(event)
        avatarsService <- createAvatarsService(avatarsClient)
        _ <- getTask(avatarsService).run(partition)

        stored <- imageService.get(imageId)
        _ <- check(stored should not be empty)
        storedImage = stored.get
        _ <- check(storedImage.image.get.meta shouldBe NewMeta)

        elements <- runTx(reloadMetaService.pollElements(now, partition, 100).withAutoCommit)
        _ <- check(elements shouldBe empty)
      } yield ()
    }

    "reload meta if meta was already deleted from avatars then drop from queue" in ydbTest {
      val avatarsClient = failingAvatarsClient(AvatarsError.ErrorResponse(404, "msg"))

      for {
        now <- clock.instant
        protoTs = now.toProtoTimestamp

        originalUrl = "http://www.yandex.ru/ok_with_meta_deleted.jpeg"
        url <- Url.create(originalUrl)
        imageId = ImageId(url)
        ir = createImage(imageId, now)

        _ <- imageService.upsert(ir)

        event = ImageReloadingEvent(
          ImageEventHeader(protoTs, protoNamespace, ir.id, url.originalUrl),
          ir.image.get
        )
        _ <- runTx(reloadMetaService.addElement(event).withAutoCommit)

        partition <- reloadMetaService.partition(event)
        avatarsService <- createAvatarsService(avatarsClient)
        _ <- getTask(avatarsService).run(partition)

        stored <- imageService.get(imageId)
        _ <- check(stored should not be empty)
        storedImage = stored.get
        _ <- check(storedImage.image.get.meta shouldBe NewMeta)

        elements <- runTx(reloadMetaService.pollElements(now, partition, 100).withAutoCommit)
        _ <- check(elements shouldBe empty)
      } yield ()
    }

    "drop event if no one image found" in ydbTest {
      for {
        now <- clock.instant
        protoTs = now.toProtoTimestamp

        originalUrl = "http://www.yandex.ru/none.jpeg"
        url <- Url.create(originalUrl)
        imageId = ImageId.apply(url)
        ir = createImage(imageId, now)

        event = ImageReloadingEvent(
          ImageEventHeader(protoTs, protoNamespace, ir.id, url.originalUrl),
          ir.image.get
        )
        _ <- runTx(reloadMetaService.addElement(event).withAutoCommit)

        partition <- reloadMetaService.partition(event)
        avatarsService <- createAvatarsService(avatarsClient)
        _ <- getTask(avatarsService).run(partition)

        stored <- imageService.get(imageId)
        _ <- check(stored shouldBe empty)

        elements <- runTx(reloadMetaService.pollElements(now, partition, 100).withAutoCommit)
        _ <- check(elements shouldBe empty)
      } yield ()
    }

    "drop event for no-reloading image" in ydbTest {
      for {
        now <- clock.instant
        protoTs = now.toProtoTimestamp

        originalUrl = "http://www.yandex.ru/never.jpeg"
        url <- Url.create(originalUrl)
        imageId = ImageId.apply(url)
        ir = createImage(imageId, now, ProcessingStatuses.Processed)

        _ <- imageService.upsert(ir)

        event = ImageReloadingEvent(
          ImageEventHeader(protoTs, protoNamespace, ir.id, url.originalUrl),
          ir.image.get
        )
        _ <- runTx(reloadMetaService.addElement(event).withAutoCommit)

        partition <- reloadMetaService.partition(event)
        avatarsService <- createAvatarsService(avatarsClient)
        _ <- getTask(avatarsService).run(partition)

        stored <- imageService.get(imageId)
        _ <- check(stored should not be empty)
        storedImage = stored.get

        _ <- check(storedImage.image.get.meta shouldBe OldMeta)

        elements <- runTx(reloadMetaService.pollElements(now, partition, 100).withAutoCommit)
        _ <- check(elements shouldBe empty)

      } yield ()
    }

    "reschedule if retried by ava processor" in ydbTest {
      val avatarsClient = failingAvatarsClient(avaError)

      for {
        now <- clock.instant
        protoTs = now.toProtoTimestamp

        originalUrl = "http://www.yandex.ru/retry.jpeg"
        url <- Url.create(originalUrl)
        imageId = ImageId.apply(url)
        ir = createImage(imageId, now)

        _ <- imageService.upsert(ir)

        event = ImageReloadingEvent(
          ImageEventHeader(protoTs, protoNamespace, ir.id, url.originalUrl),
          ir.image.get,
          errors = Seq(avaProtoError)
        )
        _ <- runTx(reloadMetaService.addElement(event).withAutoCommit)

        partition <- reloadMetaService.partition(event)
        avatarsService <- createAvatarsService(avatarsClient)
        _ <- getTask(avatarsService).run(partition)

        stored <- imageService.get(imageId)
        _ <- check(stored should not be empty)
        storedImage = stored.get

        _ <- check(storedImage.image.get.meta shouldBe OldMeta)

        elements <- runTx(reloadMetaService.pollElements(now.plusSeconds(20), partition, 100).withAutoCommit)
        _ <- check(elements should not be empty)
        event = elements.head
        _ <- check(event.timestamp.isAfter(now) shouldBe true)
        _ <- check(event.payload.errors should contain theSameElementsAs Seq(avaProtoError, avaProtoError))
      } yield ()
    }

    "drop event if no more retries exceeded" in ydbTest {
      val avatarsClient = failingAvatarsClient(avaError)

      for {
        now <- clock.instant
        protoTs = now.toProtoTimestamp

        originalUrl = "http://www.yandex.ru/failed.jpeg"
        url <- Url.create(originalUrl)
        imageId = ImageId.apply(url)
        ir = createImage(imageId, now)

        _ <- imageService.upsert(ir)

        errors = Seq(avaProtoError, avaProtoError, avaProtoError)
        event = ImageReloadingEvent(
          ImageEventHeader(protoTs, protoNamespace, ir.id, url.originalUrl),
          ir.image.get,
          errors = errors
        )
        _ <- runTx(reloadMetaService.addElement(event).withAutoCommit)

        partition <- reloadMetaService.partition(event)
        avatarsService <- createAvatarsService(avatarsClient)
        _ <- getTask(avatarsService).run(partition)

        stored <- imageService.get(imageId)
        _ <- check(stored should not be empty)
        storedImage = stored.get

        _ <- check(storedImage.image.get.meta shouldBe OldMeta)
        _ <- check(storedImage.status shouldBe ProcessingStatuses.Failed)
        _ <- check(
          storedImage.errors should contain theSameElementsAs Seq(
            avaProtoError,
            avaProtoError,
            avaProtoError,
            avaProtoError
          )
        )

        elements <- runTx(reloadMetaService.pollElements(now.plusSeconds(20), partition, 100).withAutoCommit)
        _ <- check(elements shouldBe empty)
      } yield ()
    }
  }
}
