package ru.auto.salesman.model.user.product

import ru.auto.salesman.model.DeprecatedDomains
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import ru.auto.salesman.model.user.product.ProductProvider.{AutoruBundles, AutoruGoods}
import ru.auto.salesman.test.BaseSpec

class ProductsSpec extends BaseSpec {

  "Products" should {
    "get default duration for all products" in {
      DeprecatedDomains.values.foreach { implicit domain =>
        Products.all.foreach { p =>
          Products.defaultDurationOf(p)
        }
      }
    }
    "get receipt name for all products" in {
      DeprecatedDomains.values.foreach { implicit domain =>
        Products.all.foreach { p =>
          Products.receiptNameOf(p)
        }
      }
    }
    "get all products by name" in {
      DeprecatedDomains.values.foreach { implicit domain =>
        Products.all.foreach { p =>
          val opt = Products.withNameOpt[AutoruProduct](p.name)
          val strict = Products.withName[AutoruProduct](p.name)
          opt should contain(p)
          strict shouldBe p
        }
      }
    }
    "get all products by alias" in {
      DeprecatedDomains.values.foreach { implicit domain =>
        Products.all.foreach { p =>
          val byName = Products.withNameOrAlias[AutoruProduct](p.name)
          val byAlias = Products.withNameOrAlias[AutoruProduct](p.alias)
          byName shouldBe p
          byAlias shouldBe p
        }
      }
    }
    "contain all AutoruGoods" in {
      Products.all(
        DeprecatedDomains.AutoRu
      ) should contain allElementsOf AutoruGoods.all
    }
    "contain all AutoruBundles" in {
      Products.all(
        DeprecatedDomains.AutoRu
      ) should contain allElementsOf AutoruBundles.all
    }

    "parse offers-history-reports-10" in {
      val byAlias =
        Products
          .withNameOrAliasOpt[AutoruProduct]("offers-history-reports-10")
          .get
      byAlias shouldBe OffersHistoryReports(10)
    }

    "parse offers-history-reports-1" in {
      val byAlias =
        Products
          .withNameOrAliasOpt[AutoruProduct]("offers-history-reports-1")
          .get
      byAlias shouldBe OffersHistoryReports(1)
    }

    "name for offers-history-reports-10 is offers-history-reports" in {
      OffersHistoryReports(10).name shouldBe "offers-history-reports"
    }
  }
}
