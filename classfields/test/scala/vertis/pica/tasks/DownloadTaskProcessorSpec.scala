package vertis.pica.tasks

import ru.yandex.vertis.pica.model.model.Image
import vertis.zio.test.ZioSpecBase
import vertis.pica.Gens.QueuedImageRecordGen
import vertis.pica.Gens.imageGen
import vertis.pica.Gens.asProducer
import vertis.pica.model.ProcessingStatuses
import vertis.pica.service.avatars.{AvatarsService, TestAvatarsService}
import vertis.zio.BTask
import zio.Task

import scala.concurrent.duration.Duration

/** @author ruslansd
  */
class DownloadTaskProcessorSpec extends ZioSpecBase {

  private def getProcessor(image: Image) = {
    val avatarsMock = new TestAvatarsService {

      override def put(url: String, imageName: String, expire: Option[Duration]): BTask[Image] =
        Task.succeed(image)
    }

    new DownloadTaskProcessor {
      override def avatars: AvatarsService = avatarsMock
    }
  }

  "DownloadTaskProcessor" should {
    "process image record with finished meta" in ioTest {
      val image = imageGen().next
      val record = QueuedImageRecordGen.next
      for {
        resultRecord <- getProcessor(image).process(record)
        _ = resultRecord.status shouldBe ProcessingStatuses.Processed
        _ = resultRecord.image shouldBe Some(image)
      } yield ()
    }

    "process image record with not finished meta" in ioTest {
      val image = imageGen(false).next
      val record = QueuedImageRecordGen.next
      for {
        resultRecord <- getProcessor(image).process(record)
        _ = resultRecord.status shouldBe ProcessingStatuses.WaitingMeta
        _ = resultRecord.image shouldBe Some(image)
      } yield ()
    }
  }
}
