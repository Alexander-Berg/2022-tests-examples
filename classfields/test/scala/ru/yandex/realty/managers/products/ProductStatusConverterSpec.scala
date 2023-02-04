package ru.yandex.realty.managers.products

import com.google.protobuf.Timestamp
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.managers.products.status.ProductStatusUnifier
import ru.yandex.realty.proto.products.Products.AggregatedProductStatus.StatusCase
import ru.yandex.realty.proto.seller.{ProductTypes, PurchaseStatus}
import ru.yandex.realty.util.protobuf.BasicEnumProtoFormats.ProductTypeFormat

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class ProductStatusConverterSpec extends BaseProductUnifierSpec {

  "ProductStatusConverter" should {
    "Set all information about active product" in {
      val offerId = "123"
      val ptType = ProductTypes.PRODUCT_TYPE_PROMOTION
      val activeProduct = createActiveProduct(offerId, ptType)
      val timestampBefore = currentTimeSeconds
      val result = ProductStatusUnifier.buildAggregatedProductStatusPerType(offerId, List(activeProduct))
      val timestampAfter = currentTimeSeconds
      result.contains(ptType) should be(true)
      result.get(ptType).foreach { r =>
        r.getStatusCase should be(StatusCase.ACTIVE_STATUS)
        r.getActiveStatus.getEndTime should be(activeProduct.getProduct.getEndTime)
        val secondsUntilExpiryActual = r.getActiveStatus.getSecondsUntilExpiry.getValue.longValue()
        val secondsUntilExpiryMin = r.getActiveStatus.getEndTime.getSeconds - timestampAfter
        val secondsUntilExpiryMax = r.getActiveStatus.getEndTime.getSeconds - timestampBefore
        inRange(secondsUntilExpiryActual, (secondsUntilExpiryMin, secondsUntilExpiryMax)) should be(true)
      }
    }

    "Combination of information active and pending products" in {
      val offerId = "123"
      val ptType = ProductTypes.PRODUCT_TYPE_PROMOTION
      val activeProduct = createActiveProduct(offerId, ptType)
      val purchaseStatus1 = PurchaseStatus.PURCHASE_STATUS_WAIT_FOR_PAYMENT
      val pending1 = createPendingProduct(offerId, ptType, purchaseStatus1, activeProduct.getProduct.getEndTime)
      val purchaseStatus2 = PurchaseStatus.PURCHASE_STATUS_PAID
      val pending2 = createPendingProduct(offerId, ptType, purchaseStatus2, pending1.getProduct.getEndTime)
      val productsList = List(activeProduct, pending1, pending2)
      val timestampBefore = currentTimeSeconds
      val result = ProductStatusUnifier.buildAggregatedProductStatusPerType(offerId, productsList)
      val timestampAfter = currentTimeSeconds

      result.contains(ptType) should be(true)
      result.get(ptType).foreach { r =>
        r.getStatusCase should be(StatusCase.ACTIVE_STATUS)
        r.getActiveStatus.getEndTime should be(pending2.getProduct.getEndTime)
        val secondsUntilExpiryActual = r.getActiveStatus.getSecondsUntilExpiry.getValue.longValue()
        val secondsUntilExpiryMin = r.getActiveStatus.getEndTime.getSeconds - timestampAfter
        val secondsUntilExpiryMax = r.getActiveStatus.getEndTime.getSeconds - timestampBefore
        inRange(secondsUntilExpiryActual, (secondsUntilExpiryMin, secondsUntilExpiryMax)) should be(true)

        r.hasPendingPurchaseInfo should be(true)
        r.getPendingPurchaseInfo.getWaitingForActivationList.size() should be(1)
        val waitingForActivation = r.getPendingPurchaseInfo.getWaitingForActivationList.get(0)
        waitingForActivation.getPurchaseId should be(pending2.getPurchase.getPurchaseId)
        r.getPendingPurchaseInfo.getWaitingForPaymentList.size() should be(1)
        val waitingForPayment = r.getPendingPurchaseInfo.getWaitingForPaymentList.get(0)
        waitingForPayment.getPurchaseId should be(pending1.getPurchase.getPurchaseId)
      }
    }

    "Combination of several pending products" in {
      val offerId = "123"
      val ptType = ProductTypes.PRODUCT_TYPE_PROMOTION
      val startTime1 = Timestamp.newBuilder().setSeconds(currentTimeSeconds).build()
      val purchaseStatus1 = PurchaseStatus.PURCHASE_STATUS_WAIT_FOR_PAYMENT
      val pending1 = createPendingProduct(offerId, ptType, purchaseStatus1, startTime1)
      val purchaseStatus2 = PurchaseStatus.PURCHASE_STATUS_PAID
      val pending2 = createPendingProduct(offerId, ptType, purchaseStatus2, pending1.getProduct.getEndTime)
      val productsList = List(pending1, pending2)
      val result = ProductStatusUnifier.buildAggregatedProductStatusPerType(offerId, productsList)

      result.contains(ptType) should be(true)
      result.get(ptType).foreach { r =>
        r.getStatusCase should be(StatusCase.INACTIVE_STATUS)

        r.hasPendingPurchaseInfo should be(true)
        r.getPendingPurchaseInfo.getWaitingForActivationList.size() should be(1)
        val waitingForActivation = r.getPendingPurchaseInfo.getWaitingForActivationList.get(0)
        waitingForActivation.getPurchaseId should be(pending2.getPurchase.getPurchaseId)
        r.getPendingPurchaseInfo.getWaitingForPaymentList.size() should be(1)
        val waitingForPayment = r.getPendingPurchaseInfo.getWaitingForPaymentList.get(0)
        waitingForPayment.getPurchaseId should be(pending1.getPurchase.getPurchaseId)
      }
    }

    "Combination of pending and cancelled products" in {
      val offerId = "123"
      val ptType = ProductTypes.PRODUCT_TYPE_PROMOTION
      val startTime = Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000).build()
      val pending1 = createPendingProduct(offerId, ptType, PurchaseStatus.PURCHASE_STATUS_PAID, startTime)
      val cancelled = createCancelledProduct(offerId, ptType, "someId", startTime)
      val productsLists = List(cancelled, pending1)
      val result = ProductStatusUnifier.buildAggregatedProductStatusPerType(offerId, productsLists)
      result.contains(ptType) should be(true)
      result.get(ptType).foreach { r =>
        r.getStatusCase should be(StatusCase.INACTIVE_STATUS)

        r.hasPendingPurchaseInfo should be(true)
        r.getPendingPurchaseInfo.getWaitingForActivationList.size() should be(1)
        val waitingForActivation = r.getPendingPurchaseInfo.getWaitingForActivationList.get(0)
        waitingForActivation.getPurchaseId should be(pending1.getPurchase.getPurchaseId)
        r.getPendingPurchaseInfo.getWaitingForPaymentList.size() should be(0)
      }
    }

    "Two cancelled products" in {
      val offerId = "123"
      val ptType = ProductTypes.PRODUCT_TYPE_PROMOTION
      val startTime1 = Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000 - oneDaySeconds).build()
      val startTime2 = Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000).build()
      val cancelled1 = createCancelledProduct(offerId, ptType, "purchaseIdCancelled1", startTime1)
      val cancelled2 = createCancelledProduct(offerId, ptType, "purchaseIdCancelled2", startTime2)
      val productsList = List(cancelled1, cancelled2)
      val result2 = ProductStatusUnifier.buildAggregatedProductStatusPerType(offerId, productsList)
      result2.contains(ptType) should be(true)
      result2.get(ptType).foreach { r =>
        r.getStatusCase should be(StatusCase.INACTIVE_STATUS)
      }
    }
  }
}
