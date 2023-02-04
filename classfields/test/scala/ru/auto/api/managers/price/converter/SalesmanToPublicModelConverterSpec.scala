package ru.auto.api.managers.price.converter

import com.google.protobuf.{Duration, Timestamp}
import org.joda.time.DateTime
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.CommonModel.PaidService.{PaymentReason => CommmonPaymentReason}
import ru.auto.api.managers.price.converter.SalesmanToPublicModelConverter._
import ru.auto.api.managers.price.converter.SalesmanToPublicModelConverterSpec.SalesmanModelHelper._
import ru.auto.api.managers.price.converter.SalesmanToPublicModelConverterSpec.PublicModelHelper._
import ru.auto.api.model.gen.BasicGenerators
import ru.auto.salesman.model.user.{ApiModel => salesmanModel}
import vsmoney.public_model.{PublicVsmoneyModel => publicModel}

import scala.jdk.CollectionConverters._

class SalesmanToPublicModelConverterSpec extends BaseSpec with ScalaCheckPropertyChecks with BasicGenerators {
  val timestamp = Timestamp.newBuilder.setSeconds(DateTime.now.getMillis / 1000)

  val daySeconds = 24 * 60 * 60
  val duration = Duration.newBuilder.setSeconds(daySeconds).build

  "SalesmanToPublicModelConverter" should {
    "convert simple fields" in {
      val salesmanProductPrice = salesmanModel.ProductPrice
        .newBuilder()
        .setProduct("offers-history-reports")
        .setCounter(10)
        .setDuration(duration)
        .setDays(5)
        .setProlongationAllowed(true)
        .setProlongationForced(true)
        .setProlongationForcedNotTogglable(true)
        .build

      val publicProductPrice = publicModel.ProductPrice
        .newBuilder()
        .setProduct("offers-history-reports")
        .setDuration(duration)
        .setCounter(10)
        .setDays(5)
        .setProlongationAllowed(true)
        .setProlongationForced(true)
        .setProlongationForcedNotTogglable(true)
        .build

      toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
      salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
    }

    "convert right prolongation flag" in {
      forAll(bool, bool, bool) { (prolongationAllowed, prolongationForced, prolongationForcedNotTogglable) =>
        val salesmanProductPrice = salesmanModel.ProductPrice
          .newBuilder()
          .setProlongationAllowed(prolongationAllowed)
          .setProlongationForced(prolongationForced)
          .setProlongationForcedNotTogglable(prolongationForcedNotTogglable)
          .build()

        val publicProductPrice = publicModel.ProductPrice
          .newBuilder()
          .setProlongationAllowed(prolongationAllowed)
          .setProlongationForced(prolongationForced)
          .setProlongationForcedNotTogglable(prolongationForcedNotTogglable)
          .build()

        toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
        salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
      }
    }

    "convert payment reason" in {
      def withSalesmanProductPrice(commmonPaymentReason: CommmonPaymentReason): salesmanModel.ProductPrice = {
        salesmanModel.ProductPrice.newBuilder.setPaymentReason(commmonPaymentReason).build
      }

      def withPublicProductPrice(publicPaymentReason: publicModel.PaymentReason): publicModel.ProductPrice = {
        publicModel.ProductPrice.newBuilder.setPaymentReason(publicPaymentReason).build
      }

      toPublicModel(withSalesmanProductPrice(CommmonPaymentReason.UNKNOWN_PAYMENT_REASON)) shouldBe withPublicProductPrice(
        publicModel.PaymentReason.UNKNOWN_PAYMENT_REASON
      )
      toPublicModel(withSalesmanProductPrice(CommmonPaymentReason.FREE_LIMIT_EXCEED)) shouldBe withPublicProductPrice(
        publicModel.PaymentReason.FREE_LIMIT_EXCEED
      )
      toPublicModel(withSalesmanProductPrice(CommmonPaymentReason.USER_QUOTA_EXCEED)) shouldBe withPublicProductPrice(
        publicModel.PaymentReason.USER_QUOTA_EXCEED
      )
      toPublicModel(withSalesmanProductPrice(CommmonPaymentReason.PAID_OFFER)) shouldBe withPublicProductPrice(
        publicModel.PaymentReason.PAID_OFFER
      )
      toPublicModel(withSalesmanProductPrice(CommmonPaymentReason.PREMIUM_OFFER)) shouldBe withPublicProductPrice(
        publicModel.PaymentReason.PREMIUM_OFFER
      )
      toPublicModel(withSalesmanProductPrice(CommmonPaymentReason.DUPLICATE_OFFER)) shouldBe withPublicProductPrice(
        publicModel.PaymentReason.DUPLICATE_OFFER
      )
    }

    "convert price convert simple fields" in {
      val salesmanPrice =
        salesmanModel.Price.newBuilder.setBasePrice(10).setEffectivePrice(100).setProlongPrice(1000).build
      val publicPrice = publicModel.Price.newBuilder.setBasePrice(10).setEffectivePrice(100).setProlongPrice(1000).build

      val salesmanProductPrice = salesmanModel.ProductPrice.newBuilder.setPrice(salesmanPrice).build
      val publicProductPrice = publicModel.ProductPrice.newBuilder.setPrice(publicPrice).build

      toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
      salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
    }

    "convert empty price modifier" in {
      val salesmanPriceModifier = salesmanModel.PriceModifier.newBuilder
      val salesmanProductPrice = withSalesmanPriceModifier(salesmanPriceModifier)

      val publicPriceModifier = publicModel.PriceModifier.newBuilder
      val publicProductPrice = withPublicPriceModifier(publicPriceModifier)

      toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
      salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
    }

    "convert price modifier convert simple fields" in {
      val salesmanPriceModifier = salesmanModel.PriceModifier.newBuilder
        .setBundleId("id-123")
        .setPilotId("pilot-123")
      val salesmanProductPrice = withSalesmanPriceModifier(salesmanPriceModifier)

      val publicPriceModifier = publicModel.PriceModifier.newBuilder
        .setBundleId("id-123")
        .setPilotId("pilot-123")
      val publicProductPrice = withPublicPriceModifier(publicPriceModifier)

      toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
      salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
    }

    "convert price modifier, discount" in {

      val salesmanPeriodicalDiscount =
        salesmanModel.PriceModifier.PeriodicalDiscount.newBuilder.setDiscount(70).setDeadline(timestamp).setId("123")
      val salesmanProductPrice = withSalesmanPeriodicalDiscount(salesmanPeriodicalDiscount)

      val publicPeriodicalDiscount =
        publicModel.PriceModifier.PeriodicalDiscount.newBuilder.setDiscount(70).setDeadline(timestamp).setId("123")
      val publicProductPrice = withPublicPeriodicalDiscount(publicPeriodicalDiscount)

      toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
      salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
    }

    "convect price modifier, prolongInterval" in {
      val salesmanProlongInterval =
        salesmanModel.PriceModifier.ProlongInterval.newBuilder.setProlongPrice(100).setWillExpire(timestamp)
      val salesmanProductPrice = withSalesmanProlongInterval(salesmanProlongInterval)

      val publicProlongInterval =
        publicModel.PriceModifier.ProlongInterval.newBuilder.setProlongPrice(100).setWillExpire(timestamp)
      val publicProductPrice = withPublicProlongInterval(publicProlongInterval)

      toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
      salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
    }

    "convect price modifier, promocoderFeature, convert simple fields" in {
      val salesmanFeature =
        salesmanModel.PriceModifier.Feature.newBuilder.setCount(10).setDeadline(timestamp).setId("feature-123")
      val salesmanProductPrice = withSalesmanFeature(salesmanFeature)

      val publicFeature =
        publicModel.PriceModifier.Feature.newBuilder.setCount(10).setDeadline(timestamp).setId("feature-123")
      val publicProductPrice = withPublicFeature(publicFeature)

      toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
      salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
    }

    "price modifier, promocoder feature, feature payload, convert feature unit field" in {

      toPublicModel(withSalesmanFeatureUnit(salesmanModel.FeatureUnit.UNKNOWN_FEATURE_UNIT)) shouldBe withPublicFeatureUnit(
        publicModel.FeatureUnit.UNKNOWN_FEATURE_UNIT
      )
      toPublicModel(withSalesmanFeatureUnit(salesmanModel.FeatureUnit.MONEY)) shouldBe withPublicFeatureUnit(
        publicModel.FeatureUnit.MONEY
      )
      toPublicModel(withSalesmanFeatureUnit(salesmanModel.FeatureUnit.ITEMS)) shouldBe withPublicFeatureUnit(
        publicModel.FeatureUnit.ITEMS
      )
    }

    "price modifier, promocoder feature, feature payload, convert feature type field" in {
      toPublicModel(withSalesmanFeatureType(salesmanModel.FeatureType.UNKNOWN_FEATURE_TYPE)) shouldBe withPublicFeatureType(
        publicModel.FeatureType.UNKNOWN_FEATURE_TYPE
      )
      toPublicModel(withSalesmanFeatureType(salesmanModel.FeatureType.BUNDLE)) shouldBe withPublicFeatureType(
        publicModel.FeatureType.BUNDLE
      )
      toPublicModel(withSalesmanFeatureType(salesmanModel.FeatureType.LOYALTY)) shouldBe withPublicFeatureType(
        publicModel.FeatureType.LOYALTY
      )
      toPublicModel(withSalesmanFeatureType(salesmanModel.FeatureType.PROMOCODE)) shouldBe withPublicFeatureType(
        publicModel.FeatureType.PROMOCODE
      )
    }

    "price modifier, promocoder feature, feature payload, convert feature constraints field" in {
      val salesmanFeatureConstraints = salesmanModel.FeaturePayload.Constraints.newBuilder.setOfferId("abc-1234")
      val salesmanProductPrice = withSalesmanFeatureConstainsts(salesmanFeatureConstraints)

      val publicFeatureConstaints =
        publicModel.FeaturePayload.Constraints.newBuilder.setOfferId("abc-1234")
      val publicProductPrice = withPublicFeatureConstainsts(publicFeatureConstaints)

      toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
      salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
    }

    "price modifier, promocoder feature, feature payload, convert feature discount value" in {
      val salesmanfeatureDiscount = salesmanModel.FeatureDiscount.newBuilder.setValue(30)
      val salesmanProductPrice = withSalesmanFeatureDiscount(salesmanfeatureDiscount)

      val publicFeatureDiscount = publicModel.FeatureDiscount.newBuilder.setValue(30)
      val publicProductPrice = withPublicFeatureDiscount(publicFeatureDiscount)

      toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
      salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
    }

    "price modifier, promocoder feature, feature payload, convert feature discount type" in {
      toPublicModel(withSalesmanFeatureDiscountType(salesmanModel.FeatureDiscount.DiscountType.UNKNOWN_DISCOUNT_TYPE)) shouldBe withPublicFeatureDiscountType(
        publicModel.FeatureDiscount.DiscountType.UNKNOWN_DISCOUNT_TYPE
      )
      toPublicModel(withSalesmanFeatureDiscountType(salesmanModel.FeatureDiscount.DiscountType.PERCENT)) shouldBe withPublicFeatureDiscountType(
        publicModel.FeatureDiscount.DiscountType.PERCENT
      )
      toPublicModel(withSalesmanFeatureDiscountType(salesmanModel.FeatureDiscount.DiscountType.FIXED_PRICE)) shouldBe withPublicFeatureDiscountType(
        publicModel.FeatureDiscount.DiscountType.FIXED_PRICE
      )
    }

    "price modifier, promocoder feature, feature payload, convert bundle parameters field" in {
      val salesmanFeaturePayload = salesmanModel.FeaturePayload.BundleParameters.newBuilder.setProductDuration(duration)
      val salesmanProductPrice = withSalesmanFeatureBundleParameters(salesmanFeaturePayload)

      val publicFeaturePayload =
        publicModel.FeaturePayload.BundleParameters.newBuilder.setProductDuration(duration)
      val publicProductPrice = withPublicFeatureBundleParameters(publicFeaturePayload)

      toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
      salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
    }

    "convert price modifier, removeQuota" in {
      val salesmanRemoveUserQuota = salesmanModel.PriceModifier.RemoveUserQuota.newBuilder
        .setOriginalProlongPrice(200)
      val salesmanProductPrice = withSalesmanRemoveUserQuota(salesmanRemoveUserQuota)

      val publicRemoveUserQuota = publicModel.PriceModifier.RemoveUserQuota.newBuilder
        .setOriginalProlongPrice(200)
      val publicProductPrice = withPublicRemoveUserQuota(publicRemoveUserQuota)

      toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
      salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
    }

    "convert price modifier, restoreQuota" in {
      val salesmanRestoreUserQuota = salesmanModel.PriceModifier.RestoreUserQuota.newBuilder
        .setOriginalProlongPrice(300)
      val salesmanProductPrice = withSalesmanRestoreUserQuota(salesmanRestoreUserQuota)

      val publicRestoreUserQuota = publicModel.PriceModifier.RestoreUserQuota.newBuilder
        .setOriginalProlongPrice(300)
      val publicProductPrice = withPublicRestoreUserQuota(publicRestoreUserQuota)

      toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
      salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
    }

    "convert product price with empty quota left" in {
      val salesmanProductPriceInfo = salesmanModel.ProductPriceInfo.newBuilder
      val salesmanProductPrice = withSalesmanProductPriceInfo(salesmanProductPriceInfo)

      val publicProductPriceInfo = publicModel.ProductPriceInfo.newBuilder
      val publicProductPrice = withPublicProductPriceInfo(publicProductPriceInfo)

      toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
      salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
    }

    "convert product price info first level fields" in {
      val salesmanProductPriceInfo = salesmanModel.ProductPriceInfo.newBuilder
        .setName("product name")
        .setTitle("product title")
        .setDescription("product description")
        .setMultiplier(3)
        .addAllAliases(List("first alias", "second alias").asJava)
        .setAutoApplyPrice(500)
        .setQuotaLeft(12)
      val salesmanProductPrice = withSalesmanProductPriceInfo(salesmanProductPriceInfo)

      val publicProductPriceInfo = publicModel.ProductPriceInfo.newBuilder
        .setName("product name")
        .setTitle("product title")
        .setDescription("product description")
        .setMultiplier(3)
        .addAllAliases(List("first alias", "second alias").asJava)
        .setAutoApplyPrice(500)
        .setQuotaLeft(12)

      val publicProductPrice = withPublicProductPriceInfo(publicProductPriceInfo)

      toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
      salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
    }

    "convert product price info, package content" in {
      val salesmanPackageContent = salesmanModel.PackageContent.newBuilder
        .setAlias("alias")
        .setName("name")
        .setDuration(duration)
      val salesmanProductPrice = withSalesmanPackageContent(salesmanPackageContent)

      val publicPackageContent = publicModel.PackageContent.newBuilder
        .setAlias("alias")
        .setName("name")
        .setDuration(duration)
      val publicProductPrice = withPublicPackageContent(publicPackageContent)

      toPublicModel(salesmanProductPrice) shouldBe publicProductPrice
      salesmanProductPrice.toByteArray shouldBe toPublicModel(salesmanProductPrice).toByteArray
    }
  }
}

