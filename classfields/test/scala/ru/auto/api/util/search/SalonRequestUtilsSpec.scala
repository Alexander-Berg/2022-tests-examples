package ru.auto.api.util.search

import ru.auto.api.BaseSpec
import ru.auto.api.model.CategorySelector._
import ru.auto.api.search.SearchModel.{RegionFilter, SalonSearchRequestParameters}

class SalonRequestUtilsSpec extends BaseSpec with SalonRequestUtils {

  private val mm = Map(
    "salon_id" -> Set("123", "456"),
    "tariff_type" -> Set("cars", "TRUCKS"),
    "car_mark" -> Set("AUDI", "BMW"),
    "salon_net_id" -> Set("789"),
    "salon_net_code" -> Set("qweqwe"),
    "rid" -> Set("213")
  )

  private val apiReq = SalonSearchRequestParameters
    .newBuilder()
    .addSalonId(123)
    .addSalonId(456)
    .addCarMark("AUDI")
    .addCarMark("BMW")
    .addSalonNetId(789)
    .addSalonNetCode("qweqwe")
    .addRegionFilter(RegionFilter.newBuilder().setRid(213).build())
    .addTariffType(Cars.`enum`)
    .addTariffType(Trucks.`enum`)
    .build()

  private val searchParams = Map(
    "dealer_id" -> Set("123", "456"),
    "tariff_type" -> Set("AUTO", "TRUCKS"),
    "mark" -> Set("AUDI", "BMW"),
    "dealer_net_id" -> Set("789"),
    "dealer_net_semantic_url" -> Set("qweqwe"),
    "rid" -> Set("213")
  )

  "requestParamsToApiRequest" in {
    requestParamsToApiRequest(mm) shouldEqual apiReq
  }

  "apiRequestToSearcherParams" in {
    apiRequestToSearcherParams(apiReq) shouldEqual searchParams
  }
}
