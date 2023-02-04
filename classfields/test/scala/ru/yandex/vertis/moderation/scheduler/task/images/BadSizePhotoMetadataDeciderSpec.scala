package ru.yandex.vertis.moderation.scheduler.task.images

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.photos.ImagesMetadataConverter
import ru.yandex.vertis.moderation.picapica.PicaService.ImageId
import ru.yandex.vertis.moderation.scheduler.task.images.BadSizePhotoMetadataDeciderSpec._
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema.Metadata

/**
  * @author mpoplavkov
  */
@RunWith(classOf[JUnitRunner])
class BadSizePhotoMetadataDeciderSpec extends SpecBase {

  private val minPixelSize = IntGen.next
  private val minCount = Gen.chooseNum(1, 5).next

  private val decider = new BadSizePhotoMetadataDecider(minPixelSize, minCount)

  "BadSizePhotoMetadataDecider" should {

    "pass a bad verdict if there are too many bad photos" in {
      val photosCount = Gen.chooseNum(minCount, minCount * 2).next
      val badPhotosCount = Gen.chooseNum(minCount, photosCount).next
      val source = generateSource(photosCount, badPhotosCount, minPixelSize)
      decider.apply(source).warns should not be 'empty
    }

    "pass a good verdict if there are less than `minCount` bad photos" in {
      val photosCount = Gen.chooseNum(minCount, minCount * 2).next
      val badPhotosCount = Gen.chooseNum(0, minCount - 1).next
      val source = generateSource(photosCount, badPhotosCount, minPixelSize)
      decider.apply(source) shouldBe PhotoDecider.Verdict.Ok
    }
  }
}

object BadSizePhotoMetadataDeciderSpec {

  def generateSource(photosCount: Int, badPhotosCount: Int, minPixelSize: Int): PhotoDecider.Source = {

    val isGoodPhotoSeq = Seq.fill(badPhotosCount)(false) ++ Seq.fill(photosCount - badPhotosCount)(true)
    val meta =
      isGoodPhotoSeq.map { isGood =>
        val imageName = StringGen.next
        val imageSrcUrl = StringGen.next

        val meta =
          Metadata.newBuilder
            .setVersion(1)
            .setIsFinished(true)
            .setOrigSize(generateOrigSize(isGood, minPixelSize))
            .build

        ImageId(imageName, imageSrcUrl) -> meta
      }.toMap

    ImagesMetadataConverter.convertPicaImagesMetadataToSource(InstanceGen.next, meta)
  }

  def generateOrigSize(isGood: Boolean, minPixelSize: Int): Metadata.Size = {
    val goodIntGen = Gen.chooseNum(minPixelSize, Int.MaxValue)
    val badIntGen = Gen.chooseNum(0, minPixelSize - 1)
    val randIntGen = Gen.chooseNum(0, minPixelSize * 2)

    val (x, y) =
      if (isGood) {
        (goodIntGen.next, goodIntGen.next)
      } else {
        val gen =
          for {
            x <- randIntGen
            y <- if (x < minPixelSize) randIntGen else badIntGen
          } yield (x, y)
        gen.next
      }

    Metadata.Size.newBuilder
      .setX(x)
      .setY(y)
      .build
  }

}
