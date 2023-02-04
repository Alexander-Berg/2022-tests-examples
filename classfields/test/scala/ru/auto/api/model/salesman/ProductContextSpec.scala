package ru.auto.api.model.salesman

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.AutoruProduct
import ru.auto.api.model.AutoruProduct.OffersHistoryReports
import ru.auto.api.model.gen.SalesmanModelGenerators._
import ru.auto.api.services.salesman.SalesmanUserClient.SalesmanVinOrLicencePlate

class ProductContextSpec extends BaseSpec with ScalaCheckPropertyChecks {

  "ProductContext factory" should {

    "build bundle context" in {
      forAll(PackageProductGen.suchThat(p => !AutoruProduct.subscriptionProduct(p)), ProductPriceGen) {
        (product, productPrice) =>
          val context = ProductContext(product, productPrice, vinOrLicencePlate = None, garageId = None)
          val bundle = context.getBundle
          bundle.getProductPrice shouldBe productPrice
      }
    }

    "build goods context" in {
      forAll(NonPackageProductGen.suchThat(p => !AutoruProduct.subscriptionProduct(p)), ProductPriceGen) {
        (product, productPrice) =>
          val context = ProductContext(product, productPrice, vinOrLicencePlate = None, garageId = None)
          val goods = context.getGoods
          goods.getProductPrice shouldBe productPrice
      }
    }

    "build subscription goods context" in {
      forAll(Gen.const(OffersHistoryReports), ProductPriceGen) { (product, productPrice) =>
        val vin = SalesmanVinOrLicencePlate("vinOrPlate")
        val context = ProductContext(product, productPrice, Some(vin), garageId = None)
        val goods = context.getSubscription
        goods.getProductPrice shouldBe productPrice
        goods.getVinOrLicensePlate shouldBe vin.value
      }
    }

  }
}
