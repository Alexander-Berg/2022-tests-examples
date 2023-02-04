package ru.yandex.realty.managers.products

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.proto.seller.{ProductRenewalStatus, ProductTypes, PurchaseStatus}
import ru.yandex.realty.proto.seller.ProductRenewalStatus._
import ru.yandex.realty.proto.seller.ProductTypes._
import ru.yandex.realty.seller.proto.api.renewals.internal.{
  InternalOfferRenewalInfo,
  InternalRenewalInfo,
  InternalRenewalState
}
import ru.yandex.realty.model.message.ExtDataSchema.ProductDictionaryRecord.ProductType
import ru.yandex.realty.model.message.ExtDataSchema.ProductDictionaryRecord.ProductType._
import ru.yandex.realty.seller.proto.api.products
import ru.yandex.realty.seller.proto.api.purchase.PurchaseSource
import ru.yandex.realty.seller.proto.api.purchase.PurchaseSource.PackagePurchaseSource
import ru.yandex.realty.seller.proto.api.renewals.RenewalInfo

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ProductRenewalUnifierSpec extends BaseProductUnifierSpec {

  private val OfferId = "6527583134565214356"
  private val AnotherOfferId = "23465472457625125761"

  private val AvailableProductTypes = Seq(
    PRODUCT_TYPE_RAISING,
    PRODUCT_TYPE_PREMIUM,
    PRODUCT_TYPE_PROMOTION,
    PRODUCT_TYPE_PACKAGE_TURBO
  )

  "ProductRenewalUnifier" should {
    "return Unavailable renewal status if no renewals turned on" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq.empty)
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, Seq.empty)
      assertStatuses(statuses, Map.empty)
    }

    "return Inactive promotion renewal status" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq.empty)
      val products = Seq(createActiveProduct(OfferId, PRODUCT_TYPE_PROMOTION))
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(statuses, Map(PROMOTION -> PRODUCT_RENEWAL_STATUS_INACTIVE))
    }

    "return Active premium renewal status" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq(PRODUCT_TYPE_PREMIUM))
      val products = Seq(createActiveProduct(OfferId, PRODUCT_TYPE_PREMIUM))
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(statuses, Map(PREMIUM -> PRODUCT_RENEWAL_STATUS_ACTIVE))
    }

    "return DisabledInactive promotion renewal status when turbo renewal is turned on" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq(PRODUCT_TYPE_PACKAGE_TURBO))
      val products = Seq(
        createActiveProduct(OfferId, PRODUCT_TYPE_PROMOTION),
        createActiveProduct(OfferId, PRODUCT_TYPE_PACKAGE_TURBO)
      )
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(
        statuses,
        Map(PROMOTION -> PRODUCT_RENEWAL_STATUS_DISABLED_INACTIVE, PACKAGE_TURBO -> PRODUCT_RENEWAL_STATUS_ACTIVE)
      )
    }

    "return DisabledActive premium renewal status when turbo renewal is turned on" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq(PRODUCT_TYPE_PREMIUM, PRODUCT_TYPE_PACKAGE_TURBO))
      val products = Seq(
        createActiveProduct(OfferId, PRODUCT_TYPE_PREMIUM),
        createActiveProduct(OfferId, PRODUCT_TYPE_PACKAGE_TURBO)
      )
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(
        statuses,
        Map(PREMIUM -> PRODUCT_RENEWAL_STATUS_DISABLED_ACTIVE, PACKAGE_TURBO -> PRODUCT_RENEWAL_STATUS_ACTIVE)
      )
    }

    "return Inactive promotion renewal status when turbo renewal is turned off" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq.empty)
      val products = Seq(
        createActiveProduct(OfferId, PRODUCT_TYPE_PROMOTION),
        createActiveProduct(OfferId, PRODUCT_TYPE_PACKAGE_TURBO)
      )
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(
        statuses,
        Map(PROMOTION -> PRODUCT_RENEWAL_STATUS_INACTIVE, PACKAGE_TURBO -> PRODUCT_RENEWAL_STATUS_INACTIVE)
      )
    }

    "return Active premium renewal status when turbo renewal is turned off" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq(PRODUCT_TYPE_PREMIUM))
      val products = Seq(
        createActiveProduct(OfferId, PRODUCT_TYPE_PREMIUM),
        createActiveProduct(OfferId, PRODUCT_TYPE_PACKAGE_TURBO)
      )
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(
        statuses,
        Map(PREMIUM -> PRODUCT_RENEWAL_STATUS_ACTIVE, PACKAGE_TURBO -> PRODUCT_RENEWAL_STATUS_INACTIVE)
      )
    }

    "return DisabledInactive raising renewal status when turbo renewal is turned on" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq(PRODUCT_TYPE_PACKAGE_TURBO))
      val products = Seq(
        createActiveProduct(OfferId, PRODUCT_TYPE_RAISING),
        createActiveProduct(OfferId, PRODUCT_TYPE_PACKAGE_TURBO)
      )
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(
        statuses,
        Map(RAISING -> PRODUCT_RENEWAL_STATUS_DISABLED_INACTIVE, PACKAGE_TURBO -> PRODUCT_RENEWAL_STATUS_ACTIVE)
      )
    }

    "return Active raising renewal status when turbo renewal is turned on" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq(PRODUCT_TYPE_RAISING, PRODUCT_TYPE_PACKAGE_TURBO))
      val products = Seq(
        createActiveProduct(OfferId, PRODUCT_TYPE_RAISING),
        createActiveProduct(OfferId, PRODUCT_TYPE_PACKAGE_TURBO)
      )
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(
        statuses,
        Map(RAISING -> PRODUCT_RENEWAL_STATUS_DISABLED_ACTIVE, PACKAGE_TURBO -> PRODUCT_RENEWAL_STATUS_ACTIVE)
      )
    }

    "return Unavailable premium renewal status when premium is in turbo package" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq(PRODUCT_TYPE_PACKAGE_TURBO))
      val products = Seq(
        inPackage(createActiveProduct(OfferId, PRODUCT_TYPE_PREMIUM)),
        createActiveProduct(OfferId, PRODUCT_TYPE_PACKAGE_TURBO)
      )
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(statuses, Map(PACKAGE_TURBO -> PRODUCT_RENEWAL_STATUS_ACTIVE))
    }

    "return DisabledInactive premium renewal status when premium was bought before" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq(PRODUCT_TYPE_PACKAGE_TURBO))
      val products = Seq(
        inPackage(createActiveProduct(OfferId, PRODUCT_TYPE_PREMIUM), withPreviousBought = true),
        createActiveProduct(OfferId, PRODUCT_TYPE_PACKAGE_TURBO)
      )
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(
        statuses,
        Map(PREMIUM -> PRODUCT_RENEWAL_STATUS_DISABLED_INACTIVE, PACKAGE_TURBO -> PRODUCT_RENEWAL_STATUS_ACTIVE)
      )
    }

    "return Active renewal statuses for several offers call case" in {
      val firstRenewalInfo = buildRenewalInfo(OfferId, Seq(PRODUCT_TYPE_PREMIUM))
      val secondRenewalInfo = buildRenewalInfo(AnotherOfferId, Seq(PRODUCT_TYPE_PROMOTION))
      val renewalState = InternalRenewalState
        .newBuilder()
        .addOffers(firstRenewalInfo)
        .addOffers(secondRenewalInfo)
        .build()
      val products = Seq(
        createActiveProduct(OfferId, PRODUCT_TYPE_PREMIUM),
        createActiveProduct(AnotherOfferId, PRODUCT_TYPE_PROMOTION)
      )

      val firstOfferStatuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalState, products)
      val secondOfferStatuses = ProductRenewalUnifier.buildRenewalStatuses(AnotherOfferId, renewalState, products)

      assertStatuses(firstOfferStatuses, Map(PREMIUM -> PRODUCT_RENEWAL_STATUS_ACTIVE))
      assertStatuses(secondOfferStatuses, Map(PROMOTION -> PRODUCT_RENEWAL_STATUS_ACTIVE))
    }

    "return In progress renewal status when no renewing products created yet" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq(PRODUCT_TYPE_PREMIUM))
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, Seq.empty)
      assertStatuses(statuses, Map(PREMIUM -> PRODUCT_RENEWAL_STATUS_IN_PROGRESS))
    }

    "return In progress renewal status for purchase in New status without failed attempts" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq(PRODUCT_TYPE_PREMIUM))
      val products = Seq(createRenewingProduct(OfferId, PRODUCT_TYPE_PREMIUM, PurchaseStatus.PURCHASE_STATUS_NEW))
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(statuses, Map(PREMIUM -> PRODUCT_RENEWAL_STATUS_IN_PROGRESS))
    }

    "return In progress renewal status for purchase in Wait for payment status" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq(PRODUCT_TYPE_PREMIUM))
      val products =
        Seq(createRenewingProduct(OfferId, PRODUCT_TYPE_PREMIUM, PurchaseStatus.PURCHASE_STATUS_WAIT_FOR_PAYMENT))
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(statuses, Map(PREMIUM -> PRODUCT_RENEWAL_STATUS_IN_PROGRESS))
    }

    "return In progress renewal status for purchase in Paid status" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq(PRODUCT_TYPE_PREMIUM))
      val products = Seq(createRenewingProduct(OfferId, PRODUCT_TYPE_PREMIUM, PurchaseStatus.PURCHASE_STATUS_PAID))
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(statuses, Map(PREMIUM -> PRODUCT_RENEWAL_STATUS_IN_PROGRESS))
    }

    "return Warning renewal status for purchase in New status with failed attempts" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq(PRODUCT_TYPE_PREMIUM))
      val products = Seq(
        createRenewingProduct(
          OfferId,
          PRODUCT_TYPE_PREMIUM,
          PurchaseStatus.PURCHASE_STATUS_NEW,
          Some(buildTimestamp(currentTimeSeconds - halfDaySeconds))
        )
      )
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(statuses, Map(PREMIUM -> PRODUCT_RENEWAL_STATUS_WARNING))
    }

    "return Error renewal status when renewal was failed half day ago" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq.empty)
      val products = Seq(
        createRenewingProduct(
          OfferId,
          PRODUCT_TYPE_PREMIUM,
          PurchaseStatus.PURCHASE_STATUS_CANCELLED,
          Some(buildTimestamp(currentTimeSeconds - halfDaySeconds))
        )
      )
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(statuses, Map(PREMIUM -> PRODUCT_RENEWAL_STATUS_ERROR))
    }

    "return Unavailable renewal status when renewal was failed more then day ago" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq.empty)
      val products = Seq(
        createRenewingProduct(
          OfferId,
          PRODUCT_TYPE_PREMIUM,
          PurchaseStatus.PURCHASE_STATUS_CANCELLED,
          Some(buildTimestamp(currentTimeSeconds - oneDaySeconds - halfDaySeconds))
        )
      )
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(statuses, Map(PREMIUM -> PRODUCT_RENEWAL_STATUS_UNAVAILABLE))
    }

    "return Unavailable renewal status for purchase in Expired status" in {
      val renewalInfo = buildRenewalInfo(OfferId, Seq.empty)
      val products = Seq(
        createRenewingProduct(
          OfferId,
          PRODUCT_TYPE_PREMIUM,
          PurchaseStatus.PURCHASE_STATUS_EXPIRED,
          Some(buildTimestamp(currentTimeSeconds - halfDaySeconds))
        )
      )
      val statuses = ProductRenewalUnifier.buildRenewalStatuses(OfferId, renewalInfo, products)
      assertStatuses(statuses, Map(PREMIUM -> PRODUCT_RENEWAL_STATUS_UNAVAILABLE))
    }
  }

  private def buildRenewalInfo(offerId: String, activeProductTypes: Seq[ProductTypes]): InternalOfferRenewalInfo = {
    val renewalInfoList = AvailableProductTypes
      .map { pt =>
        InternalRenewalInfo
          .newBuilder()
          .setProductType(pt)
          .setTurnedOn(activeProductTypes.contains(pt))
          .build()
      }

    InternalOfferRenewalInfo
      .newBuilder()
      .setOfferId(offerId)
      .addAllRenewals(renewalInfoList.asJava)
      .build()
  }

  private def inPackage(product: products.Product, withPreviousBought: Boolean = false): products.Product = {
    val builder = product.getProduct.toBuilder
    builder.setSource(PurchaseSource.newBuilder().setPackagePurchase(PackagePurchaseSource.getDefaultInstance))
    if (withPreviousBought) {
      builder.setPreviousBoughtProductId("previousId")
    }

    product.toBuilder
      .setProduct(builder)
      .build()
  }

  private def assertStatuses(
    statusMap: Map[ProductType, RenewalInfo],
    expectedStatusMap: Map[ProductType, ProductRenewalStatus]
  ) {
    statusMap.foreach {
      case (pt, info) => info.getStatus shouldEqual expectedStatusMap.getOrElse(pt, PRODUCT_RENEWAL_STATUS_UNAVAILABLE)
    }
  }
}
