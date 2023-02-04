package ru.yandex.realty.clients.banker

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.http.{HttpClientMock, RequestAware}
import ru.yandex.vertis.generators.ProducerProvider
import akka.http.scaladsl.model.HttpMethods.{POST, PUT}
import akka.http.scaladsl.model.StatusCodes
import ru.yandex.realty.seller.proto.api.payment.BankerPaymentMethod
import ru.yandex.vertis.banker.model.ApiModel.ApiError.CancellationPaymentError
import ru.yandex.vertis.banker.model.ApiModel.{AccountConsumeRequest, _}
import ru.yandex.vertis.banker.model.ApiModel.PaymentRequest.Form
import ru.yandex.vertis.external.yandexkassa.ApiModel.Confirmation
import ru.yandex.realty.tracing.Traced

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class BankerClientSpec
  extends AsyncSpecBase
  with PropertyChecks
  with RequestAware
  with HttpClientMock
  with ProducerProvider {

  val uid = "4007216463"

  val client = new BankerClientImpl(httpService)

  "BankerClient" should {
    "successfully post makeExternalTransaction" in {
      val method = yandexKassaV3BankCardMethod
      val request = PaymentRequest.Source.getDefaultInstance

      val response = Form
        .newBuilder()
        .setId("295b5a6c-5cc3-473c-9f0a-1e7cedaa5fcf")
        .setFields(
          Form.Fields
            .newBuilder()
            .addFields(Form.Field.newBuilder().setKey("action").setValue("https://demomoney.yandex.ru/eshop.xml"))
        )
        .build()

      expectYandexKassaV3(method, request)
      httpClient.respondWith(response)

      val res = client.makeExternalTransaction(uid, None, None, method, request)(Traced.empty).futureValue
      res shouldBe SuccessfulPayment(response)
    }

    "successfully post makeExternalTransaction with sms confirmation" in {
      val method = yandexKassaV3SberbankOnlineMethod
      val request = PaymentRequest.Source.getDefaultInstance

      val response = Form
        .newBuilder()
        .setId("295b5a6c-5cc3-473c-9f0a-1e7cedaa5fcf")
        .setFields(
          Form.Fields
            .newBuilder()
            .addFields(Form.Field.newBuilder().setKey("action").setValue("https://demomoney.yandex.ru/eshop.xml"))
        )
        .setConfirmationType(Confirmation.Type.external)
        .build()

      expectYandexKassaV3(method, request)
      httpClient.respondWith(response)

      val res = client.makeExternalTransaction(uid, None, None, method, request)(Traced.empty).futureValue
      res shouldBe SuccessfulPayment(response)
    }

    "handle 500 CancellationPaymentError error for post makeExternalTransaction" in {
      val method = yandexKassaV3BankCardMethod
      val request = PaymentRequest.Source.getDefaultInstance

      val apiError = ApiError
        .newBuilder()
        .setPaymentCancellationError(CancellationPaymentError.INVALID_CARD_NUMBER)
        .build()

      expectYandexKassaV3(method, request)
      httpClient.respondWith(StatusCodes.InternalServerError, apiError)

      val res = client.makeExternalTransaction(uid, None, None, method, request)(Traced.empty).futureValue
      res shouldBe FailedPayment(apiError)
    }

    "handle 500 ServerError for post makeExternalTransaction" in {
      val method = yandexKassaV3BankCardMethod
      val request = PaymentRequest.Source.getDefaultInstance

      val apiError = ApiError
        .newBuilder()
        .setServerError(ApiError.ServerError.INTERNAL_SERVER_ERROR)
        .build()

      expectYandexKassaV3(method, request)
      httpClient.respondWith(StatusCodes.InternalServerError, apiError)

      val res = client.makeExternalTransaction(uid, None, None, method, request)(Traced.empty).futureValue
      res shouldBe FailedPayment(apiError)
    }

    "successfully put makeWalletTransaction" in {
      val accountId = "4007216463_1538669490997"
      val request = AccountConsumeRequest.newBuilder().setAccount(accountId).build()

      val response = Transaction.getDefaultInstance

      expectAccountConsume(accountId, request)
      httpClient.respondWith(StatusCodes.OK, response)

      val res = client.makeWalletTransaction(uid, None, request).futureValue
      res shouldBe SuccessfulPayment(response)
    }

    "handle 402 out makeWalletTransaction" in {
      val accountId = "4007216463_1538669490997"
      val request = AccountConsumeRequest.newBuilder().setAccount(accountId).build()
      val apiError = ApiError.newBuilder().setConsumeError(ApiError.ConsumeError.NOT_ENOUGH_FUNDS).build()

      expectAccountConsume(accountId, request)
      httpClient.respondWith(StatusCodes.PaymentRequired, apiError)

      val res = client.makeWalletTransaction(uid, None, request).futureValue
      res shouldBe FailedPayment(apiError)
    }

    def expectYandexKassaV3(method: BankerPaymentMethod, request: PaymentRequest.Source): Unit = {
      val gate = method.getPsId.name()
      val paymentMethod = method.getId
      httpClient.expect(POST, s"/api/1.x/service/realty/customer/$uid/payment/$gate/method/$paymentMethod")
      httpClient.expectProto(request)
    }

    def yandexKassaV3BankCardMethod: BankerPaymentMethod = {
      BankerPaymentMethod
        .newBuilder()
        .setPsId(PaymentSystemId.YANDEXKASSA_V3)
        .setId("bank_card")
        .setName("Банковская карта")
        .build()
    }

    def yandexKassaV3SberbankOnlineMethod: BankerPaymentMethod = {
      BankerPaymentMethod
        .newBuilder()
        .setPsId(PaymentSystemId.YANDEXKASSA_V3)
        .setId("sberbank")
        .setName("Сбербанк Онлайн")
        .build()
    }

    def expectAccountConsume(accountId: String, request: AccountConsumeRequest): Unit = {
      httpClient.expect(PUT, s"/api/1.x/service/realty/customer/$uid/account/$accountId/consume")
      httpClient.expectProto(request)
    }
  }
}
