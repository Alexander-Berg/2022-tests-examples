package ru.auto.salesman.service.banker

import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import ru.auto.salesman.Task
import ru.auto.salesman.client.banker.BankerClient
import ru.auto.salesman.dao.user.TransactionDao
import ru.auto.salesman.dao.user.TransactionDao.Filter
import ru.auto.salesman.model.{
  AutoruUser,
  DeprecatedDomain,
  DeprecatedDomains,
  Funds,
  UserId
}
import ru.auto.salesman.service.PassportService
import ru.auto.salesman.service.banker.domain.{
  PayByAccountResponse,
  PayRecurrentResponse,
  PayWithLinkedCardRequest,
  PayWithLinkedCardResponse
}
import ru.auto.salesman.service.user.UserFeatureService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.BankerApiModelGenerators
import ru.auto.salesman.test.model.gens.{userEssentialsGen, BasicSalesmanGenerators}
import ru.auto.salesman.util.CacheControl.NoCache
import ru.auto.salesman.util.sttp.SttpProtobufSupport.SttpProtoException.{
  BadRequestException,
  NotFoundException,
  PaymentRequiredException,
  UnexpectedResponseException
}
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.passport.model.api.ApiModel.{UserEssentials, UserSocialProfile}
import ru.yandex.vertis.SocialProvider
import ru.yandex.vertis.banker.model.ApiModel._
import zio.test.environment.TestEnvironment

import scala.collection.JavaConverters.{asScalaBufferConverter, mapAsScalaMapConverter}

class BankerServiceImplSpec extends BaseSpec with BankerApiModelGenerators {

  import BankerServiceImpl._
  import BankerServiceImplSpec._

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  implicit val rc: RequestContext = AutomatedContext("unit-test", NoCache)

  private val bankerClient = mock[BankerClient]
  private val passportService = mock[PassportService]
  private val transactionDao = mock[TransactionDao]
  private val featureService = mock[UserFeatureService]

  val service =
    new BankerServiceImpl(bankerClient, passportService, transactionDao, featureService)

  private def mockFeatureToggleOn() =
    (featureService.useTrustGate _)
      .expects()
      .returning(true)

  private def mockFeatureToggleOff() =
    (featureService.useTrustGate _)
      .expects()
      .returning(false)

  private def mockGetAccountInfo =
    toMockFunction2(
      bankerClient.getAccountInfo(_: UserId, _: String)
    )

  private def mockGetYandexPassportId =
    toMockFunction1(
      passportService.userEssentials(_: Long)
    )

  private def mockGetTransaction =
    toMockFunction2(
      transactionDao.get(_: Filter, _: Boolean)
    )

  private def mockGetAttachedCards =
    toMockFunction3(
      bankerClient.getCards(
        _: UserId,
        _: PaymentSystemId,
        _: Option[String]
      )
    )

  private def mockPayRecurrent =
    toMockFunction3(
      bankerClient.payRecurrentWithTrust(
        _: UserId,
        _: RecurrentPaymentSource,
        _: String
      )
    )

  private def mockPay =
    toMockFunction4(
      bankerClient.pay(
        _: UserId,
        _: PaymentMethod,
        _: PaymentRequest.Source,
        _: Option[String]
      )
    )

  private def mockExecuteAccountRequest =
    toMockFunction2(
      bankerClient.executeAccountRequest(
        _: UserId,
        _: AccountConsumeRequest
      )
    )

