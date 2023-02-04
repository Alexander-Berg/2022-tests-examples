package ru.auto.salesman.service.impl.user.autoru.price.service

import org.scalacheck.Gen
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.model.user.ProductPriceInfo
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators

class ProductPriceInfoCalculatorSpec extends BaseSpec with ServiceModelGenerators {

  import ProductPriceInfoCalculatorImpl._

  "RichProductPriceInfo.withAutoApplyPrice" should {
    "update autoApplyPrice" in {
      forAll(ProductPriceInfoGen, Gen.posNum[Int]) { (productPriceInfo, price) =>
        val res = productPriceInfo.withAutoApplyPrice(Some(price))
        val productPriceInfoEnriched =
          productPriceInfo.copy(autoApplyPrice = Some(price))
        res shouldBe productPriceInfoEnriched
      }
    }

    "return old productPriceInfo if price is None" in {
      forAll(ProductPriceInfoGen) { productPriceInfo =>
        val res =
          productPriceInfo.withAutoApplyPrice(None)
        res shouldBe productPriceInfo
      }
    }
  }

  "RichProductPriceInfo.toOption" should {
    "return None if empty ProductPriceInfo" in {
      ProductPriceInfo.empty.toOption shouldBe None
    }

    "return Some if productPriceInfo not None" in {
      forAll(ProductPriceInfoGen.suchThat(_ != ProductPriceInfo.empty)) {
        productPriceInfo =>
          productPriceInfo.toOption shouldBe Some(productPriceInfo)
      }
    }
  }

  "RichProductPriceInfo." should {
    "set placement alias" in {
      forAll(ProductPriceInfoGen.map(_.copy(aliases = Nil))) { productPriceInfo =>
        val res =
          productPriceInfo.withFullNameAliases(Placement)
        res.aliases.loneElement shouldBe "placement"
      }
    }
    "set offers-history-reports-10 alias" in {
      forAll(ProductPriceInfoGen.map(_.copy(aliases = Nil))) { productPriceInfo =>
        val res =
          productPriceInfo.withFullNameAliases(OffersHistoryReports(10))
        res.aliases.loneElement shouldBe "offers-history-reports-10"
      }
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
