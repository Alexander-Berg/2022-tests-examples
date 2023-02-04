package ru.auto.salesman.service.impl

import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.ApiOfferModel.Category.{CARS, CATEGORY_UNKNOWN, MOTO, TRUCKS}
import ru.auto.salesman.dao.GoodsDao
import ru.auto.salesman.test.BaseSpec

class GoodsDaoProviderImplSpec extends BaseSpec {

  private val carsDao = mock[GoodsDao]
  private val categorizedDao = mock[GoodsDao]

  private val service =
    new GoodsDaoProviderImpl(carsDao, categorizedDao)

  "GoodsDaoProviderImplSpec" should {

    "choose goods service" in {
      service.chooseDao(CARS) shouldBe carsDao
    }

    "choose categorized service for moto" in {
      service.chooseDao(MOTO) shouldBe categorizedDao
    }

    "choose categorized service for trucks" in {
      service.chooseDao(TRUCKS) shouldBe categorizedDao
    }

    "throw on unknown category" in {
      intercept[RuntimeException](service.chooseDao(CATEGORY_UNKNOWN))
    }

    "throw on unrecognized" in {
      intercept[RuntimeException](service.chooseDao(Category.UNRECOGNIZED))
    }
  }
}
