package vertis.pica.tasks

import ru.yandex.vertis.pica.model.model.Image
import vertis.pica.Gens.{asProducer, imageGen, ImageRequestGen}
import vertis.pica.dao.PicaYdbSpecBase
import vertis.pica.model.{ImagePutRequest, ProcessingStatuses}
import vertis.pica.service.{ImageService, TestBrokerQueueService}
import vertis.pica.service.avatars.{AvatarsService, TestAvatarsService}
import vertis.zio.BTask
import vertis.zio.test.ZioSpecBase
import zio._

import scala.concurrent.duration.Duration

/** @author ruslansd
  */
class ProcessingImageTaskIntSpec extends ZioSpecBase with PicaYdbSpecBase {

  private def getTask(image: Image) = {

    val avatarsMock = new TestAvatarsService {

      override def put(url: String, imageName: String, expire: Option[Duration]): BTask[Image] =
        Task.succeed(image)
    }

    val brokerQueueServiceMock = TestBrokerQueueService(namespaceConf.namespace)

    lazy val imageService = new ImageService(storage, brokerQueueServiceMock)

    val downloader = new DownloadTaskProcessor {
      override def avatars: AvatarsService = avatarsMock
    }

    val metaLoader = new LoadMetaTaskProcessor {
      override def avatars: AvatarsService = avatarsMock
    }

    new ProcessingImageTask(
      imageService,
      downloader,
      metaLoader
    )
  }

  "ProcessingImageTask" should {

    "process queued image with finished meta" in ydbTest {
      val request = ImageRequestGen.next
      val image = imageGen().next
      for {
        processed <- putRunGet(request)(getTask(image))
        _ <- check("record should be processed")(processed.status shouldBe ProcessingStatuses.Processed)
      } yield ()
    }

    "process queued image with not finished meta" in ydbTest {
      val request = ImageRequestGen.next
      val image = imageGen(false).next
      for {
        processed <- putRunGet(request)(getTask(image))
        _ <- check("record should be processed")(processed.status shouldBe ProcessingStatuses.WaitingMeta)
      } yield ()
    }
  }

  private def putRunGet(request: ImagePutRequest)(task: ProcessingImageTask) =
    for {
      _ <- runTx(storage.put(request))
      _ <- task.run(getPartition(request.imageId.url))
      processed <- runTx(storage.get(request.imageId))
      _ <- check("processed record should exists")(processed.isDefined shouldBe true)
    } yield processed.get
}
