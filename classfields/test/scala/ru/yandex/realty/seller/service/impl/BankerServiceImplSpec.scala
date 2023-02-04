package ru.yandex.realty.seller.service.impl

import java.net.InetAddress
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import org.scalatest.time.{Millis, Minutes, Span}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.banker.{BankerClient, SuccessfulPayment}
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.managers.banker.BankerManager
import ru.yandex.realty.model.gen.ProtobufMessageGenerators
import ru.yandex.realty.seller.model.banker.PaymentResultWrapper
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.PriceContext
import ru.yandex.realty.seller.proto.api.payment.PaymentSuccess.{NoConfirmation, SmsConfirmation, UrlConfirmation}
import ru.yandex.realty.seller.proto.api.payment._
import ru.yandex.realty.seller.proto.api.purchase.PaymentInfo
import ru.yandex.realty.vos.model.user.User
import ru.yandex.vertis.banker.model.ApiModel.PaymentRequest.Form.{Field, Fields}
import ru.yandex.vertis.banker.model.ApiModel.{AccountConsumeRequest, AccountInfo, PaymentRequest, Transaction}
import ru.yandex.vertis.external.yandexkassa.ApiModel.Confirmation
import ru.yandex.vertis.protobuf.ProtoInstanceProvider
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.scalamock.util.RichFutureCallHandler

