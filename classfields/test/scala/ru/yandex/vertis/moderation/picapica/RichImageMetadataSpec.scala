package ru.yandex.vertis.moderation.picapica

import java.io.ByteArrayOutputStream

import com.datastax.oss.protocol.internal.util.Bytes
import org.apache.commons.io.IOUtils
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.photo
import ru.yandex.vertis.moderation.model.photo.ImageMetadata.RichImageMetadata
import ru.yandex.vertis.moderation.model.photo.{ImageMetadata, OrigSize}
import ru.yandex.vertis.moderation.photos.ImagesMetadataConverter
import ru.yandex.vertis.moderation.picapica.RichImageMetadataSpec.{getMeta, getMetaFromBase64}
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema.Metadata

/**
  * Specs for [[RichImageMetadata]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class RichImageMetadataSpec extends SpecBase {

  implicit def convert: Metadata => ImageMetadata = ImagesMetadataConverter.protoPicaMetaToImageMetadata

  "RichMetadata" should {
    "return correct sizes" in {
      getMeta("1060211456-a31b4_1.bin").origSize should be(Some(OrigSize(886, 916)))
      getMeta("1061304058-a17264_1.bin").origSize should be(Some(OrigSize(1200, 900)))
      getMeta("721440493865909761.bin").origSize should be(Some(OrigSize(1200, 900)))
      getMeta("9069199666259274999.bin").origSize should be(Some(OrigSize(960, 1280)))
      getMeta("4729787348356411905.bin").origSize should be(Some(OrigSize(3264, 2448)))
      getMeta("6300461494541429459.bin").origSize should be(Some(OrigSize(960, 1280)))
      getMeta("6114158062166522625.bin").origSize should be(Some(OrigSize(99, 99)))

      getMetaFromBase64("offer.9069199666259274999.txt").origSize should be(Some(OrigSize(960, 1280)))
    }
    "return cv data if has one" in {
      getMeta("1060211456-a31b4_1.bin").ocrResults should be(empty)
      getMeta("1061304058-a17264_1.bin").ocrResults.toList match {
        case photo.OcrResult("auto_contacts", values) :: Nil if values.size == 1 =>
          values.head.confidence should be(0.643299f)
          values.head.text should be("+7-968-041-33-71")
          values.head.ocrType should be("phone")
        case other => fail(s"Unexpected $other")
      }
    }
    "return something by MaxWeightTopClassId" in {
      convert(getMeta("1060211456-a31b4_1.bin"))
        .getMaxWeightTopClassId("autoru_v1_cost_2057") should be(Some(36))
      convert(getMeta("1060211456-a31b4_1.bin"))
        .getMaxWeightTopClassId("autoru_v1_views_cost") should be(Some(10))
      convert(getMeta("1060211456-a31b4_1.bin"))
        .getMaxWeightTopClassId("autoru_v1_views_cost111") should be(empty)
    }
  }
}

object RichImageMetadataSpec {

  def getMeta(resource: String): Metadata = {
    val is = this.getClass.getClassLoader.getResourceAsStream(resource)
    val m = Metadata.parseFrom(is)
    is.close()
    m
  }

  def getMetaFromBase64(resource: String): Metadata = {
    val is = this.getClass.getClassLoader.getResourceAsStream(resource)
    val os = new ByteArrayOutputStream()
    IOUtils.copy(is, os)
    val m = Metadata.parseFrom(Bytes.fromHexString(new String(os.toByteArray)).array())
    is.close()
    m
  }
}
