package ru.auto.salesman.service.impl.user

import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.user.PriceService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators

class PriceServiceSpec extends BaseSpec with UserModelGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  "PriceService" should {
    "convert price to funds" in {
      PriceService.priceToFunds(1) shouldBe 100
      PriceService.priceToFunds(1.25) shouldBe 125
      PriceService.priceToFunds(100) shouldBe 10000
      PriceService.priceToFunds(1.123456) shouldBe 112
    }

    "get correct rounded discount" in {
      val price = PriceService.priceToFunds(499)

      PriceService.priceWithDiscount(price, 20) shouldBe PriceService
        .priceToFunds(400)
      PriceService.priceWithDiscount(price, 50) shouldBe PriceService
        .priceToFunds(250)
      PriceService.priceWithDiscount(price, 70) shouldBe PriceService
        .priceToFunds(150)
    }

  }

}