import scala.concurrent.Future

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class BankerServiceImplSpec
  extends AsyncSpecBase
  with PropertyChecks
  with SellerModelGenerators
  with RequestAware
  with OneInstancePerTest
  with ProtoInstanceProvider
  with ProtobufMessageGenerators {

  private val bankerClient = mock[BankerClient]
  private val bankerManager = mock[BankerManager]
  private val bankerService = new BankerServiceImpl(bankerClient, bankerManager)

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(1, Minutes), interval = Span(50, Millis))

  implicit class RichPaymentResult(val paymentResult: PaymentResult) {

    def withPaymentRequestId(paymentRequestId: String): PaymentResult = {
      val b = paymentResult.toBuilder
      b.getPaymentSuccessBuilder.setPaymentRequestId(paymentRequestId)
      b.build()
    }
  }

  "BankerService" should {
    "Throw exception if no wallet nor external system information provided" in {
      val vosUser = generate[User]().next
      val pwp = purchaseWithProductsGen.next

      interceptCause[IllegalStateException] {
        bankerService.makePayment(vosUser, ClientPaymentInfo.getDefaultInstance, pwp, pwp.fullPrice, None).futureValue
      }
    }

    "Successfully make payment using wallet" in {
      val vosUser = generate[User]().next
      val uid = vosUser.getId.toString
      val pwp = purchaseWithProductsGen.next
      val purchaseId = pwp.purchase.id
      val request = ClientPaymentInfo
        .newBuilder()
        .setPurchaseId(purchaseId)
        .setWallet(WalletPaymentInfo.getDefaultInstance)
        .build()
      val txn = generate[Transaction]().next
      val response = SuccessfulPayment(txn)

      val account = accountGen.next
      (bankerManager
        .getOrCreateAccount(_: User)(_: Traced))
        .expects(vosUser, *)
        .returningF(account)

      (bankerManager
        .makeWalletTransaction(_: String, _: Option[String], _: AccountConsumeRequest)(_: Traced))
        .expects(where { (user, _, accountConsumeRequest, _) =>
          user == uid && accountConsumeRequest.getAmount == pwp.fullPrice.effectivePrice
        })
        .returningF(response)

      (bankerManager
        .saveDefaultEmailIfEmpty(_: Long, _: String)(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(()))

      val expectedResult = PaymentResultWrapper(
        PaymentResult
          .newBuilder()
          .setPaymentSuccess(
            PaymentSuccess
              .newBuilder()
              .setNoConfirmation(NoConfirmation.getDefaultInstance)
              .setPaymentRequestId(txn.getId.getId)
          )
          .build,
        Some(txn.getId.getId)
      )
      val res = bankerService.makePayment(vosUser, request, pwp, pwp.fullPrice, None).futureValue
      res shouldBe expectedResult
    }

    "Successfully make payment using external system where form contains url" in {
      val vosUser = generate[User]().next
      val uid = vosUser.getId.toString
      val pwp = purchaseWithProductsGen.next
      val purchaseId = pwp.purchase.id
      val request = buildClientPaymentInfo(purchaseId, "some@yandex.ru")
      val paymentInfo = request.getExternalSystem
      val form = generate[PaymentRequest.Form]().next.toBuilder
        .clearFields()
        .clearUrl()
        .clearImpl()
        .setConfirmationType(Confirmation.Type.redirect)
        .setUrl("http://test.ru")
        .build()
      val response = SuccessfulPayment(form)

      val account = accountGen.next
      (bankerManager
        .getOrCreateAccount(_: User)(_: Traced))
        .expects(vosUser, *)
        .returningF(account)

      (bankerManager
        .makeExternalTransaction(
          _: String,
          _: Option[String],
          _: BankerPaymentMethod,
          _: Option[InetAddress],
          _: PaymentRequest.Source
        )(_: Traced))
        .expects(where { (user, _, method, _, requestSource, _) =>
          user == uid &&
          method == paymentInfo.getMethod &&
          requestSource.getAmount == pwp.fullPrice.effectivePrice
        })
        .returningF(response)

      (bankerManager
        .saveDefaultEmailIfEmpty(_: Long, _: String)(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(()))

      val expectedResult = PaymentResultWrapper(
        buildPaymentResult(form.getUrl).withPaymentRequestId(form.getId),
        Some(form.getId)
      )
      val res = bankerService.makePayment(vosUser, request, pwp, pwp.fullPrice, None).futureValue
      res shouldBe expectedResult
    }
  }

  "Successfully make payment using external system where form contains external confirmation" in {
    val vosUser = generate[User]().next
    val uid = vosUser.getId.toString
    val pwp = purchaseWithProductsGen.next
    val purchaseId = pwp.purchase.id
    val request = buildClientPaymentInfo(purchaseId, "some@yandex.ru")
    val paymentInfo = request.getExternalSystem
    val form = generate[PaymentRequest.Form]().next.toBuilder
      .clearFields()
      .clearUrl()
      .clearImpl()
      .setConfirmationType(Confirmation.Type.external)
      .build()
    val response = SuccessfulPayment(form)

    val account = accountGen.next
    (bankerManager
      .getOrCreateAccount(_: User)(_: Traced))
      .expects(vosUser, *)
      .returningF(account)

    (bankerManager
      .makeExternalTransaction(
        _: String,
        _: Option[String],
        _: BankerPaymentMethod,
        _: Option[InetAddress],
        _: PaymentRequest.Source
      )(_: Traced))
      .expects(where { (user, _, method, _, requestSource, _) =>
        user == uid &&
        method == paymentInfo.getMethod &&
        requestSource.getAmount == pwp.fullPrice.effectivePrice
      })
      .returningF(response)

    (bankerManager
      .saveDefaultEmailIfEmpty(_: Long, _: String)(_: Traced))
      .expects(*, *, *)
      .returning(Future.successful(()))

    val expectedResult = PaymentResultWrapper(
      buildPaymentResultSmsConfirmation.withPaymentRequestId(form.getId),
      Some(form.getId)
    )

    val res = bankerService.makePayment(vosUser, request, pwp, pwp.fullPrice, None).futureValue
    res shouldBe expectedResult
  }

  "Successfully make payment using external system where form contains fields" in {
    val vosUser = generate[User]().next
    val uid = vosUser.getId.toString
    val pwp = purchaseWithProductsGen.next
    val purchaseId = pwp.purchase.id
    val request = buildClientPaymentInfo(purchaseId, "user@yandex.ru")
    val paymentInfo = request.getExternalSystem
    val form = generate[PaymentRequest.Form]().next.toBuilder
      .clearFields()
      .clearUrl()
      .clearImpl()
      .setConfirmationType(Confirmation.Type.redirect)
      .setFields(
        Fields
          .newBuilder()
          .addFields(Field.newBuilder().setKey("action").setValue("http://test.yandex.kassa.ru/"))
          .addFields(Field.newBuilder().setKey("param1").setValue("black&white"))
          .build()
      )
      .build()
    val response = SuccessfulPayment(form)

    val account = accountGen.next
    (bankerManager
      .getOrCreateAccount(_: User)(_: Traced))
      .expects(vosUser, *)
      .returningF(account)

    (bankerManager
      .makeExternalTransaction(
        _: String,
        _: Option[String],
        _: BankerPaymentMethod,
        _: Option[InetAddress],
        _: PaymentRequest.Source
      )(_: Traced))
      .expects(where { (user, _, method, _, requestSource, _) =>
        user == uid &&
        method == paymentInfo.getMethod &&
        requestSource.getAmount == pwp.fullPrice.effectivePrice
      })
      .returningF(response)

    (bankerManager
      .saveDefaultEmailIfEmpty(_: Long, _: String)(_: Traced))
      .expects(*, *, *)
      .returning(Future.successful(()))

    val res = bankerService.makePayment(vosUser, request, pwp, pwp.fullPrice, None).futureValue
    val expectedResult =
      PaymentResultWrapper(
        buildPaymentResult("http://test.yandex.kassa.ru/?param1=black%26white").withPaymentRequestId(form.getId),
        Some(form.getId)
      )
    res shouldBe expectedResult
  }

  private def buildClientPaymentInfo(purchaseId: String, email: String): ClientPaymentInfo = {
    val generatedPaymentInfo = generate[BankerPaymentInfo]().next
    val paymentInfo = generatedPaymentInfo.toBuilder
      .setMethod(bankerPaymentMethodGen.next)
      .setContext(generatedPaymentInfo.getContext.toBuilder.setEmail(email))
      .build()
    ClientPaymentInfo
      .newBuilder()
      .setPurchaseId(purchaseId)
      .setExternalSystem(paymentInfo)
      .build()
  }

  private def buildPaymentResult(url: String): PaymentResult =
    PaymentResult
      .newBuilder()
      .setPaymentSuccess(PaymentSuccess.newBuilder().setUrlConfirmation(UrlConfirmation.newBuilder().setUrl(url)))
      .build

  private def buildPaymentResultSmsConfirmation: PaymentResult =
    PaymentResult
      .newBuilder()
      .setPaymentSuccess(PaymentSuccess.newBuilder().setSmsConfirmation(SmsConfirmation.getDefaultInstance))
      .build

  "Successfully make payment using promo code if effectivePrice is zero" in {
    val vosUser = generate[User]().next
    val uid = vosUser.getId.toString
    val pwp = purchaseWithProductsGen.next
    val purchaseId = pwp.purchase.id
    val request = ClientPaymentInfo
      .newBuilder()
      .setPurchaseId(purchaseId)
      .setPromocodeOnlyPayment(PromocodeOnlyPaymentInfo.getDefaultInstance)
      .build()
    val fullPrice = pwp.fullPrice.copy(effectivePrice = 0L)

    val accountBuilder = accountGen.next.toBuilder
    accountBuilder.getPropertiesBuilder.setEmail("some@email.com")

    val form = PaymentRequest.Form
      .newBuilder()
      .setId("oiusdgf8wcg9siudhocuahudc9")
      .build()

    (bankerManager
      .getOrCreateAccount(_: User)(_: Traced))
      .expects(vosUser, *)
      .returningF(accountBuilder.build())

    (bankerManager
      .makeExternalTransaction(
        _: String,
        _: Option[String],
        _: BankerPaymentMethod,
        _: Option[InetAddress],
        _: PaymentRequest.Source
      )(_: Traced))
      .expects(where { (user, _, method, _, requestSource, _) =>
        user == uid &&
        method == BankerServiceImpl.FreeOfChargeMethod
        requestSource.getAmount == pwp.fullPrice.basePrice
      })
      .returningF(SuccessfulPayment(form))

    val res = bankerService.makePayment(vosUser, request, pwp, fullPrice, None).futureValue
    val expectedResult = PaymentResultWrapper(
      PaymentResult
        .newBuilder()
        .setPaymentSuccess(
          PaymentSuccess
            .newBuilder()
            .setNoConfirmation(NoConfirmation.getDefaultInstance)
            .setPaymentRequestId(form.getId)
        )
        .build,
      transactionId = Some(form.getId)
    )
    res shouldBe expectedResult
  }

  "Throw exception for payment using promo code with nonzero effective price" in {
    val vosUser = generate[User]().next
    val pwp = purchaseWithProductsGen.next
    val purchaseId = pwp.purchase.id
    val request = ClientPaymentInfo
      .newBuilder()
      .setPurchaseId(purchaseId)
      .setPromocodeOnlyPayment(PromocodeOnlyPaymentInfo.getDefaultInstance)
      .build()
    val fullPrice = pwp.fullPrice.copy(effectivePrice = 1000L)

    interceptCause[IllegalArgumentException] {
      bankerService.makePayment(vosUser, request, pwp, fullPrice, None).futureValue
    }
  }

  "Not return wallet if wallet doesn't contain enough money" in {
    val vosUser = generate[User]().next
    val uid = vosUser.getId.toString
    val price = Gen.posNum[Long].next
    val walletBalance = Gen.choose[Long](0L, price - 1).next
    val pwp = buildPurchaseWithOneProduct(Some(PriceContext(2L, price, Iterable.empty, Iterable.empty))).next
    val account = accountGen.next

    assert(pwp.products.nonEmpty && pwp.fullPrice.effectivePrice > 0)

    val accountInfo = AccountInfo
      .newBuilder()
      .setBalance(walletBalance)
      .build()

    assert(accountInfo.getBalance >= 0 && accountInfo.getBalance < pwp.fullPrice.effectivePrice)

    (bankerManager
      .getOrCreateAccount(_: User)(_: Traced))
      .expects(vosUser, *)
      .returningF(account)

    (bankerManager
      .getAccountInfo(_: String, _: Option[String], _: Option[DateTime], _: Option[DateTime])(_: Traced))
      .expects(uid, Some(uid), *, *, *)
      .returning(Future.successful(Some(accountInfo)))

    (bankerManager
      .getPaymentMethods(_: String, _: Option[String], _: Option[InetAddress], _: PaymentRequest.Source)(_: Traced))
      .expects(uid, Some(uid), *, *, *)
      .returning(Future.successful(Seq()))

    val res: PaymentInfo = bankerService.buildPaymentInfo(vosUser, pwp, pwp.fullPrice).futureValue

    res should not be None
    res.hasWallet should be(false)
  }
}
