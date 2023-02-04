package ru.yandex.realty.api.service.search.extender

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase

@RunWith(classOf[JUnitRunner])
class PriceFilterExtenderSpec extends SpecBase {
  val priceFilterExtender = new PriceFilterExtender

  "PriceFilterExtenderSpec " should {
    "don't change anything in unrelated params" in {
      val unrelatedParams = Map(
        "type" -> List("SELL"),
        "category" -> List("APARTMENT"),
        "leftLongitude" -> List("30.125786"),
        "topLatitude" -> List("60.111073"),
        "rightLongitude" -> List("30.939378"),
        "bottomLatitude" -> List("59.87927")
      )
      val mutableUnrelatedParams = collection.mutable.Map(unrelatedParams.toSeq: _*)
      priceFilterExtender.checkAndExtendToNewParams(unrelatedParams) should be(None)
      priceFilterExtender.extendToCurrent(mutableUnrelatedParams) should be(mutableUnrelatedParams)
    }

    "change priceMin if only priceMin is defined" in {
      val priceMinParams = Map(
        "type" -> List("SELL"),
        "category" -> List("APARTMENT"),
        "leftLongitude" -> List("30.125786"),
        "topLatitude" -> List("60.111073"),
        "rightLongitude" -> List("30.939378"),
        "priceMin" -> List("1000000"),
        "bottomLatitude" -> List("59.87927")
      )

      val targetPriceMinParams = Map(
        "type" -> List("SELL"),
        "category" -> List("APARTMENT"),
        "leftLongitude" -> List("30.125786"),
        "topLatitude" -> List("60.111073"),
        "rightLongitude" -> List("30.939378"),
        "priceMin" -> List("900000"),
        "bottomLatitude" -> List("59.87927")
      )
      val mutablePriceMinParams = collection.mutable.Map(priceMinParams.toSeq: _*)
      val mutableTargetPriceMinParams = collection.mutable.Map(targetPriceMinParams.toSeq: _*)

      priceFilterExtender.checkAndExtendToNewParams(priceMinParams) should be(Some(targetPriceMinParams))
      priceFilterExtender.extendToCurrent(mutablePriceMinParams) should be(mutableTargetPriceMinParams)
    }

    "change all params in " in {
      val params = Map(
        "type" -> List("SELL"),
        "category" -> List("APARTMENT"),
        "priceMin" -> List("1000000"),
        "priceMax" -> List("1000000"),
        "leftLongitude" -> List("30.125786"),
        "topLatitude" -> List("60.111073"),
        "rightLongitude" -> List("30.939378"),
        "bottomLatitude" -> List("59.87927")
      )

      val targetParams = Map(
        "type" -> List("SELL"),
        "category" -> List("APARTMENT"),
        "priceMin" -> List("900000"),
        "priceMax" -> List("1100000"),
        "leftLongitude" -> List("30.125786"),
        "topLatitude" -> List("60.111073"),
        "rightLongitude" -> List("30.939378"),
        "bottomLatitude" -> List("59.87927")
      )
      val mutablePriceMinParams = collection.mutable.Map(params.toSeq: _*)
      val mutableTargetPriceMinParams = collection.mutable.Map(targetParams.toSeq: _*)

      priceFilterExtender.checkAndExtendToNewParams(params) should be(Some(targetParams))
      priceFilterExtender.extendToCurrent(mutablePriceMinParams) should be(mutableTargetPriceMinParams)
    }
  }
}
