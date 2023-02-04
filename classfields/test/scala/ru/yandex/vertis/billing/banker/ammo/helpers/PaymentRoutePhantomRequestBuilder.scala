package ru.yandex.vertis.billing.banker.ammo.helpers

import akka.http.scaladsl.model.ContentTypes
import ru.yandex.vertis.billing.banker.api.v1.view.PaymentRequestSourceView
import ru.yandex.vertis.billing.banker.model.{Account, Funds, Payload, PaymentRequest, PaymentRequestId}
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{Context, Options, Targets}
import ru.yandex.vertis.billing.banker.ammo.Constants.Protocol

trait PaymentRoutePhantomRequestBuilder extends BasePhantomRequestBuilder with RequestPathProviders with HeaderHelpers {

  private val SuperUser = "stress-super-user"
  private val Localhost = "127.0.0.1"

  def getAllowedPaymentsMethodsRequest(account: Account, amount: Funds = 100): String = {
    val source = PaymentRequest.Source(
      account.id,
      amount,
      Payload.Empty,
      Options(),
      None,
      Context(Targets.Wallet),
      None,
      None
    )
    val body = PaymentRequestSourceView.jsonFormat
      .write(PaymentRequestSourceView(source))
      .compactPrint
    build(
      s"POST ${paymentPath(account.user)} $Protocol",
      customHeaders(
        vertisUser = Some(account.user),
        contentType = Some(ContentTypes.`application/json`),
        xForwardedFor = Some(Localhost)
      ),
      Some(body),
      tag = "get_allowed_payment_methods"
    )
  }

  def initPaymentRequest(
      account: Account,
      amount: Funds = 100000,
      paymentRequestId: Option[PaymentRequestId] = None): String = {
    val source = PaymentRequest.Source(
      account.id,
      amount,
      Payload.Empty,
      Options(id = paymentRequestId),
      None,
      Context(Targets.Wallet),
      None,
      None
    )
    val body = PaymentRequestSourceView.jsonFormat
      .write(PaymentRequestSourceView(source))
      .compactPrint
    build(
      s"POST ${paymentPath(account.user)}/freeofcharge/method/free $Protocol",
      customHeaders(
        vertisUser = Some(account.user),
        contentType = Some(ContentTypes.`application/json`),
        xForwardedFor = Some(Localhost)
      ),
      Some(body),
      tag = "init_payment"
    )
  }

}
