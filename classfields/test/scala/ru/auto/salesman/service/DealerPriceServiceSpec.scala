package ru.auto.salesman.service

import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators

class DealerPriceServiceSpec extends BaseSpec with UserModelGenerators {
  "PriceService" should {
    "get correct rounded discount with kopecks" in {
      val price = 49900

      DealerPriceService.priceWithDiscountWithKoopeks(100, 33) shouldBe 67
      DealerPriceService.priceWithDiscountWithKoopeks(price, 20) shouldBe 39920
      DealerPriceService.priceWithDiscountWithKoopeks(price, 50) shouldBe 24950
      DealerPriceService.priceWithDiscountWithKoopeks(price, 70) shouldBe 14970
      DealerPriceService.priceWithDiscountWithKoopeks(price, 8) shouldBe 45908
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
