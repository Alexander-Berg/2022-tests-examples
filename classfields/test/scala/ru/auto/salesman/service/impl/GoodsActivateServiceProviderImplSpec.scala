package ru.auto.salesman.service.impl

import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.ApiOfferModel.Category.{CARS, CATEGORY_UNKNOWN, MOTO, TRUCKS}
import ru.auto.salesman.service.GoodsActivateService
import ru.auto.salesman.test.BaseSpec

class GoodsActivateServiceProviderImplSpec extends BaseSpec {

  private val carsService = mock[GoodsActivateService]
  private val categorizedService = mock[GoodsActivateService]

  private val service =
    new GoodsActivateServiceProviderImpl(carsService, categorizedService)

  "GoodsActivateServiceProviderImplSpec" should {

    "choose goods service" in {
      service.chooseService(CARS) shouldBe carsService
    }

    "choose categorized service for moto" in {
      service.chooseService(MOTO) shouldBe categorizedService
    }

    "choose categorized service for trucks" in {
      service.chooseService(TRUCKS) shouldBe categorizedService
    }

    "throw on unknown category" in {
      intercept[RuntimeException](service.chooseService(CATEGORY_UNKNOWN))
    }

    "throw on unrecognized" in {
      intercept[RuntimeException](service.chooseService(Category.UNRECOGNIZED))
    }
  }
}
