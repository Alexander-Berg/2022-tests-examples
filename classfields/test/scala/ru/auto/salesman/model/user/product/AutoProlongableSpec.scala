package ru.auto.salesman.model.user.product

import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles.{
  Express,
  Turbo,
  Vip
}
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods._
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions._
import ru.auto.salesman.model.user.product.ProductProvider.AutoProlongable
import ru.auto.salesman.test.BaseSpec

class AutoProlongableSpec extends BaseSpec {

  "AutoProlongable" should {

    // This test protects against accident `with AutoProlongable` for new products.
    // If this test failed for new product, one should:
    // 1. either remove `with AutoProlongable` in new product declaration
    // 2. or add new product into $expected in this test
    "be implemented only by autoprolongable products" in {
      val expected = Set(
        Placement,
        Special,
        Top,
        Turbo,
        Express,
        ShowInStories
      )
      val autoProlongable =
        onlyAutoProlongable(ProductProvider.values.flatMap(_.all))
      val autoProlongableSubscriptions =
        onlyAutoProlongable(Companion.values.map(_(0)))
      (autoProlongable ++ autoProlongableSubscriptions) shouldBe expected
    }

    // This test protects against forgotten `with AutoProlongable` for new products.
    // If this test failed for new product, one should:
    // 1. either add `with AutoProlongable` in new product declaration
    // 2. or add new product into $expected in this test
    "not be implemented only by non-autoprolongable products" in {
      val expected = Set(
        Boost,
        Refresh,
        Badge,
        Color,
        CertificationMobile,
        CertificationPlanned,
        OffersHistoryReports(0),
        Vip
      )
      val nonAutoProlongable =
        onlyNonAutoProlongable(ProductProvider.values.flatMap(_.all))
      val nonAutoProlongableSubscriptions =
        onlyNonAutoProlongable(Companion.values.map(_(0)))
      (nonAutoProlongable ++ nonAutoProlongableSubscriptions) shouldBe expected
    }
  }

  private def onlyAutoProlongable(products: Set[AutoruProduct]) =
    products.collect { case p: AutoProlongable => p }

  private def onlyNonAutoProlongable(products: Set[AutoruProduct]) =
    products.filter {
      case _: AutoProlongable => false
      case _ => true
    }
}
