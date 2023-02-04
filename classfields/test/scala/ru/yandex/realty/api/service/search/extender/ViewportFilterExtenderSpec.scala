package ru.yandex.realty.api.service.search.extender

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase

@RunWith(classOf[JUnitRunner])
class ViewportFilterExtenderSpec extends SpecBase {
  val viewportFilterExtender = new ViewportFilterExtender
  "ViewportFilterExtenderSpec " should {
    "don't change anything in unrelated params" in {
      val unrelatedParams = Map(
        "type" -> List("SELL"),
        "category" -> List("APARTMENT"),
        "priceMin" -> List("1000000"),
        "priceMax" -> List("1000000")
      )
      val mutableUnrelatedParams = collection.mutable.Map(unrelatedParams.toSeq: _*)
      viewportFilterExtender.checkAndExtendToNewParams(unrelatedParams) should be(None)
      viewportFilterExtender.extendToCurrent(mutableUnrelatedParams) should be(mutableUnrelatedParams)
    }
    "don't change anything if we don't take whole group related params" in {
      val incompleteParams = Map(
        "type" -> List("SELL"),
        "category" -> List("APARTMENT"),
        "leftLongitude" -> List("30.125786"),
        "topLatitude" -> List("60.111073"),
        "bottomLatitude" -> List("59.87927")
      )
      val mutableIncompleteParams = collection.mutable.Map(incompleteParams.toSeq: _*)
      viewportFilterExtender.checkAndExtendToNewParams(incompleteParams) should be(None)
      viewportFilterExtender.extendToCurrent(mutableIncompleteParams) should be(mutableIncompleteParams)
    }

    "change all params " in {
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
        "priceMin" -> List("1000000"),
        "priceMax" -> List("1000000"),
        "leftLongitude" -> List("30.044426"),
        "topLatitude" -> List("60.134254"),
        "rightLongitude" -> List("31.020739"),
        "bottomLatitude" -> List("59.856087")
      )
      val mutablePriceMinParams = collection.mutable.Map(params.toSeq: _*)
      val mutableTargetPriceMinParams = collection.mutable.Map(targetParams.toSeq: _*)

      viewportFilterExtender.checkAndExtendToNewParams(params) should be(Some(targetParams))
      viewportFilterExtender.extendToCurrent(mutablePriceMinParams) should be(mutableTargetPriceMinParams)
    }

  }

}