object SalesmanToPublicModelConverterSpec {

  object SalesmanModelHelper {
    import salesmanModel._

    def withSalesmanRestoreUserQuota(restoreUserQuota: PriceModifier.RestoreUserQuota.Builder) = {
      val priceModifier = PriceModifier.newBuilder.setRestoreQuota(restoreUserQuota)
      withSalesmanPriceModifier(priceModifier)
    }

    def withSalesmanRemoveUserQuota(removeUserQuota: PriceModifier.RemoveUserQuota.Builder) = {
      val priceModifier = PriceModifier.newBuilder.setRemoveQuota(removeUserQuota)
      withSalesmanPriceModifier(priceModifier)
    }

    def withSalesmanPackageContent(packageContent: PackageContent.Builder) = {
      val productPriceInfo = ProductPriceInfo.newBuilder.addAllPackageContent(List(packageContent.build).asJava)
      withSalesmanProductPriceInfo(productPriceInfo)
    }

    def withSalesmanProductPriceInfo(productPriceInfo: ProductPriceInfo.Builder) = {
      ProductPrice.newBuilder.setProductPriceInfo(productPriceInfo).build
    }

    def withSalesmanFeatureBundleParameters(bundleParameters: FeaturePayload.BundleParameters.Builder) = {
      val featurePayload = FeaturePayload.newBuilder.setBundleParameters(bundleParameters)
      withSalesmanFeaturePayload(featurePayload)
    }

