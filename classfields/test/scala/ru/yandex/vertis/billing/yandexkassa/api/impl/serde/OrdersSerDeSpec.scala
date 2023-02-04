package ru.yandex.vertis.billing.yandexkassa.api.impl.serde

import ru.yandex.vertis.billing.yandexkassa.api.model.orders.{Refund, RefundRequest, RefundResponse}
import ru.yandex.vertis.billing.yandexkassa.api.model.{ErrorCodes, OrderStatuses}
import ru.yandex.vertis.util.crypto.UrlEncodedUtils
import spray.json.JsonParser

/**
  * Specs on [[OrdersSerDe]]
  *
  * @author alex-kovalenko
  */
class OrdersSerDeSpec extends SerDeSpecBase {

  "OrdersSerDe" should {

    "serialize refund request" in {
      val refund = Refund(clientOrderId, charge, Some("comment"), None)
      val rq = RefundRequest(orderId, refund)

      val serialized = OrdersSerDe.serialize(rq, settings)
      val requestData = UrlEncodedUtils.from(serialized).get.toList match {
        case ("request", rqData) :: Nil =>
          rqData
        case other =>
          fail(s"Unexpected $other")
      }

      val parsedPayload = OrdersSerDe.parseJWS(requestData, settings).getUnverifiedPayload

      val expected = getJson("/api/refund_rq_payload.json")

      JsonParser(parsedPayload) shouldBe expected
    }

    def checkResponse(name: String, expected: RefundResponse): Unit = {
      s"parse $name RefundResponse" in {
        val json = getJson(s"/api/refund_rs_$name.json")
        val response = OrdersSerDe.parseRefund(json)

        response shouldBe expected
      }
    }

    checkResponse(
      "success",
      RefundResponse(
        status = Some(OrderStatuses.Refunded),
        orderId = Some(orderId),
        refund = Some(Refund(clientOrderId, charge, None, Some(OrderStatuses.Authorized))),
        error = None,
        parameterName = None,
        nextRetry = None
      )
    )

    checkResponse(
      "refuse",
      RefundResponse(
        status = Some(OrderStatuses.Canceled),
        orderId = Some(orderId),
        error = Some(ErrorCodes.InappropriateStatus),
        refund = Some(Refund(clientOrderId, charge, None, Some(OrderStatuses.Refused))),
        parameterName = None,
        nextRetry = None
      )
    )

    checkResponse(
      "processing",
      RefundResponse(
        status = Some(OrderStatuses.Delivered),
        orderId = Some(orderId),
        refund = Some(Refund(clientOrderId, charge, None, Some(OrderStatuses.Processing))),
        parameterName = None,
        nextRetry = Some(5000),
        error = None
      )
    )

    checkResponse(
      "error",
      RefundResponse(
        error = Some(ErrorCodes.IllegalParameter),
        parameterName = Some("refund.clientOrderId"),
        status = None,
        orderId = None,
        nextRetry = None,
        refund = None
      )
    )
  }
}
