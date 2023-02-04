package ru.auto.api.services.billing

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.auth.Application.moderation
import ru.auto.api.exceptions.{ApiException, BankerBadRequest, InvalidEmailFormatException}
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.ModelGenerators.{PrivateUserRefGen, ReadableStringGen}
import ru.auto.api.model.gen.BankerModelGenerators._
import ru.auto.api.services.billing.BankerClient.{BankerDomains, PaymentMethodFilter}
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.StringUtils._
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.banker.model.ApiModel.ApiError
import ru.yandex.vertis.banker.model.ApiModel.ApiError.{BadRequestDetailError, ConsumeError, NotFoundDetailError}
import ru.yandex.vertis.util.strings.ifNonEmpty

/**
  * @author alex-kovalenko
  */
class DefaultBankerClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with TestRequest {

  val bankerClient = new DefaultBankerClientImpl(http)

  implicit override val request: Request = super.request

  val moderationRequest: Request = {
    val r = new RequestImpl
    r.setApplication(moderation)
    r
  }

  private val DefaultPaymentRequestErrorDescriptionRu: String = "Техническая ошибка оплаты"

  private val StatusCodesToBeEnrichedGen = Gen.oneOf(Seq(StatusCodes.NotFound, StatusCodes.BadGateway))

  private val ErrorDefaultErrorDescriptionRuProducerGen: Gen[ApiError] = {
    val remoteErrorsGen = RemoteErrorGen.map(r => r.toBuilder.setDescriptionRu("").build)

    val accountNotFoundError = ApiError
      .newBuilder()
      .setNotFoundDetailError(NotFoundDetailError.ACCOUNT_NOT_FOUND)
      .build()

    val unknownConsumeError = ApiError
      .newBuilder()
      .setConsumeError(ConsumeError.UNKNOWN_CONSUME_ERROR)
      .build()

    Gen.oneOf(remoteErrorsGen, Gen.const(accountNotFoundError), Gen.const(unknownConsumeError))
  }