    def withSalesmanFeatureDiscountType(discountType: FeatureDiscount.DiscountType) = {
      val featureDiscount = FeatureDiscount.newBuilder.setType(discountType)
      withSalesmanFeatureDiscount(featureDiscount)
    }

    def withSalesmanFeatureDiscount(featureDiscount: FeatureDiscount.Builder) = {
      val featurePayload = FeaturePayload.newBuilder.setDiscount(featureDiscount)
      withSalesmanFeaturePayload(featurePayload)
    }

    def withSalesmanFeatureConstainsts(constraints: FeaturePayload.Constraints.Builder) = {
      val featurePayload = FeaturePayload.newBuilder.setConstraints(constraints)
      withSalesmanFeaturePayload(featurePayload)
    }

    def withSalesmanFeatureType(featureType: FeatureType) = {
      val featurePayload = FeaturePayload.newBuilder.setType(featureType)
      withSalesmanFeaturePayload(featurePayload)
    }

    def withSalesmanFeatureUnit(featureUnit: FeatureUnit) = {
      val featurePayload = FeaturePayload.newBuilder.setUnit(featureUnit)
      withSalesmanFeaturePayload(featurePayload)
    }

    def withSalesmanFeaturePayload(featurePayload: FeaturePayload.Builder) = {
      val feature = PriceModifier.Feature.newBuilder.setPayload(featurePayload)
      withSalesmanFeature(feature)
    }

