package ru.yandex.vertis.moderation.scheduler.task.images.impl

import org.mockito.Mockito.when
import org.scalacheck.Gen
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.photo.{ImageMetadata, Prediction}
import ru.yandex.vertis.moderation.scheduler.task.images.PhotoDecider
import ru.yandex.vertis.moderation.scheduler.task.images.ScreenshotFinder.ScreenshotPhoto

class ScreenshotFinderImplSpec extends SpecBase {

  private val minimalConfidence = 0.5f
  private val screenshotFinder = new ScreenshotFinderImpl(minimalConfidence)

  private val source = mock[PhotoDecider.Source]
  private val emptyImageMetadata = ImageMetadata(None, Seq.empty, Seq.empty, Seq.empty)

  "ScreenshotFinderImpl" should {
    "return empty result for empty metadata map" in {
      when(source.imagesMeta).thenReturn(Map.empty[String, ImageMetadata])

      screenshotFinder.apply(source) shouldBe Seq.empty
    }

    "return empty result for image without meta" in {
      when(source.imagesMeta).thenReturn(Map("image_id" -> emptyImageMetadata))

      screenshotFinder.apply(source) shouldBe Seq.empty
    }

    "check label and filter by minimal confidence" in {
      val metadata = createImageMetadataWithScreenshotPrediction(0.2)
      val metadata1 = createImageMetadataWithScreenshotPrediction(0.5)
      val metadata2 = createImageMetadataWithScreenshotPrediction(0.9)
      val metadata3 = createImageMetadataWithScreenshotPrediction(0.4)

      val imagesMetadata =
        Map(
          "image_id0" -> metadata,
          "image_id1" -> emptyImageMetadata,
          "image_id2" -> metadata1,
          "image_id3" -> metadata2,
          "image_id4" -> emptyImageMetadata,
          "image_id5" -> metadata3
        )

      when(source.imagesMeta).thenReturn(imagesMetadata)

      val expectedResult =
        Seq(
          ScreenshotPhoto("image_id2", 0.5),
          ScreenshotPhoto("image_id3", 0.9)
        )

      screenshotFinder.apply(source) should contain theSameElementsAs expectedResult
    }
  }

  private def createImageMetadataWithScreenshotPrediction(confidence: Double) = {
    val predictionGen: Gen[Prediction] =
      for {
        label      <- Gen.alphaStr
        confidence <- Gen.chooseNum(0.0, 1.0)
      } yield Prediction(label, confidence, "")

    val predictionsGen: Gen[Seq[Prediction]] =
      for {
        numPredictions <- Gen.choose(0, 6)
        predictions    <- Gen.listOfN(numPredictions, predictionGen)
      } yield predictions

    ImageMetadata(
      None,
      Seq.empty,
      Seq.empty,
      Seq(Prediction(ScreenshotFinderImpl.ScreenshotLabel, confidence, "")) ++ predictionsGen.next
    )
  }
}
