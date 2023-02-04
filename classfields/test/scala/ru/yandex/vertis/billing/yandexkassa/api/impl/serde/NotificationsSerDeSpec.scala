package ru.yandex.vertis.billing.yandexkassa.api.impl.serde

import org.jose4j.jws.JsonWebSignature
import ru.yandex.vertis.billing.yandexkassa.api.model.notifications.Notification
import ru.yandex.vertis.billing.yandexkassa.api.model.notifications.Notification.Response
import ru.yandex.vertis.billing.yandexkassa.api.model.{
  ErrorCodes,
  Instrument,
  NotificationType,
  NotificationTypes,
  OrderStatuses,
  PaymentSources
}
import ru.yandex.vertis.util.crypto.UrlEncodedUtils
import spray.json.enrichString

/**
  * Specs on [[NotificationsSerDe]]
  *
  * @author alex-kovalenko
  */
class NotificationsSerDeSpec extends SerDeSpecBase {

  prepareCheckOrder()
  preparePaymentAviso()
  preparePaymentAvisoWithRebilling()

  "NotificationsSerDe" should {
    "parse CheckOrder notification" in {
      val raw = getContent("/api/n_CheckOrder.txt")
      val n = NotificationsSerDe.parse(NotificationsSerDe.parseJWS(raw, settings).getUnverifiedPayload.parseJson)
      val e = Notification(
        notificationType = NotificationTypes.CheckOrder,
        orderId = orderId,
        status = OrderStatuses.Created,
        createdDt = createdDt,
        recipient = recipient,
        order = order,
        source = Some(PaymentSources.BankCard),
        method = Some("/api/v2/payments/dsrpWallet"),
        authorizedDt = Some(authorizedDt),
        cardAuthorizedDt = None,
        cardAuthorizeResult = None,
        charge = Some(charge),
        income = Some(income),
        instrument = None
      )

      n shouldBe e
    }

    "parse PaymentAviso notification" in {
      val raw = getContent("/api/n_PaymentAviso.txt")
      val n = NotificationsSerDe.parse(NotificationsSerDe.parseJWS(raw, settings).getUnverifiedPayload.parseJson)
      val e = Notification(
        notificationType = NotificationTypes.PaymentAviso,
        orderId = orderId,
        status = OrderStatuses.Authorized,
        createdDt = createdDt,
        recipient = recipient,
        order = order,
        source = Some(PaymentSources.BankCard),
        method = Some("/api/v2/payments/dsrpWallet"),
        authorizedDt = Some(authorizedDt),
        cardAuthorizedDt = None,
        cardAuthorizeResult = None,
        charge = Some(charge),
        income = Some(income),
        instrument = None
      )

      n shouldBe e
    }

    "parse PaymentAviso with rebilling notification" in {
      val raw = getContent("/api/n_PaymentAviso_rebilling.txt")
      val n = NotificationsSerDe.parse(NotificationsSerDe.parseJWS(raw, settings).getUnverifiedPayload.parseJson)
      val e = Notification(
        notificationType = NotificationTypes.PaymentAviso,
        orderId = orderId,
        status = OrderStatuses.Authorized,
        createdDt = createdDt,
        recipient = recipient,
        order = order,
        source = Some(PaymentSources.BankCard),
        method = Some("/api/v2/payments/bankCard"),
        authorizedDt = Some(authorizedDt),
        cardAuthorizedDt = None,
        cardAuthorizeResult = None,
        charge = Some(charge),
        income = Some(income),
        instrument = Some(
          Instrument(
            PaymentSources.BankCard,
            method = "/api/v2/payments/bankCard",
            title = "5189 01** **** 0446",
            reference = "invoiceId:2000366020286"
          )
        )
      )

      n shouldBe e
    }

    def checkResponse(`type`: NotificationType, status: String, response: Notification.Response): Unit = {
      s"serialize $status ${`type`} response" in {
        val serialized = NotificationsSerDe.serialize(response, settings)
        val responseData = new String(serialized.data)
        val parsedPayload = NotificationsSerDe.parseJWS(responseData, settings).getUnverifiedPayload

        val expected = getJson(s"/api/n_${`type`}_result_$status.json")

        parsedPayload.parseJson shouldBe expected
      }
    }

    val checkOrderResponse = Response(
      notificationType = NotificationTypes.CheckOrder,
      orderId = Some(orderId),
      status = Some(OrderStatuses.Created),
      error = None,
      parameterName = None
    )

    checkResponse(
      NotificationTypes.CheckOrder,
      "success",
      checkOrderResponse.copy(status = Some(OrderStatuses.Approved))
    )

    checkResponse(
      NotificationTypes.CheckOrder,
      "refused",
      checkOrderResponse.copy(status = Some(OrderStatuses.Refused), error = Some(ErrorCodes.IllegalSignature))
    )

    checkResponse(
      NotificationTypes.CheckOrder,
      "syntax_error",
      checkOrderResponse
        .copy(status = Some(OrderStatuses.Refused), error = Some(ErrorCodes.SyntaxError), orderId = None)
    )

    val paymentAvisoResponse = Response(
      notificationType = NotificationTypes.PaymentAviso,
      orderId = Some(orderId),
      status = Some(OrderStatuses.Created),
      error = None,
      parameterName = None
    )

    checkResponse(
      NotificationTypes.PaymentAviso,
      "success",
      paymentAvisoResponse.copy(status = Some(OrderStatuses.Delivered))
    )

    checkResponse(
      NotificationTypes.PaymentAviso,
      "refused",
      paymentAvisoResponse.copy(status = Some(OrderStatuses.Refused), error = Some(ErrorCodes.IllegalSignature))
    )

    checkResponse(
      NotificationTypes.PaymentAviso,
      "syntax_error",
      paymentAvisoResponse
        .copy(status = Some(OrderStatuses.Refused), error = Some(ErrorCodes.SyntaxError), orderId = None)
    )
  }

  private def preparePaymentAvisoWithRebilling(): Unit = {
    prepareNotification("PaymentAviso with rebilling", "/api/n_PaymentAviso_rebilling_payload.json")
  }

  private def preparePaymentAviso(): Unit = {
    prepareNotification("PaymentAviso", "/api/n_PaymentAviso_payload.json")
  }

  private def prepareCheckOrder(): Unit = {
    prepareNotification("CheckOrder", "/api/n_CheckOrder_payload.json")
  }

  private def prepareNotification(label: String, fileName: String): Unit = {
    val payload = getJson(fileName).compactPrint
    val jws = new JsonWebSignature
    jws.setHeader("alg", "ES256")
    jws.setHeader("iat", "1447777017465")
    jws.setHeader("iss", "Yandex.Money")
    jws.setHeader("aud", "test")
    jws.setPayload(payload)
    jws.setKey(settings.privateKey)
    jws.sign()
    info(s"$label: ${jws.getCompactSerialization}")
  }
}
