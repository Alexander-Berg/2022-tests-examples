package ru.yandex.realty.managers.products

import com.google.protobuf.Timestamp
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.proto.seller.{ProductTypes, PurchaseStatus, PurchasedProductStatus}
import ru.yandex.realty.seller.proto.api.products.PurchasePayment.{ManualPayment, RenewalPayment}
import ru.yandex.realty.seller.proto.api.products.{Product, PurchasePayment}
import ru.yandex.realty.seller.proto.api.purchase.PurchaseSource.ManualPurchaseSource
import ru.yandex.realty.seller.proto.api.purchase.{OfferTarget, PurchaseProduct, PurchaseSource, PurchaseTarget}
import ru.yandex.realty.util.protobuf.ProtobufConversions

class BaseProductUnifierSpec extends AsyncSpecBase with ProtobufConversions {

  protected val oneDaySeconds: Int = 24 * 60 * 60
  protected val halfDaySeconds: Int = 12 * 60 * 60

  protected def createActiveProduct(offerId: String, ptType: ProductTypes): Product = {
    val startTime = currentTimeSeconds - oneDaySeconds
    val endTime = currentTimeSeconds + oneDaySeconds
    val p = PurchaseProduct
      .newBuilder()
      .setStatus(PurchasedProductStatus.PURCHASED_PRODUCT_STATUS_ACTIVE)
      .setStartTime(buildTimestamp(startTime))
      .setEndTime(buildTimestamp(endTime))
      .setType(ptType)
      .setTarget(PurchaseTarget.newBuilder().setOfferTarget(OfferTarget.newBuilder().setOfferId(offerId)))
      .setSource(PurchaseSource.newBuilder().setManual(ManualPurchaseSource.getDefaultInstance))
      .build()
    val purchase = PurchasePayment
      .newBuilder()
      .setStatus(PurchaseStatus.PURCHASE_STATUS_CLOSED)
      .setPurchaseId("activePurchaseId")
      .setManual(ManualPayment.getDefaultInstance)
      .build()
    Product
      .newBuilder()
      .setProduct(p)
      .setPurchase(purchase)
      .build()
  }

  protected def createPendingProduct(
    offerId: String,
    ptType: ProductTypes,
    purchaseStatus: PurchaseStatus,
    startTime: Timestamp
  ): Product =
    createPendingProduct(offerId, ptType, purchaseStatus, Some(startTime), isRenewalPayment = false, None)

  protected def createRenewingProduct(
    offerId: String,
    ptType: ProductTypes,
    purchaseStatus: PurchaseStatus,
    lastRenewalAttemptTime: Option[Timestamp] = None
  ): Product =
    createPendingProduct(offerId, ptType, purchaseStatus, None, isRenewalPayment = true, lastRenewalAttemptTime)

  private def createPendingProduct(
    offerId: String,
    ptType: ProductTypes,
    purchaseStatus: PurchaseStatus,
    startTimeOpt: Option[Timestamp],
    isRenewalPayment: Boolean,
    lastRenewalAttemptTime: Option[Timestamp]
  ): Product = {
    val createTime = currentTimeSeconds - oneDaySeconds
    val p = PurchaseProduct
      .newBuilder()
      .setStatus(PurchasedProductStatus.PURCHASED_PRODUCT_STATUS_PENDING)
      .setType(ptType)
      .setTarget(PurchaseTarget.newBuilder().setOfferTarget(OfferTarget.newBuilder().setOfferId(offerId)))
      .setSource(PurchaseSource.newBuilder().setManual(ManualPurchaseSource.getDefaultInstance))
      .setCreateTime(buildTimestamp(createTime))
    startTimeOpt.foreach { startTime =>
      val endTime = startTime.getSeconds + oneDaySeconds
      p.setStartTime(startTime)
        .setEndTime(buildTimestamp(endTime))
    }

    val purchase = PurchasePayment
      .newBuilder()
      .setStatus(purchaseStatus)
      .setPurchaseId("pendingPurchaseId")

    if (isRenewalPayment) {
      val renewal = RenewalPayment.newBuilder()
      lastRenewalAttemptTime.foreach(renewal.setLastRenewalAttemptTime)
      purchase.setRenewal(renewal)
    } else {
      purchase.setManual(ManualPayment.getDefaultInstance)
    }

    Product
      .newBuilder()
      .setProduct(p)
      .setPurchase(purchase)
      .build()
  }

  protected def createCancelledProduct(
    offerId: String,
    ptType: ProductTypes,
    purchaseId: String,
    startTime: Timestamp
  ): Product = {
    val p = PurchaseProduct
      .newBuilder()
      .setStatus(PurchasedProductStatus.PURCHASED_PRODUCT_STATUS_CANCELLED)
      .setType(ptType)
      .setTarget(PurchaseTarget.newBuilder().setOfferTarget(OfferTarget.newBuilder().setOfferId(offerId)))
      .setStartTime(startTime)
      .build()
    val purchase = PurchasePayment
      .newBuilder()
      .setStatus(PurchaseStatus.PURCHASE_STATUS_CANCELLED)
      .setPurchaseId(purchaseId)
      .setManual(ManualPayment.getDefaultInstance)
      .build()
    Product
      .newBuilder()
      .setProduct(p)
      .setPurchase(purchase)
      .build()
  }

  protected def currentTimeSeconds: Long = System.currentTimeMillis() / 1000

  protected def buildTimestamp(seconds: Long): Timestamp =
    Timestamp.newBuilder().setSeconds(seconds).build()

  protected def inRange(actual: Long, range: (Long, Long)): Boolean = {
    val (min, max) = range
    actual >= min && actual <= max
  }
}
