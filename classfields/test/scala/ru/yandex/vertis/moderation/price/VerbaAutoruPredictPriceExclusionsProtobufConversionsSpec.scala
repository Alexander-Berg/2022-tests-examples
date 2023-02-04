package ru.yandex.vertis.moderation.price

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.extdatacore.Extdata.VerbaAutoruPredictPriceExclusions
import ru.yandex.vertis.moderation.extdatacore.model.verba.price.AutoruPredictPriceExclusion
import ru.yandex.vertis.moderation.price.impl.verba.VerbaAutoruPredictPriceExclusionsProtobufConversions
import ru.yandex.vertis.protobuf.ProtobufUtils

/**
  * Spec for [[VerbaAutoruPredictPriceExclusionsProtobufConversions]]
  */
@RunWith(classOf[JUnitRunner])
class VerbaAutoruPredictPriceExclusionsProtobufConversionsSpec extends SpecBase {

  "VerbaAutoruPredictPriceExclusionsProtobufConversions" should {

    "parse test data correctly" in {
      val expectedResult =
        Set(
          AutoruPredictPriceExclusion("CHEVROLET", Some("LANOS"), None),
          AutoruPredictPriceExclusion("DW_HOWER", None, None),
          AutoruPredictPriceExclusion("HYUNDAI", Some("SONATA"), Some("3483580")),
          AutoruPredictPriceExclusion("HYUNDAI", Some("SONATA"), Some("2306936"))
        )
      val json = readResource("/price/autoru_predict_price_exclusions.json")
      val proto = ProtobufUtils.fromJson(VerbaAutoruPredictPriceExclusions.getDefaultInstance, json)
      val actualResult = VerbaAutoruPredictPriceExclusionsProtobufConversions.fromMessage(proto)

      actualResult shouldBe expectedResult
    }
  }
}