  "DefaultBankerClient" should {
    "get account info" in {
      forAll(PrivateUserRefGen, ReadableStringGen, AccountInfoGen) { (user, accountId, info) =>
        http.expectUrl(GET, url"/api/1.x/service/autoru/customer/$user/account/$accountId/info")
        http.respondWith(info)

        val response = bankerClient.getAccountInfo(user, accountId).futureValue
        response shouldBe info
      }
    }

    "get account info for moderation" in {
      forAll(PrivateUserRefGen, ReadableStringGen, AccountInfoGen) { (user, accountId, info) =>
        http.expectUrl(GET, url"/api/1.x/service/autoru/customer/$user/account/$accountId/info")
        http.respondWith(info)

        val response = bankerClient.getAccountInfo(user, accountId)(moderationRequest).futureValue
        response shouldBe info
      }
    }

    "get accounts" in {
      forAll(PrivateUserRefGen, list(0, 10, AccountGen)) { (user, accounts) =>
        http.expectUrl(GET, url"/api/1.x/service/autoru/customer/$user/account")

        http.respondWithMany(accounts)

        val response = bankerClient.getAccounts(user).futureValue
        response shouldBe accounts
      }
    }

    "update account" in {
      forAll(AccountGen, AccountPatchGen, AccountGen) { (account, patch, updatedAccount) =>
        http.expectUrl(PUT, url"/api/1.x/service/autoru/customer/${account.getUser}/account/${account.getId}")
        http.expectProto(patch)

        http.respondWith(updatedAccount)

        val result = bankerClient.updateAccount(account, patch).futureValue
        result shouldBe updatedAccount
      }
    }

    "create account" in {
      forAll(PrivateUserRefGen, AccountGen) { (user, account) =>
        http.expectUrl(POST, url"/api/1.x/service/autoru/customer/$user/account")
        http.expectProto(account)

        http.respondWith(account)

        val result = bankerClient.createAccount(user, account).futureValue
        result shouldBe account
      }
    }

    "fail on create account with invalid email" in {
      forAll(PrivateUserRefGen, AccountGen) { (user, account) =>
        http.expectUrl(POST, url"/api/1.x/service/autoru/customer/$user/account")
        http.expectProto(account)

        val invalidEmail = ApiError
          .newBuilder()
          .setBadRequestDetailError(BadRequestDetailError.EMAIL_BAD_FORMAT)
          .build()
        http.respondWith(StatusCodes.BadRequest, invalidEmail)

        bankerClient.createAccount(user, account).failed.futureValue shouldBe an[InvalidEmailFormatException]
      }
    }

    "fail on update account with invalid email" in {
      forAll(AccountGen, AccountPatchGen) { (account, patch) =>
        http.expectUrl(PUT, url"/api/1.x/service/autoru/customer/${account.getUser}/account/${account.getId}")
        http.expectProto(patch)

        val expectedDescriptionRu = "Некорректный адрес электронной почты"

        val invalidEmail = ApiError
          .newBuilder()
          .setBadRequestDetailError(BadRequestDetailError.EMAIL_BAD_FORMAT)
          .setDescriptionRu(expectedDescriptionRu)
          .build()
        http.respondWith(StatusCodes.BadRequest, invalidEmail)

        bankerClient.updateAccount(account, patch).failed.futureValue match {
          case e: InvalidEmailFormatException if e.descriptionRu.contains(expectedDescriptionRu) =>
            ()
          case other =>
            fail(s"Unexpected $other exception")
        }

      }
    }

    "request payment methods" in {
      forAll(PrivateUserRefGen, PaymentRequestSourceGen, list(0, 5, PaymentMethodGen)) { (user, source, methods) =>
        http.expectUrl(POST, url"/api/1.x/service/autoru/customer/$user/payment")
        http.expectProto(source)
        http.respondWithMany(methods)

        val result = bankerClient.requestPaymentMethods(user, source).futureValue
        result should contain theSameElementsAs methods
      }
    }

    "request payment" in {
      forAll(PaymentRequestSourceGen, PaymentSystemGen, readableString, PrivateUserRefGen, PaymentRequestFormGen) {
        (source, psId, methodId, user, form) =>
          http.expectUrl(POST, url"/api/1.x/service/autoru/customer/$user/payment/$psId/method/$methodId")
          http.expectProto(source)
          http.respondWith(form)

          val result = bankerClient.requestPayment(user, psId, methodId, source, yandexPassportId = None).futureValue
          result shouldBe form
      }
    }

    "fail on request payment" in {
      forAll(PaymentRequestSourceGen, PaymentSystemGen, readableString, PrivateUserRefGen, BadRequestDetailErrorGen) {
        (source, psId, methodId, user, error) =>
          http.expectUrl(POST, url"/api/1.x/service/autoru/customer/$user/payment/$psId/method/$methodId")
          http.expectProto(source)
          http.respondWith(StatusCodes.BadRequest, error)

          bankerClient.requestPayment(user, psId, methodId, source, yandexPassportId = None).failed.futureValue match {
            case e: InvalidEmailFormatException =>
              e.descriptionRu shouldBe ifNonEmpty(error.getDescriptionRu)
            case e: BankerBadRequest =>
              e.descriptionRu shouldBe ifNonEmpty(error.getDescriptionRu)
            case other =>
              fail(s"Unexpected $other exception")
          }
      }
    }

    "fail with banker bad request on request payment" in {
      forAll(PaymentRequestSourceGen, PaymentSystemGen, readableString, PrivateUserRefGen, BadRequestDetailErrorGen) {
        (source, psId, methodId, user, error) =>
          http.expectUrl(POST, url"/api/1.x/service/autoru/customer/$user/payment/$psId/method/$methodId")
          http.expectProto(source)
          http.respondWith(StatusCodes.BadRequest, error)

          bankerClient.requestPayment(user, psId, methodId, source, yandexPassportId = None).failed.futureValue match {
            case e: InvalidEmailFormatException
                if error.getBadRequestDetailError == BadRequestDetailError.EMAIL_BAD_FORMAT =>
              e.descriptionRu shouldBe ifNonEmpty(error.getDescriptionRu)
            case e: BankerBadRequest =>
              e.descriptionRu shouldBe ifNonEmpty(error.getDescriptionRu)
            case other =>
              fail(s"Unexpected $other exception")
          }
      }
    }

    "request payment handle errors card errors" in {
      forAll(CardErrorGen, PaymentRequestSourceGen, PaymentSystemGen, readableString, PrivateUserRefGen) {
        (error, source, psId, methodId, user) =>
          http.expectUrl(POST, url"/api/1.x/service/autoru/customer/$user/payment/$psId/method/$methodId")
          http.expectProto(source)
          http.respondWithProto(StatusCodes.PaymentRequired, error)

          bankerClient.requestPayment(user, psId, methodId, source, yandexPassportId = None).failed.futureValue match {
            case e: ApiException =>
              e.descriptionRu shouldBe ifNonEmpty(error.getDescriptionRu)
            case other =>
              fail(s"Unexpected $other exception")
          }
      }
    }

    "request payment handle errors from CancellationPaymentError" in {
      forAll(CancellationErrorGen, PaymentRequestSourceGen, PaymentSystemGen, readableString, PrivateUserRefGen) {
        (error, source, psId, methodId, user) =>
          http.expectUrl(POST, url"/api/1.x/service/autoru/customer/$user/payment/$psId/method/$methodId")
          http.expectProto(source)
          http.respondWith(StatusCodes.PaymentRequired, error)

          bankerClient.requestPayment(user, psId, methodId, source, yandexPassportId = None).failed.futureValue match {
            case e: ApiException =>
              e.descriptionRu shouldBe ifNonEmpty(error.getDescriptionRu)
            case other =>
              fail(s"Unexpected $other exception")
          }
      }
    }

    "request payment handle error and enrich needed codes with description in russian" in {
      forAll(
        PaymentRequestSourceGen,
        PaymentSystemGen,
        readableString,
        PrivateUserRefGen,
        StatusCodesToBeEnrichedGen,
        ErrorDefaultErrorDescriptionRuProducerGen
      ) { (source, psId, methodId, user, code, error) =>
        http.expectUrl(POST, url"/api/1.x/service/autoru/customer/$user/payment/$psId/method/$methodId")
        http.expectProto(source)
        http.respondWith(code, error)

        bankerClient.requestPayment(user, psId, methodId, source, yandexPassportId = None).failed.futureValue match {
          case e: ApiException =>
            e.descriptionRu shouldBe Some(DefaultPaymentRequestErrorDescriptionRu)
          case other =>
            fail(s"Unexpected $other exception")
        }
      }
    }

    "get payment methods" when {
      "got All filter" in {
        forAll(PrivateUserRefGen, list(0, 5, PaymentMethodGen)) { (user, methods) =>
          http.expectUrl(GET, url"/api/1.x/service/autoru/customer/$user/method")
          http.respondWithMany(methods)

          val result =
            bankerClient.getPaymentMethods(user, PaymentMethodFilter.All, yandexPassportId = None).futureValue
          result should contain theSameElementsAs methods
        }
      }

      "got OfPaymentSystem filter" in {
        forAll(PrivateUserRefGen, PaymentSystemGen, list(0, 5, PaymentMethodGen)) { (user, psId, methods) =>
          http.expectUrl(GET, url"/api/1.x/service/autoru/customer/$user/method/gate/$psId")
          http.respondWithMany(methods)

          val result = bankerClient
            .getPaymentMethods(user, PaymentMethodFilter.ForPaymentSystem(psId), yandexPassportId = None)
            .futureValue
          result should contain theSameElementsAs methods
        }
      }
      "got ForMethodName filter" in {
        forAll(PrivateUserRefGen, PaymentSystemGen, readableString, list(0, 5, PaymentMethodGen)) {
          (user, psId, method, methods) =>
            http.expectUrl(GET, url"/api/1.x/service/autoru/customer/$user/method/gate/$psId/method/$method")
            http.respondWithMany(methods)

            val result = bankerClient
              .getPaymentMethods(
                user,
                PaymentMethodFilter.ForMethodName(psId, method),
                yandexPassportId = None
              )
              .futureValue
            result should contain theSameElementsAs methods
        }
      }
    }

    "pay by account" in {
      forAll(PrivateUserRefGen, readableString, AccountConsumeRequestGen, TransactionGen) {
        (user, accountId, consumeRequest, transaction) =>
          // there is agreement about using user.toPlain as accountId
          http.expectUrl(PUT, url"/api/1.x/service/autoru/customer/$user/account/$accountId/consume")
          http.expectProto(consumeRequest)
          http.respondWith(transaction)

          val result = bankerClient.payByAccount(user, accountId, consumeRequest).futureValue
          result shouldBe transaction
      }
    }

    "pay by account handle ConsumeError.NOT_ENOUGH_FUNDS" in {
      forAll(PrivateUserRefGen, readableString, AccountConsumeRequestGen, ConsumeErrorGen) {
        (user, accountId, consumeRequest, error) =>
          // there is agreement about using user.toPlain as accountId
          http.expectUrl(PUT, url"/api/1.x/service/autoru/customer/$user/account/$accountId/consume")
          http.expectProto(consumeRequest)
          http.respondWith(402, error)

          bankerClient.payByAccount(user, accountId, consumeRequest).failed.futureValue match {
            case e: ApiException =>
              e.descriptionRu shouldBe ifNonEmpty(error.getDescriptionRu)
            case other =>
              fail(s"Unexpected $other exception")
          }
      }
    }

    "pay by account handle error and enrich needed codes with description in russian" in {
      forAll(
        PrivateUserRefGen,
        readableString,
        AccountConsumeRequestGen,
        StatusCodesToBeEnrichedGen,
        ErrorDefaultErrorDescriptionRuProducerGen
      ) { (user, accountId, consumeRequest, code, error) =>
        // there is agreement about using user.toPlain as accountId
        http.expectUrl(PUT, url"/api/1.x/service/autoru/customer/$user/account/$accountId/consume")
        http.expectProto(consumeRequest)
        http.respondWith(code, error)

        bankerClient.payByAccount(user, accountId, consumeRequest).failed.futureValue match {
          case e: ApiException =>
            e.descriptionRu shouldBe Some(DefaultPaymentRequestErrorDescriptionRu)
          case other =>
            fail(s"Unexpected $other exception")
        }
      }
    }

    "update payment method" in {
      forAll(PrivateUserRefGen, PaymentSystemGen, CardMaskGen, PaymentMethodPatchGen) { (user, psId, mask, patch) =>
        http.expectUrl(PUT, url"/api/1.x/service/autoru/customer/$user/method/gate/$psId/id/$mask")
        http.respondWithStatus(StatusCodes.OK)

        noException should be thrownBy bankerClient
          .updateMethod(user, psId, mask, patch, BankerDomains.Autoru)
          .futureValue

      }
    }
  }
}
