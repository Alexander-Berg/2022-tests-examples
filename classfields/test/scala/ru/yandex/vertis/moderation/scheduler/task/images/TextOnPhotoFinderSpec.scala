package ru.yandex.vertis.moderation.scheduler.task.images

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.TextOnPhoto
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.photos.ImagesMetadataConverter
import ru.yandex.vertis.moderation.picapica.OcrResult
import ru.yandex.vertis.moderation.picapica.PicaService.ImageId
import ru.yandex.vertis.moderation.scheduler.task.images.TextOnPhotoFinderSpec._
import ru.yandex.vertis.moderation.scheduler.task.images.impl.TextOnPhotoFinderImpl
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema.Metadata
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema.Metadata.OcrResultEntry

import scala.collection.JavaConverters._

/**
  * @author mpoplavkov
  */
@RunWith(classOf[JUnitRunner])
class TextOnPhotoFinderSpec extends SpecBase {

  private val expectName = StringGen.next
  private val expectType = StringGen.next
  private val minConfidence = Gen.chooseNum(0.0, 1.0).next
  private val foundText = StringGen.next
  private val greaterConfidence = 1.0
  private val lowerConfidence = -1.0

  private def createFinder(stringMatcher: String => Boolean = _ => true): TextOnPhotoFinder =
    new TextOnPhotoFinderImpl(expectName, Set(expectType), minConfidence, stringMatcher)

  private val simpleFinder = createFinder()

  "TextOnPhotoFinder" should {
    "find text if everything matches" in {
      val source =
        generateWithOneResult(
          name = expectName,
          result =
            OcrResult.Value(
              confidence = greaterConfidence,
              text = foundText,
              `type` = expectType
            )
        )
      val expected = Seq(TextOnPhoto(expectType, foundText, greaterConfidence))
      simpleFinder(source) shouldBe expected
    }

    "not find anything if confidence is less than a lower bound" in {
      val source =
        generateWithOneResult(
          name = expectName,
          result =
            OcrResult.Value(
              confidence = lowerConfidence,
              text = foundText,
              `type` = expectType
            )
        )
      simpleFinder(source) shouldBe Seq.empty
    }

    "not find anything if name or type not matches" in {
      val i = Gen.chooseNum(0, 1).next
      val source =
        generateWithOneResult(
          name = if (i == 0) expectName else StringGen.next,
          result =
            OcrResult.Value(
              confidence = greaterConfidence,
              text = foundText,
              `type` = if (i == 1) expectType else StringGen.next
            )
        )
      simpleFinder(source) shouldBe Seq.empty
    }

    "find text on multiple OCR results" in {
      val foundText2 = StringGen.next
      val foundText3 = StringGen.next
      val foundText4 = StringGen.next

      val ocrResult1 =
        OcrResult(
          name = expectName,
          values =
            Seq(
              OcrResult.Value(
                confidence = greaterConfidence,
                text = foundText,
                `type` = expectType
              ),
              // should be filtered cause to wrong type
              OcrResult.Value(
                confidence = greaterConfidence,
                text = StringGen.next,
                `type` = StringGen.next
              ),
              OcrResult.Value(
                confidence = greaterConfidence,
                text = foundText2,
                `type` = expectType
              )
            )
        )

      val ocrResult2 =
        OcrResult(
          name = expectName,
          values =
            Seq(
              OcrResult.Value(
                confidence = greaterConfidence,
                text = foundText3,
                `type` = expectType
              ),
              // should be filtered cause to low confidence
              OcrResult.Value(
                confidence = lowerConfidence,
                text = StringGen.next,
                `type` = expectType
              ),
              OcrResult.Value(
                confidence = greaterConfidence,
                text = foundText4,
                `type` = expectType
              )
            )
        )

      val source = generateSource(Seq(ocrResult1, ocrResult2))
      val expected =
        Seq(
          TextOnPhoto(expectType, foundText, greaterConfidence),
          TextOnPhoto(expectType, foundText2, greaterConfidence),
          TextOnPhoto(expectType, foundText3, greaterConfidence),
          TextOnPhoto(expectType, foundText4, greaterConfidence)
        )
      simpleFinder(source) should contain theSameElementsAs expected
    }

    "not find anything if text doesn't match to the given predicate" in {
      val finder = createFinder(_ => false)
      val source =
        generateWithOneResult(
          name = expectName,
          result =
            OcrResult.Value(
              confidence = greaterConfidence,
              text = foundText,
              `type` = expectType
            )
        )
      finder(source) shouldBe Seq.empty
    }
  }
}

object TextOnPhotoFinderSpec {

  private def generateWithOneResult(name: String, result: OcrResult.Value): PhotoDecider.Source = {
    val ocrResult =
      OcrResult(
        name = name,
        values = Seq(result)
      )
    generateSource(Seq(ocrResult))
  }

  private def generateSource(ocrResults: Seq[OcrResult]): PhotoDecider.Source = {
    val imagesMeta =
      ocrResults.map { result =>
        val imageName = StringGen.next
        val imageSrcUrl = StringGen.next

        val meta =
          Metadata.newBuilder
            .setVersion(1)
            .setIsFinished(true)
            .addOcrResults(ocrResultsToProto(result))
            .build

        ImageId(imageName, imageSrcUrl) -> meta

      }.toMap

    ImagesMetadataConverter.convertPicaImagesMetadataToSource(InstanceGen.next, imagesMeta)
  }

  private def ocrResultsToProto(ocrResult: OcrResult): OcrResultEntry = {
    val values =
      ocrResult.values.map { case OcrResult.Value(confidence, text, ocrType) =>
        Metadata.OcrResult.newBuilder
          .setType(ocrType)
          .setText(text)
          .setConfidence(confidence.toFloat)
          .build
      }

    Metadata.OcrResultEntry.newBuilder
      .setName(ocrResult.name)
      .addAllValues(values.asJava)
      .build

  }

}
