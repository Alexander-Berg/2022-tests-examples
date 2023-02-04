package ru.auto.salesman.service.user.autoru.price.service.ordering

import ru.auto.salesman.model.DeprecatedDomains
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators

class PriceOrderingSpec extends BaseSpec with UserModelGenerators {
  "PriceOrdering.forIosOrdering" should {
    "do increasing order by count" in {
      forAll(
        productPriceGen(product = OffersHistoryReports(10)),
        productPriceGen(product = OffersHistoryReports(1))
      ) { (priceWith10, priceWith1) =>
        List(priceWith10, priceWith1).sortWith(
          PriceOrdering.forIosOrdering
        ) shouldBe List(priceWith1, priceWith10)
      }
    }

    "don't change" in {
      forAll(
        productPriceGen(product = OffersHistoryReports(1)),
        productPriceGen(product = OffersHistoryReports(10))
      ) { (priceWith1, priceWith10) =>
        List(priceWith1, priceWith10).sortWith(
          PriceOrdering.forIosOrdering
        ) shouldBe List(priceWith1, priceWith10)
      }
    }
  }

  implicit override def domain = DeprecatedDomains.AutoRu
}
