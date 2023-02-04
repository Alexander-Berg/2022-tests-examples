package vertis.pica.tasks

import common.clients.avatars.model.{AvatarsCoordinates, ImageInfo, ImageMeta, OrigSize}
import common.clients.avatars.model.AvatarsError
import common.clients.avatars.model.AvatarsError.ErrorResponse
import ru.yandex.vertis.proto.util.convert.ProtoConversions.RichInstant
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.pica.event.event.{ImageEventHeader, ImageReloadingEvent}
import ru.yandex.vertis.pica.model.model.{Image, ImageServiceInfo}
import ru.yandex.vertis.pica.model.model
import ru.yandex.vertis.ydb.Ydb
import vertis.pica.ProcessingStatus
import vertis.pica.dao.PicaYdbSpecBase
import vertis.pica.model.{ImageId, ImageRecord, ProcessingStatuses, Url}
import vertis.pica.service.avatars.{AvatarsService, TestAvatarsClient}
import vertis.pica.service.{BrokerQueueService, ImageService, ReloadImageQueueService}
import vertis.zio.test.ZioSpecBase
import zio.duration.Duration
import zio._

import java.time.Instant
import scala.concurrent.duration

class ReloadImageQueueTaskIntSpec extends ZioSpecBase with PicaYdbSpecBase {
  import Ydb.ops._

  private val avatarsClient = new TestAvatarsClient {

    override def put(
        url: String,
        namespace: String,
        imageName: Option[String],
        ttl: Option[duration.Duration]): IO[AvatarsError, ImageInfo] = UIO {
      ImageInfo(namespace, 111, imageName.getOrElse(""), ImageMeta(OrigSize(10, 10), None, "", true, None), Map.empty)
    }

    override def delete(coordinates: AvatarsCoordinates): IO[AvatarsError, String] =
      UIO("ok")
  }

  private val failingAvatarsClient = new TestAvatarsClient {

    override def put(
        url: String,
        namespace: String,
        imageName: Option[String],
        ttl: Option[duration.Duration]): IO[AvatarsError, ImageInfo] =
      IO.fail(avaError)

    override def delete(coordinates: AvatarsCoordinates): IO[AvatarsError, String] =
      UIO("ok")
  }

  private lazy val reloadImageService = new ReloadImageQueueService(queueStorage, testNamespace)
  private lazy val brokerService = new BrokerQueueService(queueStorage, testNamespace)
  private lazy val imageService = new ImageService(storage, brokerService)

  private def getTask(avatarsService: AvatarsService) = {
    val downloader = new DownloadTaskProcessor with AvatarsErrorImageProcessor {
      override def avatars: AvatarsService = avatarsService

      override def policy: ReschedulingPolicy = new ReschedulingPolicy {
        override def retryCount: Int = 3
        override protected def fixedDelay: Duration = java.time.Duration.ofSeconds(10)
        override protected def rescheduleAfter(failureNumber: Int): Duration = java.time.Duration.ofSeconds(10)
      }
      override protected def qualifier: String = testNamespace.toString

      override protected def prometheusRegistry: PrometheusRegistry =
        ReloadImageQueueTaskIntSpec.this.prometheusRegistry
    }

    new ReloadImageQueueTask(
      reloadImageService,
      prometheusRegistry,
      downloader,
      avatarsService,
      imageService,
      testNamespace
    )
  }

