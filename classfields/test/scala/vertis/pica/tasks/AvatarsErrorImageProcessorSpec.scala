package vertis.pica.tasks

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.ops.test.TestOperationalSupport
import vertis.pica.Gens.ImageRecordGen
import vertis.pica.model.{ImageRecord, ProcessingStatuses}
import vertis.pica.service.avatars.AvatarsException
import vertis.pica.tasks.ImageTaskProcessor.ProcessingException
import vertis.zio.test.ZioSpecBase
import zio.{IO, UIO}

/** @author ruslansd
  */
class AvatarsErrorImageProcessorSpec extends ZioSpecBase with ScalaCheckPropertyChecks with TestOperationalSupport {

  private def registry = prometheusRegistry

  private def avatarsErrorProcessor(effect: ImageRecord => IO[ProcessingException, ImageRecord]) =
    new StaticImageProcessor(effect) with AvatarsErrorImageProcessor {
      override def policy: ReschedulingPolicy = ReschedulingPolicy.Default

      override protected def qualifier: String = "o-yandex"

      override def prometheusRegistry: PrometheusRegistry = registry
    }

  "AvatarsErrorImageProcessor" should {
    "do nothing on success" in {
      val processor = avatarsErrorProcessor(record => UIO(record))
      forAll(ImageRecordGen) { record =>
        ioTest {
          for {
            processed <- processor.process(record)
            _ = processed shouldBe record
          } yield ()
        }
      }
    }

    "reschedule on avatars retryable exception" in {
      val exception = ImageTaskProcessor.toProcessingException(new AvatarsException(434, "artificial"))
      val processor = avatarsErrorProcessor(_ => IO.fail(exception))
      forAll(ImageRecordGen) { record =>
        ioTest {
          for {
            rescheduled <- processor.process(record)
            _ = rescheduled.checkTs.getEpochSecond should be > record.checkTs.getEpochSecond
            _ = rescheduled.errors.nonEmpty shouldBe true
            _ = rescheduled.copy(errors = record.errors, checkTs = record.checkTs) shouldBe record
          } yield ()
        }
      }
    }

    "fail on avatars non retryable exception" in {
      val exception =
        ImageTaskProcessor.toProcessingException(new AvatarsException(404, "not found"))
      val processor = avatarsErrorProcessor(_ => IO.fail(exception))
      forAll(ImageRecordGen) { record =>
        ioTest {
          for {
            rescheduled <- processor.process(record)
            _ = rescheduled.errors.nonEmpty shouldBe true
            _ = rescheduled.status shouldBe ProcessingStatuses.Failed
          } yield ()
        }
      }
    }

    "fail on non avatars exception" in {
      val exception =
        ImageTaskProcessor.toProcessingException(new RuntimeException("artificial"))
      val processor = avatarsErrorProcessor(_ => IO.fail(exception))
      forAll(ImageRecordGen) { record =>
        intercept[exception.type] {
          ioTest {
            processor.process(record)
          }
        }
      }
    }

  }
}
