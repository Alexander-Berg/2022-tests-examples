package ru.yandex.vos2.watching.mds

import java.nio.charset.StandardCharsets

import org.apache.commons.io.IOUtils
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.model.offer.ImageMetaUtils
import ru.yandex.realty.proto.unified.offer.images.{RealtyPhotoMeta, ThinCbirPredictions, ThinRepairQualityV1}

import scala.collection.JavaConverters._

class RawMetaParserSpec extends FlatSpec with Matchers {

  behavior of RawMetaParser.getClass.getName

  it should "correctly fill ThinCbirPredictionFields" in {
    val fields = ImageMetaUtils.ThinCbirPredictionFields

    fields.size shouldBe 20

    // simple field
    fields.contains("docs_with_plans") shouldBe true

    // aliased field
    fields.contains("_500px_0") shouldBe false
    fields.contains("500px_0") shouldBe true
  }

  it should "correctly fill ThinCbirPredictionVersions" in {
    val versions = RawMetaParser.ThinCbirPredictionVersions

    versions.size shouldBe 1

    // at the time of writing, this is the only known version
    versions("8").getNumber shouldBe 1
  }

  it should "parse meta without errors and correctly" in {
    val jsonString = resource.managed(getClass.getResourceAsStream("raw-meta-example.json")).acquireAndGet { stream =>
      IOUtils.toString(stream, StandardCharsets.UTF_8)
    }
    val meta = RawMetaParser.parseRawMeta(jsonString)

    // similarity vector
    meta.hasSimilarityVector shouldBe true
    meta.getSimilarityVector.getFeatureCount shouldBe 96

    // thick CBIR predictions
    meta.getCbirPredictionCount shouldBe 0

    // thin CBIR predictions
    meta.hasCbirPredictions shouldBe true
    // unaliased
    meta.getCbirPredictions.hasDocsWithPlans shouldBe true
    meta.getCbirPredictions.getDocsWithPlans.getScore shouldBe (0.0002552216174 +- 1e-13)
    meta.getCbirPredictions.getDocsWithPlans.getKnownVersion shouldBe ThinCbirPredictions.Version.EIGHT
    // aliased
    meta.getCbirPredictions.getAliased500Px0.getScore shouldBe (0.5267419815 +- 1e-10)

    // GlobalSemidupDescriptor64
    meta.getCvHash shouldBe "M920253A190FFDEA1"

    // thick NN predictions
    meta.getNnPredictionCount shouldBe 0

    // thin NN predictions
    meta.hasNnPredictions shouldBe true
    meta.getNnPredictions.getRealtyDocsWithPlans shouldBe 6

    // thick repair_quality_v1
    meta.hasRepairQualityV1 shouldBe false

    // thin repair_quality_v1
    meta.hasThinRepairQualityV1 shouldBe true
    meta.getThinRepairQualityV1.getEuro shouldBe (-0.0316572 +- 1e-7)
    meta.getThinRepairQualityV1.getScore shouldBe (-0.102618 +- 1e-6)
    meta.getThinRepairQualityV1.getKnownVersion shouldBe ThinRepairQualityV1.Version.PROD_V5_ENC_I2T_V7_200_IMG

    // thick repair_quality_by_image_v1
    meta.getRepairQualityByImageV1Count shouldBe 0

    // thin repair_quality_by_image_v1
    meta.hasThinRepairQualityByImageV1 shouldBe true
    meta.getThinRepairQualityByImageV1.getRealtyEuro shouldBe 1

    // original size
    meta.hasOriginalSize shouldBe true
    meta.getOriginalSize.getX shouldBe 679
    meta.getOriginalSize.getY shouldBe 960
  }

  it should "not do anything if asked to set an unsupported thin field" in {
    val builder = RealtyPhotoMeta.newBuilder()
    RawMetaParser.setThinCbirPrediction(builder.getCbirPredictionsBuilder, "fff", 1, "vvv")
    RawMetaParser.setThinNnPrediction(builder.getNnPredictionsBuilder, "fff", 1)
    RawMetaParser.setThinRepairPrediction(builder.getThinRepairQualityV1Builder, "fff", 1)
    RawMetaParser.setThinRepairByImagePrediction(builder.getThinRepairQualityByImageV1Builder, "fff", 1)

    val expected = RealtyPhotoMeta.newBuilder()
    expected.getCbirPredictionsBuilder
    expected.getNnPredictionsBuilder
    expected.getThinRepairQualityV1Builder
    expected.getThinRepairQualityByImageV1Builder

    builder.build() shouldBe expected.build()
  }
}