    def withSalesmanFeature(feature: PriceModifier.Feature.Builder) = {
      val priceModifier = PriceModifier.newBuilder.setPromocoderFeature(feature)
      withSalesmanPriceModifier(priceModifier)
    }

    def withSalesmanPeriodicalDiscount(
        periodicalDiscount: PriceModifier.PeriodicalDiscount.Builder
    ) = {
      val priceModifier = PriceModifier.newBuilder.setDiscount(periodicalDiscount)
      withSalesmanPriceModifier(priceModifier)
    }

    def withSalesmanProlongInterval(prolongInterval: PriceModifier.ProlongInterval.Builder) = {
      val priceModifier = PriceModifier.newBuilder.setProlongInterval(prolongInterval)
      withSalesmanPriceModifier(priceModifier)
    }

    def withSalesmanPriceModifier(priceModifier: PriceModifier.Builder) = {
      val price = Price.newBuilder.setModifier(priceModifier)
      ProductPrice.newBuilder.setPrice(price).build
    }
  }

  object PublicModelHelper {
    import publicModel._

    def withPublicRestoreUserQuota(restoreUserQuota: PriceModifier.RestoreUserQuota.Builder) = {
      val priceModifier = PriceModifier.newBuilder.setRestoreQuota(restoreUserQuota)
      withPublicPriceModifier(priceModifier)
    }

