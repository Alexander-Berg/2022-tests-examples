package vertis.pica.tasks

import common.clients.avatars.model.{AvatarsCoordinates, ImageMeta}
import vertis.pica.Gens.{asProducer, metaGen, WaitingForCaptureImageRecordGen}
import vertis.pica.model.ProcessingStatuses
import vertis.pica.service.avatars.{AvatarsService, TestAvatarsService}
import vertis.pica.tasks.ImageTaskProcessor.ProcessingException
import vertis.zio.BTask
import vertis.zio.test.ZioSpecBase
import zio.Task

/** @author ruslansd
  */
class LoadMetaTaskProcessorSpec extends ZioSpecBase {

  private def getProcessor(meta: ImageMeta) = {
    val avatarsMock = new TestAvatarsService {
      override def getMeta(coordinates: AvatarsCoordinates): BTask[ImageMeta] =
        Task.succeed(meta)
    }

    new LoadMetaTaskProcessor {
      override def avatars: AvatarsService = avatarsMock
    }
  }

  "LoadMetaTaskProcessor" should {
    "pull meta" in ioTest {
      val record = WaitingForCaptureImageRecordGen.next
      val meta = metaGen().next
      val expectedImage = record.image.map(_.copy(meta = meta.content, metaFinished = true))
      for {
        resultRecord <- getProcessor(meta).process(record)
        _ = resultRecord.status shouldBe ProcessingStatuses.Processed
        _ = resultRecord.image shouldBe expectedImage
      } yield ()
    }

    "process image record with not finished meta" in {
      intercept[ProcessingException] {
        ioTest {
          val record = WaitingForCaptureImageRecordGen.next
          val meta = metaGen(false).next
          getProcessor(meta).process(record)
        }
      }
    }
  }
}
