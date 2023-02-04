package ru.yandex.vertis.moderation.photos

import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.InstanceGen
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.mds.MdsImageMetadata
import ru.yandex.vertis.moderation.model.photo.Prediction

class ImagesMetadataConverterSpec extends SpecBase {
  "ImagesMetadataConverter.convertMdsImagesMetadataToSource" should {
    "retain predictions" in {
      val predictions =
        Seq(
          Prediction(labelName = "label1", probability = 0.5, version = "10"),
          Prediction(labelName = "label2", probability = 0.3, version = "9")
        )
      val mdsImagesMeta =
        Map(
          "image1" -> MdsImageMetadata(
            ocrNnLite = None,
            origSize = None,
            predictions = Some(predictions)
          )
        )

      val source = ImagesMetadataConverter.convertMdsImagesMetadataToSource(InstanceGen.next, mdsImagesMeta)
      source.imagesMeta("image1").classifierPredictions shouldBe predictions
    }
  }
}