    def withPublicRemoveUserQuota(removeUserQuota: PriceModifier.RemoveUserQuota.Builder) = {
      val priceModifier = PriceModifier.newBuilder.setRemoveQuota(removeUserQuota)
      withPublicPriceModifier(priceModifier)
    }

    def withPublicPackageContent(packageContent: PackageContent.Builder) = {
      val productPriceInfo = ProductPriceInfo.newBuilder.addAllPackageContent(List(packageContent.build).asJava)
      withPublicProductPriceInfo(productPriceInfo)
    }

    def withPublicProductPriceInfo(productPriceInfo: ProductPriceInfo.Builder) = {
      ProductPrice.newBuilder.setProductPriceInfo(productPriceInfo).build
    }

    def withPublicFeatureBundleParameters(bundleParameters: FeaturePayload.BundleParameters.Builder) = {
      val featurePayload = FeaturePayload.newBuilder.setBundleParameters(bundleParameters)
      withPublicFeaturePayload(featurePayload)
    }

    def withPublicFeatureDiscountType(discountType: FeatureDiscount.DiscountType) = {
      val featureDiscount = FeatureDiscount.newBuilder.setType(discountType)
      withPublicFeatureDiscount(featureDiscount)
    }

    def withPublicFeatureDiscount(featureDiscount: FeatureDiscount.Builder) = {
      val featurePayload = FeaturePayload.newBuilder.setDiscount(featureDiscount)
      withPublicFeaturePayload(featurePayload)
    }

    def withPublicFeatureConstainsts(constraints: FeaturePayload.Constraints.Builder) = {
      val featurePayload = FeaturePayload.newBuilder.setConstraints(constraints)
      withPublicFeaturePayload(featurePayload)
    }

    def withPublicFeatureType(featureType: FeatureType) = {
      val featurePayload = publicModel.FeaturePayload.newBuilder.setType(featureType)
      withPublicFeaturePayload(featurePayload)
    }

    def withPublicFeatureUnit(featureUnit: FeatureUnit) = {
      val featurePayload = FeaturePayload.newBuilder.setUnit(featureUnit)
      withPublicFeaturePayload(featurePayload)
    }

    def withPublicFeaturePayload(featurePayload: FeaturePayload.Builder) = {
      val feature = PriceModifier.Feature.newBuilder.setPayload(featurePayload)
      withPublicFeature(feature)
    }

    def withPublicFeature(feature: PriceModifier.Feature.Builder) = {
      val priceModifier = PriceModifier.newBuilder.setPromocoderFeature(feature)
      withPublicPriceModifier(priceModifier)
    }

    def withPublicPeriodicalDiscount(periodicalDiscount: PriceModifier.PeriodicalDiscount.Builder) = {
      val priceModifier = PriceModifier.newBuilder.setDiscount(periodicalDiscount)
      withPublicPriceModifier(priceModifier)
    }

    def withPublicProlongInterval(prolongInterval: PriceModifier.ProlongInterval.Builder) = {
      val priceModifier = PriceModifier.newBuilder.setProlongInterval(prolongInterval)
      withPublicPriceModifier(priceModifier)
    }

    def withPublicPriceModifier(priceModifier: PriceModifier.Builder) = {
      val price = Price.newBuilder.setModifier(priceModifier)
      ProductPrice.newBuilder.setPrice(price).build
    }
  }

}
