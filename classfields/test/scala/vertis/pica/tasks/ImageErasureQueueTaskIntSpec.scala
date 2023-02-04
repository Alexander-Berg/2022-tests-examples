package vertis.pica.tasks

import common.clients.avatars.model.{AvatarsCoordinates, AvatarsError}
import ru.yandex.vertis.proto.util.convert.ProtoConversions.RichInstant
import ru.yandex.vertis.pica.event.event.{ImageErasureEvent, ImageEventHeader}
import ru.yandex.vertis.pica.model.model.Image
import vertis.pica.dao.PicaYdbSpecBase
import vertis.pica.service.ErasureQueueService
import vertis.pica.service.avatars.{AvatarsService, TestAvatarsClient}
import vertis.zio.test.ZioSpecBase
import zio.{clock, IO, UIO}

class ImageErasureQueueTaskIntSpec extends ZioSpecBase with PicaYdbSpecBase {

  private lazy val erasureQueueService = new ErasureQueueService(queueStorage, testNamespace)

  private val groupId = 11111.toString

  private def getTask(avatarsService: AvatarsService) =
    new ImageErasureQueueTask(
      erasureQueueService,
      prometheusRegistry,
      avatarsService,
      testNamespace
    )

  "ProcessingImageErasureQueueTask" should {

    "delete from avatars then drop from queue" in ydbTest {

      val avatarsClient = new TestAvatarsClient {
        override def delete(coordinates: AvatarsCoordinates): IO[AvatarsError, String] =
          UIO("ok")
      }

      for {
        now <- clock.instant
        protoTs = now.toProtoTimestamp
        image = Image(groupId, "ok_name", testNamespace.toString)
        event = ImageErasureEvent(
          ImageEventHeader(protoTs, protoNamespace, "ok_id", "http://www.yandex.ru/ok.jpeg"),
          image
        )
        partition <- erasureQueueService.partition(event)
        _ <- runTx(erasureQueueService.addElement(event))
        avatarsService <- createAvatarsService(avatarsClient)
        _ <- getTask(avatarsService).run(partition)
        elements <- runTx(erasureQueueService.peekElements(now, partition, 100))
        _ <- check(elements shouldBe empty)
      } yield ()
    }

    "not drop from queue if avatars failed" in ydbTest {

      val avatarsClient = new TestAvatarsClient {
        override def delete(coordinates: AvatarsCoordinates): IO[AvatarsError, String] =
          IO.fail(AvatarsError.ErrorResponse(500, "some avatars error"))
      }

      for {
        now <- clock.instant
        protoTs = now.toProtoTimestamp
        image = Image(groupId, "failed_name", testNamespace.toString)
        event = ImageErasureEvent(
          ImageEventHeader(protoTs, protoNamespace, "failed_id", "http://www.yandex.ru/failed.jpeg"),
          image
        )
        partition <- erasureQueueService.partition(event)
        _ <- runTx(erasureQueueService.addElement(event))
        avatarsService <- createAvatarsService(avatarsClient)
        _ <- getTask(avatarsService).run(partition).ignore
        elements <- runTx(erasureQueueService.peekElements(now, partition, 100))
        _ <- check(elements.map(_.payload) should contain theSameElementsAs Set(event))
      } yield ()
    }

    "drop from queue if already deleted" in ydbTest {

      val avatarsClient = new TestAvatarsClient {
        override def delete(coordinates: AvatarsCoordinates): IO[AvatarsError, String] =
          IO.fail(AvatarsError.ErrorResponse(404, "not_found"))
      }

      for {
        now <- clock.instant
        protoTs = now.toProtoTimestamp
        image = Image(groupId, "not_found_name", testNamespace.toString)
        event = ImageErasureEvent(
          ImageEventHeader(protoTs, protoNamespace, "not_found_id", "http://www.yandex.ru/not_found.jpeg"),
          image
        )
        partition <- erasureQueueService.partition(event)
        _ <- runTx(erasureQueueService.addElement(event))
        avatarsService <- createAvatarsService(avatarsClient)
        _ <- getTask(avatarsService).run(partition)
        elements <- runTx(erasureQueueService.peekElements(now, partition, 100))
        _ <- check(elements shouldBe empty)
      } yield ()

    }
  }

}
