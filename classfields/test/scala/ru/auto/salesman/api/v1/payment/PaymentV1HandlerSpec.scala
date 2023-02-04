package ru.auto.salesman.api.v1.payment

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.github.nscala_time.time.Imports.DateTime
import com.google.protobuf.timestamp.Timestamp
import org.joda.time.DateTimeZone
import org.scalatest.Inspectors
import ru.auto.salesman.api.v1.HandlerBaseSpec
import ru.auto.salesman.api.v1.SalesmanApiUtils.SalesmanHttpRequest
import ru.auto.salesman.environment.RichDateTime
import ru.auto.salesman.model.user.{PaymentPayloadRaw, PaymentRequestRaw}
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, PaymentActions}
import ru.auto.salesman.service.user.PaymentService
import ru.auto.salesman.service.user.exceptions.NoActivePaidProductForTransactionException
import ru.auto.salesman.test.proto.user.PaymentRequestWriter
import ru.yandex.vertis.banker.model.events_model.{
  PaymentFailureNotification,
  PaymentFailureNotificationsList
}

import java.io.ByteArrayOutputStream
import java.util.UUID

class PaymentV1HandlerSpec extends HandlerBaseSpec {

  override lazy val paymentService: PaymentService = mock[PaymentService]
  override lazy val domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  "payment/receiver" should {
    "handle request no matter what domain presented in request body" in {
      Inspectors.forEvery(
        Seq(DeprecatedDomains.AutoRu.toString, "booking", "any-other-domain")
      ) { sampleDomain =>
        val sampleTime =
          DateTime.parse("2020-05-27T12:13:14.123456789+04:30:15")

        val requestBody = PaymentRequestRaw(
          bankerTransactionId = UUID.randomUUID().toString,
          payload = PaymentPayloadRaw(UUID.randomUUID().toString, sampleDomain),
          time = sampleTime,
          action = PaymentActions.Activate
        )

        val requestBodyInProto = PaymentRequestWriter.write(requestBody)

        val timeAfterProto = sampleTime.withZone(DateTimeZone.getDefault)
        val expectedParsedBody = requestBody.copy(time = timeAfterProto)

        (paymentService.receiveRaw _)
          .expects(Stream(expectedParsedBody))
          .returningZ(())
          .once()

        val stream = new ByteArrayOutputStream()
        requestBodyInProto.writeDelimitedTo(stream)

        Post("/api/1.x/payment/receive")
          .withEntity(stream.toByteArray)
          .withSalesmanTestHeader() ~> Route.seal(route) ~> check {
          status shouldBe StatusCodes.OK
        }
      }
    }

    "handle payment failure request" in {
      val sampleTime =
        DateTime.parse("2020-05-27T12:13:14.123456789+04:30:15")

      val requestBody: PaymentFailureNotificationsList = PaymentFailureNotificationsList(
        Seq(
          PaymentFailureNotification(
            Some(Timestamp(sampleTime.asTimestamp.getSeconds)),
            Some(
              PaymentFailureNotification.PaymentFailurePayload(
                UUID.randomUUID().toString,
                "NO_MONEY"
              )
            )
          )
        )
      )

      (paymentService.receiveFailureRaw _)
        .expects(requestBody)
        .returningZ(())
        .once()

      Post("/api/1.x/payment/receive/failure")
        .withEntity(requestBody.toByteArray)
        .withSalesmanTestHeader() ~> Route.seal(route) ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return 500 if no active paid product found" in {
      val sampleTime =
        DateTime.parse("2020-05-27T12:13:14.123456789+04:30:15")

      val requestBody: PaymentFailureNotificationsList = PaymentFailureNotificationsList(
        Seq(
          PaymentFailureNotification(
            Some(Timestamp(sampleTime.asTimestamp.getSeconds)),
            Some(
              PaymentFailureNotification.PaymentFailurePayload(
                UUID.randomUUID().toString,
                "NO_MONEY"
              )
            )
          )
        )
      )

      (paymentService.receiveFailureRaw _)
        .expects(requestBody)
        .throwingZ(NoActivePaidProductForTransactionException("asd"))
        .once()

      Post("/api/1.x/payment/receive/failure")
        .withEntity(requestBody.toByteArray)
        .withSalesmanTestHeader() ~> Route.seal(route) ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

}