  private def createImage(
      imageId: ImageId,
      now: Instant,
      status: ProcessingStatus = ProcessingStatuses.Reloading): ImageRecord = {
    val imageName = "ok_name"
    val image = Image("111", imageName, testNamespace.toString, "", true)
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

  "ReloadImageQueueTask" should {

    "reload image then drop from queue" in ydbTest {
      val newName = "new_ok_name"

      for {
        now <- clock.instant
        protoTs = now.toProtoTimestamp

        originalUrl = "http://www.yandex.ru/ok.jpeg"
        url <- Url.create(originalUrl)
        imageId = ImageId.apply(url)
        ir = createImage(imageId, now)

        _ <- imageService.upsert(ir)

        event = ImageReloadingEvent(
          ImageEventHeader(protoTs, protoNamespace, ir.id, url.originalUrl),
          ir.image.get,
          newName
        )
        _ <- runTx(reloadImageService.addElement(event).withAutoCommit)

        partition <- reloadImageService.partition(event)
        avatarsService <- createAvatarsService(avatarsClient)
        _ <- getTask(avatarsService).run(partition)

        stored <- imageService.get(imageId)
        _ <- check(stored should not be empty)
        storedImage = stored.get

        _ <- check(storedImage.image.get.name shouldBe newName)

        elements <- runTx(reloadImageService.pollElements(now, partition, 100).withAutoCommit)
        _ <- check(elements shouldBe empty)
      } yield ()
    }

    "drop event if no one image found" in ydbTest {
      val newName = "new_none_name"

      for {
        now <- clock.instant
        protoTs = now.toProtoTimestamp

        originalUrl = "http://www.yandex.ru/none.jpeg"
        url <- Url.create(originalUrl)
        imageId = ImageId.apply(url)
        ir = createImage(imageId, now)

        event = ImageReloadingEvent(
          ImageEventHeader(protoTs, protoNamespace, ir.id, url.originalUrl),
          ir.image.get,
          newName
        )
        _ <- runTx(reloadImageService.addElement(event).withAutoCommit)

        partition <- reloadImageService.partition(event)
        avatarsService <- createAvatarsService(avatarsClient)
        _ <- getTask(avatarsService).run(partition)

        stored <- imageService.get(imageId)
        _ <- check(stored shouldBe empty)

        elements <- runTx(reloadImageService.pollElements(now, partition, 100).withAutoCommit)
        _ <- check(elements shouldBe empty)
      } yield ()
    }

    "drop event for no-reloading image" in ydbTest {
      val newName = "new_never_name"

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
          ir.image.get,
          newName
        )
        _ <- runTx(reloadImageService.addElement(event).withAutoCommit)

        partition <- reloadImageService.partition(event)
        avatarsService <- createAvatarsService(avatarsClient)
        _ <- getTask(avatarsService).run(partition)

        stored <- imageService.get(imageId)
        _ <- check(stored should not be empty)
        storedImage = stored.get

        _ <- check(storedImage.image.get.name shouldBe ir.imageName)
        _ <- check(storedImage.image.get.name should not be newName)

        elements <- runTx(reloadImageService.pollElements(now, partition, 100).withAutoCommit)
        _ <- check(elements shouldBe empty)
      } yield ()
    }

    "reschedule if retried by ava processor" in ydbTest {
      val newName = "new_retry_name"

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
          newName,
          Seq(avaProtoError)
        )
        _ <- runTx(reloadImageService.addElement(event).withAutoCommit)

        partition <- reloadImageService.partition(event)
        avatarsService <- createAvatarsService(failingAvatarsClient)
        _ <- getTask(avatarsService).run(partition)

        stored <- imageService.get(imageId)
        _ <- check(stored should not be empty)
        storedImage = stored.get

        _ <- check(storedImage.image.get.name shouldBe ir.imageName)

        elements <- runTx(reloadImageService.pollElements(now.plusSeconds(20), partition, 100).withAutoCommit)
        _ <- check(elements should not be empty)
        event = elements.head
        _ <- check(event.timestamp.isAfter(now) shouldBe true)
        _ <- check(event.payload.errors should contain theSameElementsAs Seq(avaProtoError, avaProtoError))
      } yield ()
    }

    "drop event if no more retries exceeded" in ydbTest {
      val newName = "new_failed_name"

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
          newName,
          errors
        )
        _ <- runTx(reloadImageService.addElement(event).withAutoCommit)

        partition <- reloadImageService.partition(event)
        avatarsService <- createAvatarsService(failingAvatarsClient)
        _ <- getTask(avatarsService).run(partition)

        stored <- imageService.get(imageId)
        _ <- check(stored should not be empty)
        storedImage = stored.get

        _ <- check(storedImage.image shouldBe empty)
        _ <- check(storedImage.status shouldBe ProcessingStatuses.Failed)
        _ <- check(
          storedImage.errors should contain theSameElementsAs Seq(
            avaProtoError,
            avaProtoError,
            avaProtoError,
            avaProtoError
          )
        )

        elements <- runTx(reloadImageService.pollElements(now.plusSeconds(20), partition, 100).withAutoCommit)
        _ <- check(elements shouldBe empty)
      } yield ()
    }
  }

}
