package ru.yandex.realty.seller.model

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.{
  MoneyPromocodePriceModifier,
  PriceContext,
  ProductTypes,
  VasPromocodePriceModifier
}

import scala.collection.JavaConverters._

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class PurchaseWithProductsSpec extends SpecBase with PropertyChecks with SellerModelGenerators {

  "PurchaseWithProductsUtils" should {
    "calculateFullCost correct" in {

      forAll(passportUserGen) { (userRef) =>
        val productyListGenerator = Gen.sequence(
          Seq(
            makeProduct(
              userRef,
              ProductTypes.Promotion,
              Some(PriceContext(1L, 1L, Seq(MoneyPromocodePriceModifier("1", 1)), Seq.empty))
            ),
            makeProduct(userRef, ProductTypes.Premium, Some(PriceContext(2L, 1L, Iterable.empty, Iterable.empty))),
            makeProduct(userRef, ProductTypes.Promotion, Some(PriceContext(1L, 1L, Iterable.empty, Iterable.empty))),
            makeProduct(
              userRef,
              ProductTypes.Premium,
              Some(
                PriceContext(
                  2L,
                  1L,
                  Seq(
                    VasPromocodePriceModifier("2", ProductTypes.Premium, 2),
                    VasPromocodePriceModifier("3", ProductTypes.Premium, 3)
                  ),
                  Seq.empty
                )
              )
            ),
            makeProduct(userRef, ProductTypes.Premium, Some(PriceContext(3L, 2L, Iterable.empty, Iterable.empty))),
            makeProduct(userRef, ProductTypes.PackageTurbo, Some(PriceContext(0L, 0L, Iterable.empty, Iterable.empty))),
            makeProduct(userRef, ProductTypes.PackageRaising, None)
          )
        )
        forAll(productyListGenerator, purchaseGen) { (products, purchase) =>
          val result = PurchaseWithProducts(purchase, products.asScala).fullPrice
          result.effectivePrice should be(6L)
          result.basePrice should be(9L)
          result.modifiers.seq.size should be(3)
        }
      }
    }
  }

}
