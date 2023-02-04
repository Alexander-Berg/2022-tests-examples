package ru.auto.salesman.service.tskv.user.format

import org.joda.time.DateTime
import ru.auto.salesman.model.user.{Analytics, PriceModifier}
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.tskv.user.format.impl.tskv.ProductPriceMap
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators

class ProductPriceMapSpec extends BaseSpec with UserModelGenerators {

  "ProductPriceMap" should {

    "write user_excluded_from_discount" in {
      val analytics =
        Analytics(Some(Analytics.UserExcludedFromDiscount("test-id")))
      forAll(productPriceGen(analytics = Some(analytics)), productStatusGen) {
        (productPrice, status) =>
          ProductPriceMap.toMap(productPrice, status) should contain(
            "user_excluded_from_discount" -> "test-id"
          )
      }
    }

    "not write user_excluded_from_discount" in {
      forAll(productPriceGen(analytics = None), productStatusGen) {
        (productPrice, status) =>
          ProductPriceMap
            .toMap(productPrice, status)
            .values should not contain "user_excluded_from_discount"
      }
    }

    "write user_in_discount" in {
      val testTime = DateTime.now
      val testPeriodicalDiscount =
        PriceModifier.PeriodicalDiscount("test-id", 70, testTime)
      forAll(
        productPriceGen(price =
          priceGen(modifier =
            priceModifierGen(periodicalDiscount = Some(testPeriodicalDiscount))
              .map(Some(_))
          )
        ),
        productStatusGen
      ) { (productPrice, status) =>
        ProductPriceMap.toMap(productPrice, status) should contain(
          "user_in_discount" -> "test-id"
        )
      }
    }

    "not write user_in_discount" in {
      forAll(
        productPriceGen(price =
          priceGen(modifier = priceModifierGen(periodicalDiscount = None).map(Some(_)))
        ),
        productStatusGen
      ) { (productPrice, status) =>
        ProductPriceMap
          .toMap(productPrice, status)
          .values should not contain "user_in_discount"
      }
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