  "BankerApi" when {
    "getAccount" should {
      "pass response from client" in {
        val clientResponseGen: Gen[Task[Option[Account]]] =
          Gen.oneOf(
            Task.fail(new Exception()),
            Task.fail(new RuntimeException())
          )
        forAll(readableString, clientResponseGen) { (user, response) =>
          (bankerClient
            .getAccount(_: UserId))
            .expects(user)
            .returning(response)
          service.getAccount(user).failure.cause.failures.size shouldBe 1

        }
      }
    }

    "createAccount" should {
      "pass response from client" in {
        val clientResponseGen: Gen[Task[Account]] =
          Gen.oneOf(
            Task.succeed(Account.getDefaultInstance),
            Task.succeed(Account.getDefaultInstance)
          )
        forAll(readableString, clientResponseGen) { (user, response) =>
          val expectedAccount =
            Account.newBuilder().setId(user).setUser(user).build()
          (bankerClient
            .createAccount(_: Account))
            .expects(expectedAccount)
            .returning(response)
          service.createAccount(user).unsafeRunToTry() shouldBe response
            .unsafeRunToTry()
        }
      }
      "pass forward error from client" in {
        val clientResponseGen: Gen[Task[Account]] =
          Gen.oneOf(
            Task.fail(new RuntimeException),
            Task.fail(new Exception)
          )
        forAll(readableString, clientResponseGen) { (user, response) =>
          val expectedAccount =
            Account.newBuilder().setId(user).setUser(user).build()
          (bankerClient
            .createAccount(_: Account))
            .expects(expectedAccount)
            .returning(response)
          service.createAccount(user).failure.cause.failures.length shouldBe 1
        }
      }
    }

    "payRecurrent" should {
      val socialUserId = readableString.next
      val userEssentials = UserEssentials
        .newBuilder()
        .setId(readableString.next)
        .setEmail(readableString.next)
        .addSocialProfiles(
          UserSocialProfile
            .newBuilder()
            .setProvider(SocialProvider.YANDEX)
            .setSocialUserId(socialUserId)
        )
        .build()

      "pay by trust" in {
        forAll(
          PayRecurrentRequestGen
        ) { request =>
          mockGetYandexPassportId
            .expects(AutoruUser(request.account.getUser).id)
            .returningZ(userEssentials)
          mockPayRecurrent
            .expects(
              request.account.getUser,
              asRecurrentPaymentRequestSource(request),
              socialUserId
            )
            .returningZ(CorrectRecurrentPaymentResponse)

          service
            .payRecurrent(request)
            .provideRc(rc)
            .success
            .value shouldBe PayRecurrentResponse.Accepted(
            CorrectRecurrentPaymentResponse.getStatus.name()
          )
        }
      }

      "fail with RecurrentError if http Bad Request" in {
        forAll(
          PayRecurrentRequestGen
        ) { request =>
          mockGetYandexPassportId
            .expects(AutoruUser(request.account.getUser).id)
            .returningZ(userEssentials)
          mockPayRecurrent
            .expects(
              request.account.getUser,
              asRecurrentPaymentRequestSource(request),
              socialUserId
            )
            .throwingZ(BadRequestException("/test", Some("test"), "test".getBytes()))

          service
            .payRecurrent(request)
            .provideRc(rc)
            .success
            .value shouldBe PayRecurrentResponse.RecurrentError
        }
      }

      "fail with RecurrentError if http Not Found" in {
        forAll(
          PayRecurrentRequestGen
        ) { request =>
          mockGetYandexPassportId
            .expects(AutoruUser(request.account.getUser).id)
            .returningZ(userEssentials)
          mockPayRecurrent
            .expects(
              request.account.getUser,
              asRecurrentPaymentRequestSource(request),
              socialUserId
            )
            .throwingZ(NotFoundException("/test", Some("test"), "test".getBytes()))

          service
            .payRecurrent(request)
            .provideRc(rc)
            .success
            .value shouldBe PayRecurrentResponse.RecurrentError
        }
      }

      "fail task if other http error" in {
        forAll(
          PayRecurrentRequestGen
        ) { request =>
          val ex =
            UnexpectedResponseException("/test", 500, Some("test"), "test".getBytes())
          mockGetYandexPassportId
            .expects(AutoruUser(request.account.getUser).id)
            .returningZ(userEssentials)
          mockPayRecurrent
            .expects(
              request.account.getUser,
              asRecurrentPaymentRequestSource(request),
              socialUserId
            )
            .throwingZ(ex)

          service
            .payRecurrent(request)
            .provideRc(rc)
            .failure
            .exception shouldBe ex
        }
      }
    }

    "payByAccount" should {
      "fail if the balance is less than amount" in {
        forAll(PayByAccountRequestGen) { request =>
          inSequence {
            mockGetAccountInfo
              .expects(request.account.getUser, request.account.getId)
              .returningZ(accountInfo(request.amount - 1))
            mockExecuteAccountRequest.expects(*, *).never()
          }
          service
            .payByAccount(request)
            .success
            .value shouldBe a[PayByAccountResponse.NotEnoughFunds]
        }
      }

      "fail on banker PaymentRequiredException" in {
        forAll(PayByAccountRequestGen) { request =>
          inSequence {
            mockGetAccountInfo
              .expects(request.account.getUser, request.account.getId)
              .returningZ(accountInfo(request.amount + 1))
            mockExecuteAccountRequest
              .expects(*, *)
              .throwingZ(NoEnoughFundsException)
          }
          service
            .payByAccount(request)
            .success
            .value shouldBe a[PayByAccountResponse.NotEnoughFunds]
        }
      }

      "pass executeAccountRequest result" in {
        forAll(PayByAccountRequestGen) { request =>
          inSequence {
            mockGetAccountInfo
              .expects(request.account.getUser, request.account.getId)
              .returningZ(accountInfo(request.amount))
            mockExecuteAccountRequest
              .expects(assertArgs { (user, acr) =>
                user shouldBe user
                acr.getAccount shouldBe request.account.getId
                acr.getAmount shouldBe request.amount
              })
              .returningZ(Transaction.getDefaultInstance)
          }

          service
            .payByAccount(request)
            .provideRc(rc)
            .provideLayer(zio.ZEnv.live >>> TestEnvironment.live)
            .success
            .value shouldBe PayByAccountResponse.Paid(
            Transaction.getDefaultInstance
          )
        }
      }

      "pass the same request id on two equal requests to make payment idempotent" in {
        forAll(PayByAccountRequestGen) { request =>
          mockGetAccountInfo
            .expects(*, *)
            .returningZ(accountInfo(request.amount))
            .twice()
          mockExecuteAccountRequest
            .expects {
              assertArgs { (_, consumeRequest) =>
                consumeRequest.getId shouldBe request.payload.transactionId
              }
            }
            .returningZ(Transaction.getDefaultInstance)
            .twice()
          service.payByAccount(request).provideRc(rc).success.value
          service.payByAccount(request).provideRc(rc).success.value
        }
      }
    }

    "pay with tied card" should {
      "fail if there is no tied card" in {
        forAll(PayWithTiedCardRequestGen, userEssentialsGen) {
          (request, userEssentials) =>
            mockGetYandexPassportId
              .expects(AutoruUser(request.account.getUser).id)
              .returningZ(userEssentials)
            mockFeatureToggleOff()
            mockGetAttachedCards
              .expects(
                request.account.getUser,
                PaymentSystemId.YANDEXKASSA_V3,
                None
              )
              .returningZ(List.empty)
            service
              .payWithTiedCard(request)
              .provideRc(rc)
              .success
              .value shouldBe PayWithLinkedCardResponse.NoLinkedCards
        }
      }

      "pay with tied card" in {
        forAll(PayWithTiedCardRequestGen, userEssentialsGen, AnyTiedCardGen) {
          (request, userEssentials, card) =>
            mockGetYandexPassportId
              .expects(AutoruUser(request.account.getUser).id)
              .returningZ(userEssentials)
            mockFeatureToggleOff()
            mockGetAttachedCards
              .expects(
                request.account.getUser,
                PaymentSystemId.YANDEXKASSA_V3,
                None
              )
              .returningZ(List(card))
            mockPay
              .expects(
                request.account.getUser,
                card,
                asPaymentRequestSource(request, card),
                None
              )
              .returningZ(CorrectForm)

            service
              .payWithTiedCard(request)
              .provideRc(rc)
              .success
              .value shouldBe PayWithLinkedCardResponse.Paid(CorrectForm.getId)
        }
      }

      "pay with tied card trust" in {
        forAll(
          PayWithTiedCardRequestGen,
          readableString,
          transactionRecordGen(),
          TrustTiedCardGen
        ) { (request, socialUserId, transaction, card) =>
          val userEssentials = UserEssentials
            .newBuilder()
            .setId(readableString.next)
            .setEmail(readableString.next)
            .addSocialProfiles(
              UserSocialProfile
                .newBuilder()
                .setProvider(SocialProvider.YANDEX)
                .setSocialUserId(socialUserId)
            )
            .build()
          val transactionTrust =
            transaction.copy(bankerTransactionId = Some("8#" + readableString.next))

          mockGetYandexPassportId
            .expects(AutoruUser(request.account.getUser).id)
            .returningZ(userEssentials)
          mockFeatureToggleOn()
          mockGetTransaction
            .expects(Filter.ForTransactionId(request.parentTransactionId.get), false)
            .returningZ(List(transactionTrust))
          mockGetAttachedCards
            .expects(
              request.account.getUser,
              PaymentSystemId.TRUST,
              Some(socialUserId)
            )
            .returningZ(List(card))
          mockPay
            .expects(
              request.account.getUser,
              card,
              asPaymentRequestSource(request, card),
              Some(socialUserId)
            )
            .returningZ(CorrectForm)

          service
            .payWithTiedCard(request)
            .provideRc(rc)
            .success
            .value shouldBe PayWithLinkedCardResponse.Paid(CorrectForm.getId)
        }
      }

      "pay by yandex kassa if banker_transaction_id not equal trust" in {
        forAll(
          PayWithTiedCardRequestGen,
          readableString,
          transactionRecordGen(),
          AnyTiedCardGen
        ) { (request, socialUserId, transaction, card) =>
          val userEssentials = UserEssentials
            .newBuilder()
            .setId(readableString.next)
            .setEmail(readableString.next)
            .addSocialProfiles(
              UserSocialProfile
                .newBuilder()
                .setProvider(SocialProvider.YANDEX)
                .setSocialUserId(socialUserId)
            )
            .build()

          mockGetYandexPassportId
            .expects(AutoruUser(request.account.getUser).id)
            .returningZ(userEssentials)
          mockFeatureToggleOn()
          mockGetTransaction
            .expects(Filter.ForTransactionId(request.parentTransactionId.get), false)
            .returningZ(List(transaction))
          mockGetAttachedCards
            .expects(
              request.account.getUser,
              PaymentSystemId.YANDEXKASSA_V3,
              Some(socialUserId)
            )
            .returningZ(List(card))
          mockPay
            .expects(
              request.account.getUser,
              card,
              asPaymentRequestSource(request, card),
              Some(socialUserId)
            )
            .returningZ(CorrectForm)

          service
            .payWithTiedCard(request)
            .provideRc(rc)
            .success
            .value shouldBe PayWithLinkedCardResponse.Paid(CorrectForm.getId)
        }
      }

      "not fail if passport error" in {
        forAll(PayWithTiedCardRequestGen, AnyTiedCardGen) { (request, card) =>
          mockGetYandexPassportId
            .expects(AutoruUser(request.account.getUser).id)
            .throwingZ(new RuntimeException)
          mockFeatureToggleOff()
          mockGetAttachedCards
            .expects(
              request.account.getUser,
              PaymentSystemId.YANDEXKASSA_V3,
              None
            )
            .returningZ(List(card))
          mockPay
            .expects(
              request.account.getUser,
              card,
              asPaymentRequestSource(request, card),
              None
            )
            .returningZ(CorrectForm)

          service
            .payWithTiedCard(request)
            .provideRc(rc)
            .success
            .value shouldBe PayWithLinkedCardResponse.Paid(CorrectForm.getId)
        }
      }

      "pass the same request id on two equal requests to make payment idempotent" in {
        forAll(PayWithTiedCardRequestGen, userEssentialsGen, AnyTiedCardGen) {
          (request, userEssentials, card) =>
            mockGetYandexPassportId
              .expects(AutoruUser(request.account.getUser).id)
              .returningZ(userEssentials)
              .twice()
            mockFeatureToggleOff()
              .twice()
            mockGetAttachedCards
              .expects(*, *, *)
              .returningZ(List(card))
              .twice()
            mockPay
              .expects {
                assertArgs { (_, _, paymentRequest, _) =>
                  paymentRequest.getOptions.getId shouldBe request.payload.transactionId
                }
              }
              .returningZ(CorrectForm)
              .twice()
            service.payWithTiedCard(request).provideRc(rc).success.value
            service.payWithTiedCard(request).provideRc(rc).success.value
        }
      }

      "not fail in case of payment system exceptions" in {
        forAll(PayWithTiedCardRequestGen, userEssentialsGen, AnyTiedCardGen) {
          (request, userEssentials, card) =>
            val apiError = ApiError
              .newBuilder()
              .setCardError(
                ApiError.RecurrentPaymentError.CARD_HAS_NO_ENOUGH_FUNDS
              )
              .build()
            inSequence {
              mockGetYandexPassportId
                .expects(AutoruUser(request.account.getUser).id)
                .returningZ(userEssentials)
              mockFeatureToggleOff()
              mockGetAttachedCards
                .expects(
                  request.account.getUser,
                  PaymentSystemId.YANDEXKASSA_V3,
                  None
                )
                .returningZ(List(card))
              mockPay
                .expects(*, *, *, *)
                .throwingZ(NoEnoughFundsException)
            }

            service
              .payWithTiedCard(request)
              .provideRc(rc)
              .success
              .value shouldBe PayWithLinkedCardResponse.PaymentError(
              List(apiError)
            )
        }
      }

      "iterate over preferred cards" in {
        forAll(
          PayWithTiedCardRequestGen,
          userEssentialsGen,
          listNUnique(5, PreferredTiedCardGen)(
            _.getProperties.getCard.getCddPanMask
          )
        ) { (request, userEssentials, cards) =>
          inSequence {
            mockGetYandexPassportId
              .expects(AutoruUser(request.account.getUser).id)
              .returningZ(userEssentials)
            mockFeatureToggleOff()
            mockGetAttachedCards
              .expects(
                request.account.getUser,
                PaymentSystemId.YANDEXKASSA_V3,
                None
              )
              .returningZ(cards)
            cards.foreach { card =>
              mockPay
                .expects(
                  request.account.getUser,
                  card,
                  asPaymentRequestSource(request, card),
                  None
                )
                .throwingZ(NoEnoughFundsException)
            }
          }

          service
            .payWithTiedCard(request)
            .provideRc(rc)
            .success
            .value shouldBe a[PayWithLinkedCardResponse.PaymentError]
        }
      }

      "use preferred card if present" in {
        forAll(
          PayWithTiedCardRequestGen,
          userEssentialsGen,
          PreferredTiedCardGen,
          NotPreferredTiedCardGen
        ) { (request, userEssentials, preferred, other) =>
          val cards = List(preferred, other)
          inSequence {
            mockGetYandexPassportId
              .expects(AutoruUser(request.account.getUser).id)
              .returningZ(userEssentials)
            mockFeatureToggleOff()
            mockGetAttachedCards
              .expects(
                request.account.getUser,
                PaymentSystemId.YANDEXKASSA_V3,
                None
              )
              .returningZ(cards)
            cards.foreach { card =>
              mockPay
                .expects(
                  request.account.getUser,
                  card,
                  asPaymentRequestSource(request, card),
                  None
                )
                .throwingZ(NoEnoughFundsException)
            }
          }

          service
            .payWithTiedCard(request)
            .provideRc(rc)
            .success
            .value shouldBe a[PayWithLinkedCardResponse.PaymentError]
        }
      }

      "use last not preferred if there is not preferred cards" in {
        forAll(
          PayWithTiedCardRequestGen,
          userEssentialsGen,
          listNUnique(3, NotPreferredTiedCardGen)(
            _.getProperties.getCard.getCddPanMask
          )
        ) { (request, userEssentials, orderedCards) =>
          val (preferred, other) =
            orderedCards.partition(_.getPreferred.getValue)
          val cards = preferred ++ other

          inSequence {
            mockGetYandexPassportId
              .expects(AutoruUser(request.account.getUser).id)
              .returningZ(userEssentials)
            mockFeatureToggleOff()
            mockGetAttachedCards
              .expects(
                request.account.getUser,
                PaymentSystemId.YANDEXKASSA_V3,
                None
              )
              .returningZ(cards)
            cards.foreach { card =>
              mockPay
                .expects(
                  request.account.getUser,
                  card,
                  asPaymentRequestSource(request, card),
                  None
                )
                .throwingZ(NoEnoughFundsException)
            }
          }

          service
            .payWithTiedCard(request)
            .provideRc(rc)
            .success
            .value shouldBe a[PayWithLinkedCardResponse.PaymentError]
        }
      }

      "fail if failed to pay with tied card" in {
        forAll(PayWithTiedCardRequestGen, userEssentialsGen, AnyTiedCardGen) {
          (request, userEssentials, card) =>
            val ex = new RuntimeException
            inSequence {
              mockGetYandexPassportId
                .expects(AutoruUser(request.account.getUser).id)
                .returningZ(userEssentials)
              mockFeatureToggleOff()
              mockGetAttachedCards
                .expects(
                  request.account.getUser,
                  PaymentSystemId.YANDEXKASSA_V3,
                  None
                )
                .returningZ(List(card))
              mockPay
                .expects(
                  request.account.getUser,
                  card,
                  asPaymentRequestSource(request, card),
                  None
                )
                .throwingZ(ex)
            }

            service
              .payWithTiedCard(request)
              .provideRc(rc)
              .failure
              .exception shouldBe ex
        }
      }

      "fail if banker returned non-empty form" in {
        val badFormGen: Gen[PaymentRequest.Form] =
          bool.map {
            case true =>
              CorrectForm.toBuilder.setUrl("https://example.com").build()
            case false =>
              CorrectForm.toBuilder
                .setFields(PaymentRequest.Form.Fields.getDefaultInstance)
                .build()
          }
        forAll(PayWithTiedCardRequestGen, userEssentialsGen, AnyTiedCardGen, badFormGen) {
          (request, userEssentials, card, form) =>
            inSequence {
              mockGetYandexPassportId
                .expects(AutoruUser(request.account.getUser).id)
                .returningZ(userEssentials)
              mockFeatureToggleOff()
              mockGetAttachedCards
                .expects(
                  request.account.getUser,
                  PaymentSystemId.YANDEXKASSA_V3,
                  None
                )
                .returningZ(List(card))
              mockPay
                .expects(
                  request.account.getUser,
                  card,
                  asPaymentRequestSource(request, card),
                  None
                )
                .returningZ(form)
            }

            service
              .payWithTiedCard(request)
              .provideRc(rc)
              .failure
              .exception shouldBe an[IllegalArgumentException]
        }
      }
    }
  }

