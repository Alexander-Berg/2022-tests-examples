package ru.auto.salesman.service

import ru.auto.api.ApiOfferModel.Category.CARS
import ru.auto.api.ApiOfferModel.Section._
import ru.auto.salesman.model.ProductId._
import ru.auto.salesman.model.RegionId
import ru.auto.salesman.service.PromocoderFeatureService.LoyaltyArgs
import ru.auto.salesman.test.BaseSpec

class LoyaltyArgsSpec extends BaseSpec {

  private val dealerRegionId: RegionId = RegionId(1)

  "OfferLoyaltyArgs.apply" should {

    "return cars:new for trade-in-request:cars:new" in {
      val product = TradeInRequestCarsNew
      val result = LoyaltyArgs(product, dealerRegionId).value
      result.category shouldBe CARS
      result.section shouldBe NEW
    }

    "return cars:used for trade-in-request:cars:used" in {
      val product = TradeInRequestCarsUsed
      val result = LoyaltyArgs(product, dealerRegionId).value
      result.category shouldBe CARS
      result.section shouldBe USED
    }

    "return cars:new for vin-history" in {
      val product = VinHistory
      val result = LoyaltyArgs(product, dealerRegionId)
      result shouldBe empty
    }
  }
}
