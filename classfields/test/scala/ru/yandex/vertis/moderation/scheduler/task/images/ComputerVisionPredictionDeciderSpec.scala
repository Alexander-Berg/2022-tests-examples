package ru.yandex.vertis.moderation.scheduler.task.images

import ru.yandex.extdata.core.gens.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.DetailedReason
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{InstanceGen, StringGen}
import ru.yandex.vertis.moderation.model.photo.{ImageMetadata, Prediction}
import ru.yandex.vertis.moderation.scheduler.task.images.PhotoDecider.Source

class ComputerVisionPredictionDeciderSpec extends SpecBase {

  private val label1 = "trigger1"
  private val label2 = "trigger2"
  private val reason = DetailedReason.Another

  private val decider: ComputerVisionPredictionDecider =
    new ComputerVisionPredictionDecider(
      labels = Seq(label1, label2),
      minConfidence = 0.3,
      maxConfidence = 0.8,
      warnReason = reason
    )

  "ComputerVisionPredictionDecider" should {
    "return ok verdict on empty metadata" in {
      val source = photoSource()
      decider.apply(source) shouldBe PhotoDecider.Verdict.Ok
    }

    "return ok verdict on prediction with another labels" in {
      val source = photoSource("dont_care" -> 0.5, "dont_care2" -> 0.5)
      decider.apply(source) shouldBe PhotoDecider.Verdict.Ok
    }

    "return ok verdict if probability is outside specified minimum" in {
      val source = photoSource(label1 -> 0.2, label2 -> 0.9)
      decider.apply(source) shouldBe PhotoDecider.Verdict.Ok
    }

    "return verdict if probability is within the range for one label" in {
      val source = photoSource(label1 -> 0.5, label2 -> 0.9)
      val warns = decider.apply(source).warns
      warns.size shouldBe 1
      warns.head.reason shouldBe reason
      warns.head.info.get should include(s"label:$label1,confidence:0.5")
    }

    "return verdict if probability is within the range for another label" in {
      val source = photoSource(label1 -> 0.2, label2 -> 0.5)
      val warns = decider.apply(source).warns
      warns.size shouldBe 1
      warns.head.reason shouldBe reason
      warns.head.info.get should include(s"label:$label2,confidence:0.5")
    }

    "return verdict if probability is within the range for all of the labels" in {
      val source = photoSource(label1 -> 0.5, label2 -> 0.6)
      val warn1 :: warn2 :: Nil = decider.apply(source).warns.toList

      warn1.reason shouldBe reason
      warn1.info.get should include(s"label:$label1,confidence:0.5")
      warn2.reason shouldBe reason
      warn2.info.get should include(s"label:$label2,confidence:0.6")
    }
  }

  private def photoSource(labelToProbability: (String, Double)*): PhotoDecider.Source = {
    val ps =
      labelToProbability
        .map { case (l, p) =>
          Prediction(labelName = l, probability = p, version = "?")
        }
    genPhotoSource(ps)
  }

  private def genPhotoSource(predictions: Seq[Prediction]): PhotoDecider.Source = {
    val meta = ImageMetadata(None, Seq.empty, Seq.empty, predictions)
    val imagesMeta = Map(StringGen.next -> meta)
    Source(InstanceGen.next, imagesMeta)
  }
}
