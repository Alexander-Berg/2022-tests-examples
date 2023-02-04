package ru.yandex.vertis.billing.banker.api

import akka.http.scaladsl.model.{HttpEntity, StatusCode, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.model.ApiModel.ApiError
import ru.yandex.vertis.billing.banker.api.DomainExceptionHandler.{ErrorJsonWriter, ErrorProtoWriter}
import ru.yandex.vertis.billing.banker.exceptions.Exceptions._
import ru.yandex.vertis.billing.banker.service.PaymentSystemSupport.{
  ExternalServiceException,
  ExternalServiceTimeoutException
}
import ru.yandex.vertis.billing.yandexkassa.api.YandexKassaApiV3.InvalidCredentialsException
import ru.yandex.vertis.external.yandexkassa.ApiModel.{ApiError => YandexKassaApiError}
import spray.json.{JsValue, JsonParser, ParserInput}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class ExceptionHandlerSpec extends AnyWordSpecLike with HandlerSpecBase with Directives with AsyncSpecBase {

  private def route(exception: Exception): Route = {
    path("ping") {
      failWith(exception)
    }
  }

  private val timeout: FiniteDuration = 3.seconds

  private def check(exception: Exception, expectedCode: StatusCode): Unit = {
    val (expectedMessage, expectedJson) = expectedData(exception)
    val toMessageMapper = { entity: HttpEntity.Strict =>
      val data = entity.data.toArray
      expectedMessage.getParserForType.parseFrom(data)
    }
    check(exception, expectedCode, addAcceptProtobuf, toMessageMapper, expectedMessage)

    val toJsonMapper = { entity: HttpEntity.Strict =>
      JsonParser(ParserInput(entity.data.toArray))
    }
    check(exception, expectedCode, addAcceptJson, toJsonMapper, expectedJson)
  }

  private def check[T](
      exception: Exception,
      expectedCode: StatusCode,
      transformer: RequestTransformer,
      entityMapper: HttpEntity.Strict => T,
      expectedResult: T): Unit = {
    val aim = seal(route(exception))
    val request = transformer(Get(url("/ping")))
    request ~> aim ~> check {
      status shouldBe expectedCode
      val strictEntity = response.entity.toStrict(timeout)
      val actualResult = strictEntity.map(entityMapper).futureValue
      actualResult shouldBe expectedResult: Unit
    }
  }

  private def expectedData(exception: Exception): (ApiError, JsValue) = {
    val protoError = ErrorProtoWriter.write(exception)
    val jsonError = ErrorJsonWriter.write(exception)
    (protoError, jsonError)
  }

  "ExceptionHandler" should {
    "work correctly" when {
      "ExternalServiceTimeoutException was thrown" in {
        val message = "time out"
        val duration = 123.seconds
        val exception = ExternalServiceTimeoutException(message, Some(duration))
        check(exception, StatusCodes.GatewayTimeout)
      }
      "ExternalServiceException was thrown" in {
        ExternalServiceException.Codes.values.foreach { code =>
          val message = "external exception"
          val exception = ExternalServiceException(code, message)
          check(exception, StatusCodes.BadGateway)
        }
      }
      "EmailBadFormatException was thrown" in {
        val message = "email bad format"
        val exception = new EmailBadFormatException(message)
        check(exception, StatusCodes.BadRequest)
      }
      "NotEnoughMoneyToRefundException was thrown" in {
        val message = "not enough money to refund exception"
        val exception = new NotEnoughMoneyToRefundException(message)
        check(exception, StatusCodes.BadRequest)
      }
      "YandexUidRequiredException was thrown" in {
        val message = "Yandex UID is required for Trust payments"
        val exception = new YandexUidRequiredException(message)
        check(exception, StatusCodes.BadRequest)
      }
      "YandexKassaV3BadRequest was thrown" in {
        val message = "Yandex Kassa V3 bad request"
        val kassaError = YandexKassaApiError.newBuilder().build()
        val cause = InvalidCredentialsException(kassaError)
        val exception = new YandexKassaV3BadRequest(message, cause)
        check(exception, StatusCodes.BadRequest)
      }
      "AccountNotFoundException was thrown" in {
        val accountId = "123"
        val exception = new AccountNotFoundException(accountId)
        check(exception, StatusCodes.NotFound)
      }
      "DuplicatePaymentIdException was thrown" in {
        val paymentRequestId = "123"
        val exception = new DuplicatePaymentIdException(paymentRequestId)
        check(exception, StatusCodes.Conflict)
      }
      "PaymentAlreadyRefundedException was thrown" in {
        val paymentRequestId = "123"
        val exception = new PaymentAlreadyRefundedException(paymentRequestId)
        check(exception, StatusCodes.Conflict)
      }
      "PaymentPartlyRefundedException was thrown" in {
        val paymentRequestId = "123"
        val exception = new PaymentAlreadyRefundedException(paymentRequestId)
        check(exception, StatusCodes.Conflict)
      }
      "PaymentAlreadyChargedBackException was thrown" in {
        val paymentRequestId = "123"
        val exception = new PaymentAlreadyChargedBackException(paymentRequestId)
        check(exception, StatusCodes.Conflict)
      }
      "RefundAlreadyProcessedException was thrown" in {
        val refundPaymentRequestId = "123"
        val exception = RefundAlreadyProcessedException(refundPaymentRequestId)
        check(exception, StatusCodes.Conflict)
      }
      "UnprocessedRefundAlreadyExistsException was thrown" in {
        val paymentRequestId = "123"
        val exception = UnprocessedRefundAlreadyExistsException(paymentRequestId)
        check(exception, StatusCodes.Conflict)
      }
      "BankerInternalServerError was thrown" in {
        check(BankerInternalServerError, StatusCodes.InternalServerError)
      }
      "CancellationPaymentException was thrown" in {
        CancellationReasons.values.foreach { reason =>
          val message = "cancelled"
          val exception = new CancellationPaymentException(reason, message)
          check(exception, StatusCodes.PaymentRequired)
        }
      }
      "NotEnoughFunds was thrown" in {
        val message = "no money, no honey"
        val exception = new NotEnoughFunds(message)
        check(exception, StatusCodes.PaymentRequired)
      }
    }
  }

}
