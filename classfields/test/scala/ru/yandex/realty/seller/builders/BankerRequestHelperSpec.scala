package ru.yandex.realty.seller.builders

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.{ProductTypes, _}
import ru.yandex.realty.seller.service.builders.BankerRequestHelper

import scala.collection.JavaConverters._

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class BankerRequestHelperSpec extends SpecBase with PropertyChecks with SellerModelGenerators {

  "BankerRequestHelper" should {
    "Generate correct receipt" in {
      forAll(passportUserGen) { (userRef) =>
        val productyListGenerator = Gen.sequence(
          Seq(
            makeProduct(userRef, ProductTypes.Promotion, Some(PriceContext(1L, 1L, Iterable.empty, Iterable.empty))),
            makeProduct(userRef, ProductTypes.Premium, Some(PriceContext(2L, 1L, Iterable.empty, Iterable.empty))),
            makeProduct(userRef, ProductTypes.Promotion, Some(PriceContext(1L, 1L, Iterable.empty, Iterable.empty))),
            makeProduct(userRef, ProductTypes.Premium, Some(PriceContext(2L, 1L, Iterable.empty, Iterable.empty))),
            makeProduct(userRef, ProductTypes.Premium, Some(PriceContext(3L, 2L, Iterable.empty, Iterable.empty))),
            makeProduct(userRef, ProductTypes.PackageTurbo, Some(PriceContext(0L, 0L, Iterable.empty, Iterable.empty))),
            makeProduct(userRef, ProductTypes.PackageRaising, None)
          )
        )
        forAll(productyListGenerator) { products =>
          val result = BankerRequestHelper.buildReceiptData(products.asScala, "")

          result.getGoodsCount should be(3)
          val premium1 = result.getGoodsList.asScala
            .find(
              g =>
                g.getName == BankerRequestHelper.ProductLabels.getOrElse(ProductTypes.Premium, "") &&
                  g.getPrice == 1L
            )
          val premium2 = result.getGoodsList.asScala
            .find(
              g =>
                g.getName == BankerRequestHelper.ProductLabels.getOrElse(ProductTypes.Premium, "") &&
                  g.getPrice == 2L
            )
          val promotion = result.getGoodsList.asScala
            .find(_.getName == BankerRequestHelper.ProductLabels.getOrElse(ProductTypes.Promotion, ""))
          premium1.isDefined should be(true)
          premium1.map(_.getPrice) should be(Some(1L))
          premium1.map(_.getQuantity) should be(Some(2))
          premium2.isDefined should be(true)
          premium2.map(_.getPrice) should be(Some(2L))
          premium2.map(_.getQuantity) should be(Some(1))
          promotion.isDefined should be(true)
          promotion.map(_.getPrice) should be(Some(1L))
          promotion.map(_.getQuantity) should be(Some(2))
        }
      }
    }
  }

}
