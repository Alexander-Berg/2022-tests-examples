package ru.auto.salesman.client.pushnoy

import ru.auto.salesman.test.BaseSpec

class PushnoyDeliverySpec extends BaseSpec {
  "PushnoyDelivery enumeration" should {
    "return toString result in camel case with first lower case letter" in {
      PushnoyDelivery.ServicesAndDiscounts.entryName shouldBe "servicesAndDiscounts"
      PushnoyDelivery.PersonalRecommendations.entryName shouldBe "personalRecommendations"
    }
  }

}
