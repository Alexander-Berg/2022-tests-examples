package ru.yandex.realty.seller.backend

import java.net.InetAddress
import java.util.NoSuchElementException

import org.junit.runner.RunWith
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.model.exception.NeedAuthenticationException
import ru.yandex.realty.model.gen.ProtobufMessageGenerators
import ru.yandex.realty.seller.dao.{PaymentDetailsDao, PurchaseDao}
import ru.yandex.realty.seller.model.PurchaseWithProducts
import ru.yandex.realty.seller.model.banker.{PaymentDetails, PaymentResultWrapper, StoredBankerConfirmation}
import ru.yandex.realty.seller.model.product.{PriceContext, PurchasedProduct}
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.purchase.{PurchaseStatuses, PurchaseUpdate}
import ru.yandex.realty.seller.proto.api.payment.PaymentSuccess.NoConfirmation
import ru.yandex.realty.seller.proto.api.payment._
import ru.yandex.realty.seller.service.{BankerService, PromocoderManager, RenewalByDefaultManager}
import ru.yandex.realty.seller.service.builders.PurchaseWithProductsBuilder
import ru.yandex.realty.vos.model.user.User
import ru.yandex.vertis.scalamock.util.RichFutureCallHandler
import ru.yandex.vertis.protobuf.ProtoInstanceProvider
import ru.yandex.realty.tracing.Traced

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class PurchaseManagerMakePaymentSpec
  extends AsyncSpecBase
  with PropertyChecks
  with SellerModelGenerators
  with RequestAware
  with OneInstancePerTest
  with ProtoInstanceProvider
  with ProtobufMessageGenerators {

  private val purchaseWithProductsBuilder = mock[PurchaseWithProductsBuilder]
  private val purchaseDao = mock[PurchaseDao]
  private val paymentDetailsDao = mock[PaymentDetailsDao]
  private val bankerService = mock[BankerService]
  private val vosClient = mock[VosClientNG]
  private val promocoderManager = mock[PromocoderManager]
  private val renewalByDefaultManager = mock[RenewalByDefaultManager]

  val purchaseManager = new PurchaseManager(
    purchaseWithProductsBuilder,
    purchaseDao,
    paymentDetailsDao,
    bankerService,
    vosClient,
    promocoderManager,
    renewalByDefaultManager
  )

  "PurchasesManager.makePayment" should {

    "Throws exception if user is unknown " in {
      val userRef = webUserGen.next
      interceptCause[NeedAuthenticationException] {
        purchaseManager.payment(userRef, ClientPaymentInfo.getDefaultInstance, None).futureValue
      }
    }

    "Throws Exception if VOS returned exception" in {
      val userRef = passportUserGen.next
      (vosClient
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(userRef.uid, *, *)
        .throwingF(new IllegalArgumentException)

      interceptCause[IllegalArgumentException] {
        purchaseManager.payment(userRef, ClientPaymentInfo.getDefaultInstance, None).futureValue
      }
    }

    "Throws Exception if user was not found in VOS" in {
      val userRef = passportUserGen.next
      (vosClient
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(userRef.uid, *, *)
        .returningF(None)

      interceptCause[NoSuchElementException] {
        purchaseManager.payment(userRef, ClientPaymentInfo.getDefaultInstance, None).futureValue
      }
    }

    "Throws Exception if we cannot save PurchaseWithProducts to mysql" in {
      val vosUserGenerator = generate[User]()
      val purchaseId = readableString(8, 8).next
      val userRef = passportUserGen.next
      (vosClient
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(userRef.uid, *, *)
        .returningF(Some(vosUserGenerator.next))

      (purchaseDao
        .getWithProducts(_: String))
        .expects(purchaseId)
        .throwingF(new Exception("database exception"))

      interceptCause[Exception] {
        val request = ClientPaymentInfo.newBuilder().setPurchaseId(purchaseId).build()
        purchaseManager.payment(userRef, request, None).futureValue
      }
    }

    "Successfully make payment using wallet" in {
      val userRef = passportUserGen.next
      val purchaseWithProducts = newPurchaseWithProductsGen.next
      val purchaseId = purchaseWithProducts.purchase.id
      val request = ClientPaymentInfo
        .newBuilder()
        .setPurchaseId(purchaseId)
        .setWallet(WalletPaymentInfo.getDefaultInstance)
        .build()

      val vosUser = generate[User]().next
      (vosClient
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(userRef.uid, *, *)
        .returningF(Some(vosUser))

      (purchaseDao
        .getWithProducts(_: String))
        .expects(purchaseId)
        .returningF(purchaseWithProducts)

      val result = PaymentResult
        .newBuilder()
        .setPaymentSuccess(
          PaymentSuccess
            .newBuilder()
            .setNoConfirmation(NoConfirmation.getDefaultInstance)
        )
        .build()
      val resultWrapper = PaymentResultWrapper(result, Some("txn-00001"))
      (bankerService
        .makePayment(_: User, _: ClientPaymentInfo, _: PurchaseWithProducts, _: PriceContext, _: Option[InetAddress])(
          _: Traced
        ))
        .expects(vosUser, request, purchaseWithProducts, purchaseWithProducts.fullPrice, None, *)
        .returningF(resultWrapper)

      (purchaseDao
        .updatePurchases(_: Iterable[PurchaseUpdate]))
        .expects(where { patch: Iterable[PurchaseUpdate] =>
          val p = patch.seq.head.patch
          p.status == PurchaseStatuses.WaitForPayment &&
          p.deliveryStatus == purchaseWithProducts.purchase.deliveryStatus &&
          p.basis == purchaseWithProducts.purchase.basis &&
          p.visitTime.nonEmpty
        })
        .returningF(())

      (paymentDetailsDao
        .create(_: PaymentDetails))
        .expects(where { details: PaymentDetails =>
          details.purchaseId == purchaseId &&
          details.confirmation == StoredBankerConfirmation.NoConfirmation
        })
        .returningF(())

      mockPromocoderManager(purchaseWithProducts)

      purchaseManager.payment(userRef, request, None).futureValue shouldBe result
    }

    "Successfully make payment using external system" in {
      val userRef = passportUserGen.next
      val purchaseWithProducts = newPurchaseWithProductsGen.next
      val purchaseId = purchaseWithProducts.purchase.id
      val paymentMethod = generate[BankerPaymentMethod]().next
      val request = ClientPaymentInfo
        .newBuilder()
        .setPurchaseId(purchaseId)
        .setExternalSystem(BankerPaymentInfo.newBuilder().setMethod(paymentMethod))
        .build()

      val vosUser = generate[User]().next
      (vosClient
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(userRef.uid, *, *)
        .returningF(Some(vosUser))

      (purchaseDao
        .getWithProducts(_: String))
        .expects(purchaseId)
        .returningF(purchaseWithProducts)

      val result = PaymentResult
        .newBuilder()
        .setPaymentSuccess(
          PaymentSuccess
            .newBuilder()
            .setNoConfirmation(NoConfirmation.getDefaultInstance)
        )
        .build()
      val resultWrapper = PaymentResultWrapper(result, Some("txn-00002"))
      (bankerService
        .makePayment(_: User, _: ClientPaymentInfo, _: PurchaseWithProducts, _: PriceContext, _: Option[InetAddress])(
          _: Traced
        ))
        .expects(vosUser, request, purchaseWithProducts, purchaseWithProducts.fullPrice, None, *)
        .returningF(resultWrapper)

      (purchaseDao
        .updatePurchases(_: Iterable[PurchaseUpdate]))
        .expects(where { patch: Iterable[PurchaseUpdate] =>
          val p = patch.seq.head.patch
          p.status == PurchaseStatuses.WaitForPayment &&
          p.deliveryStatus == purchaseWithProducts.purchase.deliveryStatus &&
          p.basis == purchaseWithProducts.purchase.basis &&
          p.visitTime.nonEmpty
        })
        .returningF(())

      (paymentDetailsDao
        .create(_: PaymentDetails))
        .expects(where { details: PaymentDetails =>
          details.purchaseId == purchaseId &&
          details.confirmation == StoredBankerConfirmation.NoConfirmation
        })
        .returningF(())

      mockPromocoderManager(purchaseWithProducts)

      purchaseManager.payment(userRef, request, None).futureValue shouldBe result
    }
  }

  private def mockPromocoderManager(purchase: PurchaseWithProducts) {
    (promocoderManager
      .ensurePromoFeaturesActuality(_: PurchaseWithProducts)(_: Traced))
      .expects(purchase, *)
      .returningF(())

    (promocoderManager
      .decrementPromoFeatures(_: Iterable[PurchasedProduct])(_: Traced))
      .expects(purchase.products, *)
      .returningF(())
  }
}
