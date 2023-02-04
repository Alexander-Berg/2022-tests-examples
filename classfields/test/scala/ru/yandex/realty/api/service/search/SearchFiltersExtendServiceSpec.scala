package ru.yandex.realty.api.service.search

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.api.service.search.extender.{PriceFilterExtender, ViewportFilterExtender}

import scala.util.Try

@RunWith(classOf[JUnitRunner])
class SearchFiltersExtendServiceSpec extends SpecBase {

  private val paramsWithoutExtends = Map("type" -> List("SELL"), "category" -> List("APARTMENT"))

  private val paramsWithPriceMin =
    Map("type" -> List("SELL"), "category" -> List("APARTMENT"), "priceMin" -> List("1000000"))

  private val extendedParamsWithPriceMin =
    Map("type" -> List("SELL"), "category" -> List("APARTMENT"), "priceMin" -> List("900000"))

  private val paramsWithAllExtendableFilters =
    Map(
      "type" -> List("SELL"),
      "category" -> List("APARTMENT"),
      "priceMin" -> List("1000000"),
      "priceMax" -> List("1000000"),
      "leftLongitude" -> List("30.125786"),
      "topLatitude" -> List("60.111073"),
      "rightLongitude" -> List("30.939378"),
      "bottomLatitude" -> List("59.87927")
    )
  "SearchFiltersExtendService in extendAllFilters" should {
    "return the same params if nothing should be extended" in {
      SearchFiltersExtendService.extendAllFilters(paramsWithoutExtends) should be(paramsWithoutExtends)
    }

    "return only priceMin if only this param is defined" in {
      SearchFiltersExtendService.extendAllFilters(paramsWithPriceMin) should be(extendedParamsWithPriceMin)
    }

    "return all extended filter if this defined" in {
      val paramsWithAllExtendedFilters =
        Map(
          "type" -> List("SELL"),
          "category" -> List("APARTMENT"),
          "priceMin" -> List("900000"),
          "priceMax" -> List("1100000"),
          "leftLongitude" -> List("30.044426"),
          "topLatitude" -> List("60.134254"),
          "rightLongitude" -> List("31.020739"),
          "bottomLatitude" -> List("59.856087")
        )
      SearchFiltersExtendService.extendAllFilters(paramsWithAllExtendableFilters) should be(
        paramsWithAllExtendedFilters
      )
    }
  }

  "SearchFiltersExtendService in multiplyParamsByExtendAllFilters" should {
    "return empty result if nothing to extend" in {
      SearchFiltersExtendService.multiplyParamsByExtendAllFilters(paramsWithoutExtends) should be(Seq.empty)
    }
    "return only priceMin extender if only this param defined" in {
      val priceMinExtendedFilter =
        List(
          ExtendedParams(
            extendedParamsWithPriceMin,
            Seq(ChangedField(PriceFilterExtender.priceMinKey, Some(List("1000000")), Some(List("900000")))),
            PriceFilterExtender.filterGroupKey
          )
        )
      SearchFiltersExtendService.multiplyParamsByExtendAllFilters(paramsWithPriceMin) should be(priceMinExtendedFilter)
    }

    "throw exception, if extendable filter is not defined" in {
      val failed = Try(SearchFiltersExtendService.multiplyParamsByExtendFilters(paramsWithPriceMin, Set.empty))
      failed.isFailure shouldBe true
    }

    "return only priceMin extender if only priceMin is need to extend" in {
      import ru.yandex.realty.controllers.search.SearchParametersSupport._

      val specialParams = Map(
        "type" -> List("SELL"),
        "category" -> List("APARTMENT"),
        "priceMin" -> List("1000000"),
        "leftLongitude" -> List("30.125786"),
        "topLatitude" -> List("60.111073"),
        "rightLongitude" -> List("30.939378"),
        "bottomLatitude" -> List("59.87927"),
        "extendableGroupKey" -> List("price")
      )

      val specialExtendedParamsWithPriceMin =
        Map(
          "type" -> List("SELL"),
          "category" -> List("APARTMENT"),
          "priceMin" -> List("900000"),
          "leftLongitude" -> List("30.125786"),
          "topLatitude" -> List("60.111073"),
          "rightLongitude" -> List("30.939378"),
          "bottomLatitude" -> List("59.87927"),
          "extendableGroupKey" -> List("price")
        )
      val priceMinExtendedFilter =
        List(
          ExtendedParams(
            specialExtendedParamsWithPriceMin,
            Seq(ChangedField(PriceFilterExtender.priceMinKey, Some(List("1000000")), Some(List("900000")))),
            PriceFilterExtender.filterGroupKey
          )
        )
      val extendable = getExtendableFilters(specialParams)
      SearchFiltersExtendService.multiplyParamsByExtendFilters(specialParams, extendable) should be(
        priceMinExtendedFilter
      )
    }

    "return all extended params " in {
      val extendedPriceParams = Map(
        "type" -> List("SELL"),
        "category" -> List("APARTMENT"),
        "priceMin" -> List("900000"),
        "priceMax" -> List("1100000"),
        "leftLongitude" -> List("30.125786"),
        "topLatitude" -> List("60.111073"),
        "rightLongitude" -> List("30.939378"),
        "bottomLatitude" -> List("59.87927")
      )
      val extendedViewportParams = Map(
        "type" -> List("SELL"),
        "category" -> List("APARTMENT"),
        "priceMin" -> List("1000000"),
        "priceMax" -> List("1000000"),
        "leftLongitude" -> List("30.044426"),
        "topLatitude" -> List("60.134254"),
        "rightLongitude" -> List("31.020739"),
        "bottomLatitude" -> List("59.856087")
      )
      val allFieldMultipliedFilters =
        List(
          ExtendedParams(
            extendedPriceParams,
            Seq(
              ChangedField(PriceFilterExtender.priceMinKey, Some(List("1000000")), Some(List("900000"))),
              ChangedField(PriceFilterExtender.priceMaxKey, Some(List("1000000")), Some(List("1100000")))
            ),
            PriceFilterExtender.filterGroupKey
          ),
          ExtendedParams(
            extendedViewportParams,
            Seq(
              ChangedField(ViewportFilterExtender.bottomLatitudeKey, Some(List("59.87927")), Some(List("59.856087"))),
              ChangedField(ViewportFilterExtender.topLatitudeKey, Some(List("60.111073")), Some(List("60.134254"))),
              ChangedField(ViewportFilterExtender.rightLongitudeKey, Some(List("30.939378")), Some(List("31.020739"))),
              ChangedField(ViewportFilterExtender.leftLongitudeKey, Some(List("30.125786")), Some(List("30.044426")))
            ),
            ViewportFilterExtender.filterGroupKey
          )
        )
      SearchFiltersExtendService.multiplyParamsByExtendAllFilters(paramsWithAllExtendableFilters) should be(
        allFieldMultipliedFilters
      )

    }
  }

}