  "BankerApi" should {
    "make AccountConsumeRequest from PayByAccountRequest" in {
      forAll(PayByAccountRequestGen) { request =>
        val consumeRequest = asConsumeRequest(request)
        consumeRequest.getAccount shouldBe request.account.getId
        consumeRequest.getAmount shouldBe request.amount
        consumeRequest.getType shouldBe TransactionType.WITHDRAW
        val payloadFields =
          consumeRequest.getPayload.getStruct.getFieldsMap.asScala
        payloadFields.get("transaction").map(_.getStringValue) should contain(
          request.payload.transactionId
        )
        consumeRequest.getReceipt.getGoodsList.asScala
          .map(good =>
            (good.getName, good.getQuantity, good.getPrice)
          ) should contain theSameElementsAs
          request.receiptRows.map(row => (row.name, row.qty, row.price))
      }
    }

    "make PaymentRequest.Source from PayWithTiedCardRequest" in {
      def checkSource(
          request: PayWithLinkedCardRequest,
          card: PaymentMethod,
          shouldBeWithPaygateContext: Boolean
      ) = {
        val source = asPaymentRequestSource(request, card)
        source.getAccount shouldBe request.account.getId
        source.getAmount shouldBe request.amount
        source.hasOptions shouldBe true
        source.getOptions.getId shouldBe request.payload.transactionId
        source.hasReceipt shouldBe true
        source.hasPayGateContext shouldBe shouldBeWithPaygateContext

        source.getReceipt.getGoodsList.asScala
          .map(good =>
            (good.getName, good.getQuantity, good.getPrice)
          ) should contain theSameElementsAs
          request.receiptRows.map(row => (row.name, row.qty, row.price))
        source.getContext.getTarget shouldBe request.target
        val payloadFields = source.getPayload.getStruct.getFieldsMap.asScala
        payloadFields
          .get("transaction")
          .map(_.getStringValue) should contain(request.payload.transactionId)
      }

      forAll(PayWithTiedCardRequestGen, YandexKassaTiedCardGen) { (request, card) =>
        checkSource(request, card, shouldBeWithPaygateContext = false)
      }

      forAll(PayWithTiedCardRequestGen, YandexKassaV3TiedCardGen) { (request, card) =>
        checkSource(request, card, shouldBeWithPaygateContext = true)
      }
    }

  }

}

object BankerServiceImplSpec extends MockFactory with BasicSalesmanGenerators {

  val CorrectRecurrentPaymentResponse: RecurrentPaymentResponse =
    RecurrentPaymentResponse
      .newBuilder()
      .setStatus(RecurrentPaymentResponse.RecurrentPaymentStatus.IN_PROGRESS)
      .build()

  val CorrectForm: PaymentRequest.Form =
    PaymentRequest.Form.newBuilder().setId("form1").build()

  val NoEnoughFundsException: PaymentRequiredException = PaymentRequiredException(
    "/test",
    Some("card has no enough funds"),
    ApiError
      .newBuilder()
      .setCardError(ApiError.RecurrentPaymentError.CARD_HAS_NO_ENOUGH_FUNDS)
      .build()
      .toByteArray
  )

  def accountInfo(balance: Funds): AccountInfo =
    AccountInfo.newBuilder().setBalance(balance).build()
}
