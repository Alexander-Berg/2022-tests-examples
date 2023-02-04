package ru.auto.salesman.model.user.product

import ru.auto.salesman.model.user.product.ProductProvider.AutoProlongableForced
import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles.{Express, Turbo}
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods._
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions._
import ru.auto.salesman.test.BaseSpec

class AutoProlongableForcedSpec extends BaseSpec {

  "AutoProlongableForced" should {

    "be implemented only by forced autoprolongable products" in {
      val expected = Set(
        Placement,
        Top,
        Special,
        Turbo,
        Express
      )

      val autoProlongable =
        onlyAutoProlongableForced(ProductProvider.values.flatMap(_.all))
      val autoProlongableSubscriptions =
        onlyAutoProlongableForced(Companion.values.map(_(0)))

      (autoProlongable ++ autoProlongableSubscriptions) shouldBe expected
    }
  }

  private def onlyAutoProlongableForced(products: Set[AutoruProduct]) =
    products.collect { case p: AutoProlongableForced =>
      p
    }
}
