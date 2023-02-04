package ru.yandex.vertis.billing.banker.payment

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.dao.AccountDao
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{Context, Targets}
import ru.yandex.vertis.billing.banker.model.State.{AbstractNotificationSource, BytesNotificationSource, StateStatuses}
import ru.yandex.vertis.billing.banker.model._
import ru.yandex.vertis.billing.banker.model.gens.{PaymentPayloadGen, Producer}
import ru.yandex.vertis.billing.banker.service.PaymentSystemSupport
import ru.yandex.vertis.billing.banker.service.PaymentSystemSupport.MethodFilter
import ru.yandex.vertis.billing.banker.util.{DateTimeUtils, UserContext}

import scala.util.{Failure, Success}

/**
  * Specs for [[PaymentSystemSupport]] related to micropayments
  *
  * @author alesavin
  */
trait MicropaymentSupportSpec extends AnyWordSpec with Matchers with AsyncSpecBase {

  def accounts: AccountDao
  def paymentSystem: PaymentSystemSupport

  implicit val userContext: UserContext =
    UserContext("MicropaymentSupportSpec", "I am human. Trust me :)")

  "MicropaymentSupport" should {

    val Customer = "3"
    val Acc = "1002"
    val source =
      PaymentRequest.Source(
        Acc,
        10L,
        PaymentPayloadGen.next,
        PaymentRequest.Options(
          Some("payment1"),
          Some("http://ya.ru")
        ),
        None,
        Context(Targets.Wallet),
        None,
        None
      )
    var method: Option[PaymentMethod] = None
    var paymentRequest: Option[PaymentRequestId] = None

    "provide self id" in {
      PaymentSystemIds.values.contains(paymentSystem.psId)
    }
    "check is compatible for customer and source" in {
      paymentSystem
        .getMethods(
          Customer,
          MethodFilter.ForSource(source)
        )
        .toTry match {
        case Success(ms) if ms.nonEmpty =>
          ms.find(_.ps != paymentSystem.psId) should be(None)
          method = Some(ms.head)
        case other => fail(s"Unexpected $other")
      }
    }
    "fail if initiate payment with unsupported method" in {
      paymentSystem
        .request(
          Customer,
          "Method",
          source
        )
        .toTry match {
        case Failure(_) => info(s"Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "fail if initiate payment with supported method and non-exist account" in {
      paymentSystem
        .request(
          Customer,
          method.get.id,
          source
        )
        .toTry match {
        case Failure(_) => info(s"Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "create account" in {
      val a = Account(Acc, Customer)
      accounts.upsert(a).toTry match {
        case Success(_) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "pass to create payment request" in {
      paymentSystem
        .request(
          Customer,
          method.get.id,
          source
        )
        .toTry match {
        case Success(PaymentRequest.UrlForm(id, url, _)) =>
          info(s"Done $url")
          paymentRequest = Some(id)
        case other => fail(s"Unexpected $other")
      }
      paymentSystem.getPaymentRequest(Customer, paymentRequest.get).toTry match {
        case Success(pr) =>
          pr.id should be(paymentRequest.get)
          pr.source should be(source)
          pr.method.id should be(method.get.id)
        case other => fail(s"Unexpected $other")
      }
    }
    "fail to parse empty notification to payment" in {
      paymentSystem.parse(BytesNotificationSource("", "text/plain", Array[Byte]())).toTry match {
        case Failure(_) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "pass to parse predefined record" in {
      val incoming =
        State.Incoming(paymentRequest.get, source.account, source.amount, DateTimeUtils.now(), Raw.Empty)
      paymentSystem.parse(AbstractNotificationSource(incoming)).toTry match {
        case Success(_) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      paymentSystem.getPaymentRequest(source.account, paymentRequest.get).toTry match {
        case Success(PaymentRequest(_, _, _, _, Some(_: State.Incoming))) =>
          info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "refund payment" in {
      paymentSystem.fullRefund(Customer, paymentRequest.get, None, None).toTry match {
        case Success(_) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      paymentSystem.getPaymentRequest(Customer, paymentRequest.get).toTry match {
        case Success(PaymentRequest(_, _, _, _, Some(p: State.Incoming))) if p.stateStatus == StateStatuses.Refunded =>
          info("Done")
        case other => fail(s"Unexpected $other")
      }
    }

  }

}
