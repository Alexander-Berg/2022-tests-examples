package ru.auto.api.managers.price

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.CommonModel.PaidReason
import ru.auto.api.CommonModel.PaidService.PaymentReason
import ru.auto.api.model.gen.SalesmanModelGenerators
import ru.auto.api.model.AutoruProduct
import ru.auto.api.services.billing.util.fromCents
import ru.auto.salesman.model.user.ApiModel.ProductPrice

import scala.math.BigDecimal.RoundingMode

class UserPriceConverterSpec extends BaseSpec with ScalaCheckPropertyChecks with SalesmanModelGenerators {

  "UserPriceConverter" should {
    "convert ProductPrice to PaidServicePrice" in {
      forAll(ProductPriceGen) { productPrice =>
        val paidServicePrice =
          UserPriceConverter.toPaidServicePrice(productPrice)
        paidServicePrice.getService shouldBe UserPriceConverter.toService(productPrice.getProduct)
        paidServicePrice.getProlongationAllowed shouldBe productPrice.getProlongationAllowed
        paidServicePrice.getProlongationForced shouldBe productPrice.getProlongationForced
        paidServicePrice.getProlongationForcedNotTogglable shouldBe productPrice.getProlongationForcedNotTogglable

        paidServicePrice.getPaymentReason shouldBe productPrice.getPaymentReason

        if (productPrice.hasPrice) {
          paidServicePrice.getCurrency shouldBe "RUR"
        }

        if (productPrice.hasProductPriceInfo) {
          paidServicePrice.getName shouldBe productPrice.getProductPriceInfo.getName
          paidServicePrice.getTitle shouldBe productPrice.getProductPriceInfo.getTitle
          paidServicePrice.getDescription shouldBe productPrice.getProductPriceInfo.getDescription
          paidServicePrice.getAutoApplyPrice shouldBe fromCents(
            productPrice.getProductPriceInfo.getAutoApplyPrice.toInt
          )
          paidServicePrice.getMultiplier shouldBe productPrice.getProductPriceInfo.getMultiplier
          paidServicePrice.getAliasesList shouldBe productPrice.getProductPriceInfo.getAliasesList
        }
      }
    }

    "package products with equal base and effective price" in {
      forAll(PackageProductPriceGen) { productPrice =>
        val basePrice = productPrice.getPrice.getBasePrice
        val price = productPrice.getPrice.toBuilder
          .setEffectivePrice(basePrice)
          .setBasePrice(basePrice)
        val pp = productPrice.toBuilder.setPrice(price).build
        val psp = UserPriceConverter.toPaidServicePrice(pp)
        if (productPrice.hasPrice) {
          psp.getPrice shouldBe pp.getPrice.getEffectivePrice.toInt / 100
          psp.getOriginalPrice shouldBe BigDecimal(pp.getPrice.getBasePrice / 100 / (1 - 0.4))
            .setScale(0, RoundingMode.FLOOR)
            .toInt
        }
      }
    }

    "package products with not equal base and effective price" in {
      forAll(PackageProductPriceGen) { productPrice =>
        val basePrice = productPrice.getPrice.getBasePrice
        val effectivePrice = basePrice * 3
        val price = productPrice.getPrice.toBuilder
          .setBasePrice(basePrice)
          .setEffectivePrice(effectivePrice)
        val pp = productPrice.toBuilder.setPrice(price).build
        val psp = UserPriceConverter.toPaidServicePrice(pp)
        if (productPrice.hasPrice) {
          psp.getPrice shouldBe pp.getPrice.getEffectivePrice.toInt / 100
          psp.getOriginalPrice shouldBe pp.getPrice.getBasePrice.toInt / 100
        }
      }
    }

    "prices for not package product" in {
      forAll(NonPackageProductPriceGen) { productPrice =>
        val psp = UserPriceConverter.toPaidServicePrice(productPrice)
        if (productPrice.hasPrice) {
          if (psp.getPrice != psp.getOriginalPrice) {
            psp.getPrice shouldBe productPrice.getPrice.getEffectivePrice.toInt / 100
            psp.getOriginalPrice shouldBe productPrice.getPrice.getBasePrice.toInt / 100
          } else {
            psp.getPrice shouldBe productPrice.getPrice.getEffectivePrice.toInt / 100
            psp.getOriginalPrice shouldBe 0
          }
        }
      }
    }

    "prices for not package product for equal effective and base prices" in {
      forAll(NonPackageProductPriceGen) { productPriceGenerated =>
        val basePrice = productPriceGenerated.getPrice.getBasePrice
        val productPriceBuilder = productPriceGenerated.toBuilder
        productPriceBuilder.getPriceBuilder.setEffectivePrice(basePrice)
        productPriceBuilder.build

        val productPrice = productPriceBuilder.build
        val psp = UserPriceConverter.toPaidServicePrice(productPrice)
        if (productPrice.hasPrice) {
          psp.getPrice shouldBe productPrice.getPrice.getEffectivePrice.toInt / 100
          psp.getOriginalPrice shouldBe 0
        }
      }
    }

    "convert PaymentReason to PaidReason" in {
      forAll(PaymentReasonGen) { paymentReason =>
        val paidReason = UserPriceConverter.toPaidReason(paymentReason)
        paymentReason match {
          case PaymentReason.UNKNOWN_PAYMENT_REASON =>
            paidReason shouldBe PaidReason.REASON_UNKNOWN
          case PaymentReason.PAID_OFFER =>
            paidReason shouldBe PaidReason.PAYMENT_GROUP
          case PaymentReason.PREMIUM_OFFER =>
            paidReason shouldBe PaidReason.PREMIUM_OFFER
          case PaymentReason.USER_QUOTA_EXCEED =>
            paidReason shouldBe PaidReason.USER_QUOTA
          case PaymentReason.DUPLICATE_OFFER =>
            paidReason shouldBe PaidReason.SAME_SALE
          case PaymentReason.FREE_LIMIT_EXCEED =>
            paidReason shouldBe PaidReason.FREE_LIMIT
          case PaymentReason.UNRECOGNIZED =>
            paidReason shouldBe PaidReason.REASON_UNKNOWN
        }
      }
    }

    "convert PackageContent to PaidServicePrice" in {
      forAll(PackageContentGen) { packageContent =>
        val paidServicePrice =
          UserPriceConverter.toPackageServices(packageContent)
        paidServicePrice.getService shouldBe packageContent.getAlias
        paidServicePrice.getName shouldBe packageContent.getName
      }
    }

    "convert PaidServicePrice to ActivationPrice with promocodeId" in {
      forAll(ProductPriceGen, PriceModifierWithFeatureGen) { (productPriceGenerated, priceModifier) =>
        val price =
          productPriceGenerated.getPrice.toBuilder.setModifier(priceModifier)
        val productPrice =
          productPriceGenerated.toBuilder.setPrice(price).build
        val activationPrice =
          UserPriceConverter.toActivationPrice(productPrice)
        activationPrice.paidReason.get shouldBe UserPriceConverter
          .toPaidReason(productPrice.getPaymentReason)
        activationPrice.value shouldBe productPrice.getPrice.getEffectivePrice.toInt / 100
        activationPrice.promocodeId.get shouldBe productPrice.getPrice.getModifier.getPromocoderFeature.getId
      }
    }

    "convert PaidServicePrice to ActivationPrice without price" in {
      forAll(ProductPriceGen) { productPriceGenerated =>
        val productPrice =
          productPriceGenerated.toBuilder.clearPrice.build
        val activationPrice =
          UserPriceConverter.toActivationPrice(productPrice)
        activationPrice.paidReason.get shouldBe UserPriceConverter
          .toPaidReason(productPrice.getPaymentReason)
        activationPrice.value shouldBe productPrice.getPrice.getEffectivePrice.toInt
        activationPrice.promocodeId shouldBe None
      }
    }

    "convert PaidServicePrice to ActivationPrice without modifier" in {
      forAll(ProductPriceGen) { productPriceGenerated =>
        val price = productPriceGenerated.getPrice.toBuilder.clearModifier.build
        val productPrice =
          productPriceGenerated.toBuilder.setPrice(price).build
        val activationPrice =
          UserPriceConverter.toActivationPrice(productPrice)
        activationPrice.paidReason.get shouldBe UserPriceConverter
          .toPaidReason(productPrice.getPaymentReason)
        activationPrice.value shouldBe productPrice.getPrice.getEffectivePrice / 100
        activationPrice.promocodeId shouldBe None
      }
    }

    "convert PaidServicePrice to ActivationPrice with empty promocodeId" in {
      forAll(ProductPriceGen, PriceModifierWithFeatureGen) { (productPriceGenerated, priceModifierWithFeature) =>
        val feature =
          priceModifierWithFeature.getPromocoderFeature.toBuilder.setId("")
        val priceModifier =
          priceModifierWithFeature.toBuilder.setPromocoderFeature(feature)
        val price =
          productPriceGenerated.getPrice.toBuilder.setModifier(priceModifier)
        val productPrice =
          productPriceGenerated.toBuilder.setPrice(price).build

        val activationPrice =
          UserPriceConverter.toActivationPrice(productPrice)
        activationPrice.paidReason.get shouldBe UserPriceConverter
          .toPaidReason(productPrice.getPaymentReason)
        activationPrice.value shouldBe productPrice.getPrice.getEffectivePrice / 100
        activationPrice.promocodeId shouldBe None
      }
    }

    "convert to old product name from salesman product name" in {
      forAll(ProductPriceGen) { productPrice =>
        val paidServicePrice =
          UserPriceConverter.toPaidServicePrice(productPrice)
        AutoruProduct.values.map(_.salesName).toList should contain(paidServicePrice.getService)
      }
    }

    "set autoProlongPrice using prolongPrice" in {
      val prolongPrice = 10000
      forAll(productPriceGen(price = priceGen(prolongPrice = Gen.const(prolongPrice)), prolongationAllowed = true)) {
        productPrice =>
          val paidServicePrice = UserPriceConverter.toPaidServicePrice(productPrice)
          paidServicePrice.getAutoProlongPrice.getValue shouldBe 100
      }
    }

    "set autoProlongPrice using basePrice" in {
      val basePrice = 20000
      forAll(
        productPriceGen(
          price = priceGen(basePrice = Gen.const(basePrice), prolongPrice = Gen.const(0)),
          prolongationAllowed = true
        )
      ) { productPrice =>
        val paidServicePrice = UserPriceConverter.toPaidServicePrice(productPrice)
        paidServicePrice.getAutoProlongPrice.getValue shouldBe 200
      }
    }

    "not set autoProlongPrice for 0 prolongPrice and basePrice" in {
      forAll(
        productPriceGen(
          price = priceGen(basePrice = Gen.const(0), prolongPrice = Gen.const(0)),
          prolongationAllowed = true
        )
      ) { productPrice =>
        val paidServicePrice = UserPriceConverter.toPaidServicePrice(productPrice)
        paidServicePrice.hasAutoProlongPrice shouldBe false
      }
    }

    "not set autoProlongPrice if prolongationAllowed = false" in {
      forAll(productPriceGen(prolongationAllowed = false)) { productPrice =>
        val paidServicePrice = UserPriceConverter.toPaidServicePrice(productPrice)
        paidServicePrice.hasAutoProlongPrice shouldBe false
      }
    }

    "set prolongationIntervalWillExpire" in {
      forAll(
        productPriceGen(price = priceGen(modifier = priceModifierGen(prolongInterval = Gen.some(prolongIntervalGen()))))
      ) { productPrice =>
        val paidServicePrice = UserPriceConverter.toPaidServicePrice(productPrice)
        paidServicePrice.getProlongationIntervalWillExpire shouldBe productPrice.getPrice.getModifier.getProlongInterval.getWillExpire
      }
    }

    "set purchase_forbidden = true if got purchase_forbidden = true from salesman_user" in {
      val purchaseForbidden = {
        val b = ProductPrice.newBuilder().setProduct("offers-history-reports")
        b.getProductPriceInfoBuilder
          .setPurchaseForbidden(true)
        b.build()
      }
      val result = UserPriceConverter.toPaidServicePrice(purchaseForbidden)
      result.getPurchaseForbidden shouldBe true
    }

    "don't set purchase_forbidden if got purchase_forbidden = false from salesman_user" in {
      val result =
        UserPriceConverter.toPaidServicePrice(ProductPrice.newBuilder().setProduct("offers-history-reports").build())
      result.getPurchaseForbidden shouldBe false
    }
  }
}
