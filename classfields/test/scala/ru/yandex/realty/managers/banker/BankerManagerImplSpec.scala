package ru.yandex.realty.managers.banker

import java.net.InetAddress

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.banker.{BankerClient, FailedPayment, SuccessfulPayment}
import ru.yandex.realty.clients.blackbox.BlackboxClient
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.model.gen.ProtobufMessageGenerators
import ru.yandex.realty.seller.proto.api.payment.BankerPaymentMethod
import ru.yandex.realty.vos.model.user.{User, UserContacts}
import ru.yandex.vertis.banker.model.ApiModel._
import ru.yandex.vertis.protobuf.ProtoInstanceProvider
import ru.yandex.vertis.scalamock.util.RichFutureCallHandler
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class BankerManagerImplSpec
  extends AsyncSpecBase
  with RequestAware
  with OneInstancePerTest
  with ProtoInstanceProvider
  with ProtobufMessageGenerators {

  private val bankerClient = mock[BankerClient]
  private val vosClient = mock[VosClientNG]
  private val blackboxClient = mock[BlackboxClient]
  private val bankerManager = new BankerManagerImpl(bankerClient, vosClient, blackboxClient)

  private val uidWithAccount: Long = 123456L
  private val uidWithoutAccount: Long = 654321L
  private val existingAccount = Account
    .newBuilder()
    .setId(uidWithAccount + "_" + "someValue")
    .build()
  private val existingAccountInfo = AccountInfo
    .newBuilder()
    .setBalance(0L)
    .build()

  {
    (bankerClient
      .getAccount(_: String, _: Option[String])(_: Traced))
      .expects(*, *, *)
      .onCall((uid: String, vertisUser: Option[String], traced: Traced) => {
        if (uid == uidWithAccount.toString) {
          Future.successful(Some(existingAccount))
        } else {
          Future.successful(None)
        }
      })
      .anyNumberOfTimes()
  }

  "BankerManager " should {
    "getAccountInfo returns AccountInfo if Account exists" in {
      (bankerClient
        .getAccountInfo(_: String, _: String, _: Option[String], _: Option[DateTime], _: Option[DateTime])(_: Traced))
        .expects(uidWithAccount.toString, existingAccount.getId, *, *, *, *)
        .returning(Future.successful(Some(existingAccountInfo)))

      val result = bankerManager.getAccountInfo(uidWithAccount.toString, None, None, None)
      result.futureValue should be(Some(existingAccountInfo))
    }

    "getAccountInfo returns is None if Account does not exist" in {
      val result = bankerManager.getAccountInfo(uidWithoutAccount.toString, None, None, None)
      result.futureValue should be(None)
    }

    "getOrCreateAccount returns Account if Account already exists" in {
      val user = User.newBuilder().setId(uidWithAccount).build()
      val result = bankerManager.getOrCreateAccount(user)
      result.futureValue should be(existingAccount)
    }

    "getOrCreateAccount creates Account if Account does not already exist" in {
      val user = User
        .newBuilder()
        .setId(uidWithoutAccount)
        .setUserContacts(
          UserContacts
            .newBuilder()
            .setEmail("some@ya.ru")
            .build()
        )
        .build()
      (bankerClient
        .createAccount(_: String, _: Option[String], _: Account)(_: Traced))
        .expects(uidWithoutAccount.toString, None, *, *)
        .onCall((uid: String, vertisUser: Option[String], account: Account, traced: Traced) => {
          Future.successful(Some(account))
        })
        .anyNumberOfTimes()

      val result = bankerManager.getOrCreateAccount(user).futureValue

      result.getProperties.getEmail should be(user.getUserContacts.getEmail)
      result.getUser should be(uidWithoutAccount.toString)
      result.getId should endWith(uidWithoutAccount.toString)
    }

    "successfully makeExternalTransaction in banker" in {
      val vosUser = generate[User]().next
      val uid = vosUser.getId.toString
      val method = generate[BankerPaymentMethod]().next
      val request = generate[PaymentRequest.Source]().next
      val form = generate[PaymentRequest.Form]().next
      val response = SuccessfulPayment(form)

      (bankerClient
        .makeExternalTransaction(
          _: String,
          _: Option[String],
          _: Option[InetAddress],
          _: BankerPaymentMethod,
          _: PaymentRequest.Source
        )(_: Traced))
        .expects(uid, Some(uid), None, method, request, *)
        .returningF(response)

      val res = bankerManager.makeExternalTransaction(uid, Some(uid), method, None, request).futureValue
      res shouldBe response
    }

    "handle failed payment in makeExternalTransaction" in {
      val vosUser = generate[User]().next
      val uid = vosUser.getId.toString
      val method = generate[BankerPaymentMethod]().next
      val request = generate[PaymentRequest.Source]().next
      val apiError = ApiError
        .newBuilder()
        .setPaymentCancellationError(ApiError.CancellationPaymentError.INVALID_CARD_NUMBER)
        .build()
      val response = FailedPayment(apiError)

      (bankerClient
        .makeExternalTransaction(
          _: String,
          _: Option[String],
          _: Option[InetAddress],
          _: BankerPaymentMethod,
          _: PaymentRequest.Source
        )(_: Traced))
        .expects(uid, Some(uid), None, method, request, *)
        .returningF(response)

      val res = bankerManager.makeExternalTransaction(uid, Some(uid), method, None, request).futureValue
      res shouldBe response
    }

    "successfully makeWalletTransaction" in {
      val vosUser = generate[User]().next
      val uid = vosUser.getId.toString
      val accountConsumeRequest = generate[AccountConsumeRequest]().next
      val txn = generate[Transaction]().next
      val response = SuccessfulPayment(txn)

      (bankerClient
        .makeWalletTransaction(_: String, _: Option[String], _: AccountConsumeRequest)(_: Traced))
        .expects(uid, Some(uid), accountConsumeRequest, *)
        .returningF(response)

      val res = bankerManager.makeWalletTransaction(uid, Some(uid), accountConsumeRequest).futureValue
      res shouldBe response
    }

    "handle failed payment in makeWalletTransaction" in {
      val vosUser = generate[User]().next
      val uid = vosUser.getId.toString
      val accountConsumeRequest = generate[AccountConsumeRequest]().next
      val response = FailedPayment(ApiError.getDefaultInstance)

      (bankerClient
        .makeWalletTransaction(_: String, _: Option[String], _: AccountConsumeRequest)(_: Traced))
        .expects(uid, Some(uid), accountConsumeRequest, *)
        .returningF(response)

      val res = bankerManager.makeWalletTransaction(uid, Some(uid), accountConsumeRequest).futureValue
      res shouldBe response
    }
  }
}
