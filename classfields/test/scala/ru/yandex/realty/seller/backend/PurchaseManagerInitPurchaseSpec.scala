package ru.yandex.realty.seller.backend

import java.util.NoSuchElementException

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.model.exception.NeedAuthenticationException
import ru.yandex.realty.model.gen.ProtobufMessageGenerators
import ru.yandex.realty.model.user.UserRef
import ru.yandex.realty.seller.dao.{PaymentDetailsDao, PurchaseDao}
import ru.yandex.realty.seller.exceptions.{NoFoundMethodsInBankerException, NoPriceFoundException}
import ru.yandex.realty.seller.model.PurchaseWithProducts
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product._
import ru.yandex.realty.seller.model.purchase.{Purchase, PurchaseUpdate}
import ru.yandex.realty.seller.proto.api.payment.BankerPaymentMethod
import ru.yandex.realty.seller.proto.api.purchase.{PaymentInfo, PurchaseInitRequest}
import ru.yandex.realty.seller.service.{BankerService, PromocoderManager, RenewalByDefaultManager}
import ru.yandex.realty.seller.service.builders.PurchaseWithProductsBuilder
import ru.yandex.realty.vos.model.user.{ExtendedUserType, User}
import ru.yandex.vertis.protobuf.ProtoInstanceProvider
import ru.yandex.vertis.scalamock.util.RichFutureCallHandler
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.proto.offer.PaymentType

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class PurchaseManagerInitPurchaseSpec
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

  private val vosUserGenerator = generate[User]()
  private def generateUser: User = {
    val user = vosUserGenerator.next
    user.toBuilder
      .setUserInfo {
        user.getUserInfo.toBuilder
          .setPaymentType(PaymentType.PT_NATURAL_PERSON)
      }
      .build()
  }

  "PurchasesManager.initPurchases" should {

    "Throws exception if user is unknown " in {
      val userRef = webUserGen.next
      interceptCause[NeedAuthenticationException] {
        purchaseManager.initPurchases(userRef, PurchaseInitRequest.newBuilder().build()).futureValue
      }
    }

    "Throws Exception if VOS returned exception" in {
      val userRef = passportUserGen.next
      (vosClient
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(userRef.uid, *, *)
        .throwingF(new IllegalArgumentException)

      interceptCause[IllegalArgumentException] {
        purchaseManager.initPurchases(userRef, PurchaseInitRequest.newBuilder().build()).futureValue
      }
    }

    "Throws Exception if user was not found in VOS" in {
      val userRef = passportUserGen.next
      (vosClient
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(userRef.uid, *, *)
        .returningF(None)

      interceptCause[NoSuchElementException] {
        purchaseManager.initPurchases(userRef, PurchaseInitRequest.newBuilder().build()).futureValue
      }
    }

    "Throws Exception if we cannot build PurchaseWithProducts in builder" in {
      val userRef = passportUserGen.next
      (vosClient
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(userRef.uid, *, *)
        .returningF(Some(generateUser))
      (purchaseWithProductsBuilder
        .buildInitialPurchase(_: UserRef, _: PurchaseInitRequest, _: Option[DateTime], _: Option[ExtendedUserType])(
          _: Traced
        ))
        .expects(userRef, PurchaseInitRequest.newBuilder().build(), *, *, *)
        .throwingF(new NoPriceFoundException(userRef, OfferTarget("123"), ProductTypes.Promotion))

      interceptCause[NoPriceFoundException] {
        purchaseManager.initPurchases(userRef, PurchaseInitRequest.newBuilder().build()).futureValue
      }
    }

    "Throws Exception if we cannot save PurchaseWithProducts in mysql" in {
      val purchasePriceGen = buildPurchaseWithOneProduct(Some(PriceContext(2L, 1L, Iterable.empty, Iterable.empty)))
      val userRef = passportUserGen.next
      val pwp = purchasePriceGen.next
      (vosClient
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(userRef.uid, *, *)
        .returningF(Some(generateUser))
      (purchaseWithProductsBuilder
        .buildInitialPurchase(_: UserRef, _: PurchaseInitRequest, _: Option[DateTime], _: Option[ExtendedUserType])(
          _: Traced
        ))
        .expects(userRef, PurchaseInitRequest.newBuilder().build(), *, *, *)
        .returningF(pwp)
      (purchaseDao
        .create(_: Purchase, _: Iterable[PurchasedProduct]))
        .expects(pwp.purchase, pwp.products)
        .throwingF(new Exception("Some exception"))

      interceptCause[Exception] {
        purchaseManager.initPurchases(userRef, PurchaseInitRequest.newBuilder().build()).futureValue
      }
    }

    "Throws Exception if we received exception from Banker" in {
      val purchasePriceGen = buildPurchaseWithOneProduct(Some(PriceContext(2L, 1L, Iterable.empty, Iterable.empty)))
      val userRef = passportUserGen.next
      val vosUser = generateUser
      val p2pWithPrices = purchasePriceGen.next
      (vosClient
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(userRef.uid, *, *)
        .returningF(Some(vosUser))
      (purchaseWithProductsBuilder
        .buildInitialPurchase(_: UserRef, _: PurchaseInitRequest, _: Option[DateTime], _: Option[ExtendedUserType])(
          _: Traced
        ))
        .expects(userRef, PurchaseInitRequest.newBuilder().build(), *, *, *)
        .returningF(p2pWithPrices)
      (purchaseDao
        .create(_: Purchase, _: Iterable[PurchasedProduct]))
        .expects(p2pWithPrices.purchase, p2pWithPrices.products)
        .returningF(())
      (bankerService
        .buildPaymentInfo(_: User, _: PurchaseWithProducts, _: PriceContext)(_: Traced))
        .expects(vosUser, p2pWithPrices, *, *)
        .throwingF(new Exception("Some exception"))

      interceptCause[Exception] {
        purchaseManager.initPurchases(userRef, PurchaseInitRequest.newBuilder().build()).futureValue
      }
    }

    "Build correct response without payment methods if effective cost is 0" in {
      val purchasePriceGen = buildPurchaseWithOneProduct(Some(PriceContext(2L, 0L, Iterable.empty, Iterable.empty)))
      val userRef = passportUserGen.next
      val vosUser = generateUser
      val p2pWithPrices = purchasePriceGen.next
      (vosClient
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(userRef.uid, *, *)
        .returningF(Some(vosUser))
      (purchaseWithProductsBuilder
        .buildInitialPurchase(_: UserRef, _: PurchaseInitRequest, _: Option[DateTime], _: Option[ExtendedUserType])(
          _: Traced
        ))
        .expects(userRef, PurchaseInitRequest.newBuilder().build(), *, *, *)
        .returningF(p2pWithPrices)
      (purchaseDao
        .create(_: Purchase, _: Iterable[PurchasedProduct]))
        .expects(p2pWithPrices.purchase, p2pWithPrices.products)
        .returningF(())
      (bankerService
        .buildPaymentInfo(_: User, _: PurchaseWithProducts, _: PriceContext)(_: Traced))
        .expects(vosUser, p2pWithPrices, *, *)
        .never()

      val result = purchaseManager.initPurchases(userRef, PurchaseInitRequest.newBuilder().build()).futureValue

      result.hasPurchase should be(true)
      result.hasPaymentInfo should be(false)
    }

    "Build correct response with payment methods if effective cost is more then 0" in {
      val paymentMethodGenerator = generate[BankerPaymentMethod]()
      val purchasePriceGen = buildPurchaseWithOneProduct(Some(PriceContext(2L, 2L, Iterable.empty, Iterable.empty)))
      val userRef = passportUserGen.next
      val vosUser = generateUser
      val p2pWithPrices = purchasePriceGen.next
      val paymentMethod = paymentMethodGenerator.next
      val paymentInfo = PaymentInfo.newBuilder().addMethods(paymentMethod).build()
      (vosClient
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(userRef.uid, *, *)
        .returningF(Some(vosUser))
      (purchaseWithProductsBuilder
        .buildInitialPurchase(_: UserRef, _: PurchaseInitRequest, _: Option[DateTime], _: Option[ExtendedUserType])(
          _: Traced
        ))
        .expects(userRef, PurchaseInitRequest.newBuilder().build(), *, *, *)
        .returningF(p2pWithPrices)
      (purchaseDao
        .create(_: Purchase, _: Iterable[PurchasedProduct]))
        .expects(p2pWithPrices.purchase, p2pWithPrices.products)
        .returningF(())
      (bankerService
        .buildPaymentInfo(_: User, _: PurchaseWithProducts, _: PriceContext)(_: Traced))
        .expects(vosUser, p2pWithPrices, *, *)
        .returningF(paymentInfo)
      (purchaseDao
        .updatePurchases(_: Iterable[PurchaseUpdate]))
        .expects(*)
        .never()

      val result = purchaseManager.initPurchases(userRef, PurchaseInitRequest.newBuilder().build()).futureValue

      result.hasPurchase should be(true)
      result.hasPaymentInfo should be(true)
      result.getPaymentInfo.getMethodsCount should not be 0
      result.getProductCount should be(p2pWithPrices.products.seq.size)
    }

    "Throws Exception if payment methods were not found" in {
      val purchasePriceGen = buildPurchaseWithOneProduct(Some(PriceContext(2L, 2L, Iterable.empty, Iterable.empty)))
      val userRef = passportUserGen.next
      val vosUser = generateUser
      val p2pWithPrices = purchasePriceGen.next
      (vosClient
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(userRef.uid, *, *)
        .returningF(Some(vosUser))
      (purchaseWithProductsBuilder
        .buildInitialPurchase(_: UserRef, _: PurchaseInitRequest, _: Option[DateTime], _: Option[ExtendedUserType])(
          _: Traced
        ))
        .expects(userRef, PurchaseInitRequest.newBuilder().build(), *, *, *)
        .returningF(p2pWithPrices)
      (purchaseDao
        .create(_: Purchase, _: Iterable[PurchasedProduct]))
        .expects(p2pWithPrices.purchase, p2pWithPrices.products)
        .returningF(())
      (bankerService
        .buildPaymentInfo(_: User, _: PurchaseWithProducts, _: PriceContext)(_: Traced))
        .expects(vosUser, p2pWithPrices, *, *)
        .returningF(PaymentInfo.getDefaultInstance)

      interceptCause[NoFoundMethodsInBankerException] {
        purchaseManager.initPurchases(userRef, PurchaseInitRequest.newBuilder().build()).futureValue
      }
    }
  }
}
