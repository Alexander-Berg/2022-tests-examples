package ru.yandex.vertis.billing.banker.payment.impl

import org.scalatest.LoneElement
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.model.ApiModel.ApiError.{ConsumeError, RecurrentPaymentError}
import ru.yandex.vertis.billing.banker.dao.RecurrentPaymentDao.{RecurrentPaymentRequest, RecurrentPaymentStatus}
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.JdbcRecurrentPaymentDao
import ru.yandex.vertis.billing.banker.dao.util.CleanableJdbcRecurrentPaymentDao
import ru.yandex.vertis.billing.banker.model.AccountTransaction.PushStatuses
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{Context, ReceiptData, ReceiptGood, Targets}
import ru.yandex.vertis.billing.banker.model.TrustContext.ApiPayment
import ru.yandex.vertis.billing.banker.model._
import ru.yandex.vertis.billing.banker.model.gens.{TrustCardIdGen, _}
import ru.yandex.vertis.billing.banker.payment.TrustEnvironmentProvider
import ru.yandex.vertis.billing.banker.payment.impl.PaymentServiceImplSpec._
import ru.yandex.vertis.billing.banker.payment.impl.TrustPaymentSupportSpec.UserPassportUid
import ru.yandex.vertis.billing.banker.payment.recurrent.AccountRecurrentProcessor.WalletMethodId
import ru.yandex.vertis.billing.banker.payment.recurrent.TrustRecurrentProcessor
import ru.yandex.vertis.billing.banker.payment.util.TrustMockProvider.TrustState
import ru.yandex.vertis.billing.banker.service.PaymentSetup
import ru.yandex.vertis.billing.banker.service.impl.TrivialPaymentSetupsRegistry
import ru.yandex.vertis.billing.banker.util.TrustConversions.ToTrustTaxType
import ru.yandex.vertis.billing.banker.util.UserContext
import ru.yandex.vertis.billing.receipt.model.TaxTypes
import ru.yandex.vertis.billing.trust.exceptions.TrustException.GetBasketError
import ru.yandex.vertis.billing.trust.model._
import ru.yandex.vertis.tracing.Traced
import spray.json.{JsObject, JsString}

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

class PaymentServiceImplSpec
  extends AnyWordSpec
  with Matchers
  with LoneElement
  with AsyncSpecBase
  with TrustEnvironmentProvider {

  implicit override protected val rc =
    UserContext("payment-service-test", "I am human. Trust me :)", Some(UserPassportUid))

  private val recurrentPaymentDao =
    new JdbcRecurrentPaymentDao(database) with CleanableJdbcRecurrentPaymentDao

  private val trustRecurrentProcessor = new TrustRecurrentProcessor(paymentSupport, recurrentPaymentDao, purchaseDao)

  private val paymentSetups = Seq(PaymentSetup(paymentSupport, psTransactionsService, trustRecurrentProcessor))

  private val registry = new TrivialPaymentSetupsRegistry(paymentSetups)

  private val paymentService = new PaymentServiceImpl(accountTransactionService, registry, recurrentPaymentDao)

  override def beforeEach(): Unit = {
    super.beforeEach()
    recurrentPaymentDao.clean().futureValue
    createAccount(UserAccountId)
    ()
  }

  "PaymentService / requestRecurrent" should {

    "correctly accept a new recurrent payment request" in {
      val source = buildRecurrentPaymentSource()

      val result = paymentService.requestRecurrent(PaymentSystemIds.Trust, UserId, source).futureValue

      result shouldBe RecurrentPaymentStatus.InProgress
      val request = recurrentPaymentDao.getRequest(source.externalId).futureValue
      request.map(_.source) shouldBe Some(source)
    }

    "return an existing recurrent payment request" in {
      val source = buildRecurrentPaymentSource()
      recurrentPaymentDao
        .insertRequest(
          RecurrentPaymentRequest(
            externalId = source.externalId,
            user = UserId,
            yandexUid = UserPassportUid,
            psId = PaymentSystemIds.Trust,
            source = source,
            status = RecurrentPaymentStatus.Success,
            pushStatus = PushStatuses.Ready
          )
        )
        .futureValue

      val result = paymentService.requestRecurrent(PaymentSystemIds.Trust, UserId, source).futureValue

      result shouldBe RecurrentPaymentStatus.Success
    }

    "reject recurrent payment request with unsupported target" in {
      val source = buildRecurrentPaymentSource().copy(context = Context(Targets.Wallet))

      val exception = paymentService.requestRecurrent(PaymentSystemIds.Trust, UserId, source).failed.futureValue

      exception shouldBe an[IllegalArgumentException]
    }
  }

  "PaymentService / syncRecurrent" should {

    "pay with wallet first before trying attached cards" in {
      topupWallet(1500L)
      val request = prepareRecurrentPaymentRequest(amount = 1000L)

      paymentService.syncRecurrent(request).futureValue

      val result = paymentService.requestRecurrent(PaymentSystemIds.Trust, UserId, request.source).futureValue
      result shouldBe RecurrentPaymentStatus.Success
      val payment = recurrentPaymentDao.findPaymentsByExternalId(request.source.externalId).futureValue.loneElement
      payment.paymentMethodId shouldBe WalletMethodId
      payment.status shouldBe RecurrentPaymentStatus.Success
      payment.accountTransactionId should not be empty
      val info = accountTransactionService.info(UserAccountId).futureValue
      info.totalSpent shouldBe 1000L
      info.balance shouldBe 500L
    }

    "fail with no_funds when not enough money in wallet and no attached cards" in {
      initTrustMockWithBoundCards(List.empty)
      topupWallet(500L)
      val request = prepareRecurrentPaymentRequest(amount = 1000L)

      paymentService.syncRecurrent(request).futureValue

      val result = paymentService.requestRecurrent(PaymentSystemIds.Trust, UserId, request.source).futureValue
      result shouldBe RecurrentPaymentStatus.Failure
      val payment = recurrentPaymentDao.findPaymentsByExternalId(request.source.externalId).futureValue.loneElement
      payment.paymentMethodId shouldBe WalletMethodId
      payment.status shouldBe RecurrentPaymentStatus.Failure
      payment.errorCode shouldBe Some(ConsumeError.NOT_ENOUGH_FUNDS.name())
    }

    "pay with bank card" in {
      initTrustMockWithBoundCards(List(TinkoffCard))
      val request = prepareRecurrentPaymentRequest()

      paymentService.syncRecurrent(request).futureValue

      val result = paymentService.requestRecurrent(PaymentSystemIds.Trust, UserId, request.source).futureValue
      result shouldBe RecurrentPaymentStatus.Success
      val payments = recurrentPaymentDao.findPaymentsByExternalId(request.source.externalId).futureValue
      payments.size shouldBe 2
      val walletPayment = payments.find(_.paymentMethodId == WalletMethodId)
      walletPayment.map(_.status) shouldBe Some(RecurrentPaymentStatus.Failure)
      walletPayment.flatMap(_.errorCode) shouldBe Some(ConsumeError.NOT_ENOUGH_FUNDS.name())
      val cardPayment = payments.find(_.paymentMethodId == TinkoffCard.id)
      cardPayment.map(_.status) shouldBe Some(RecurrentPaymentStatus.Success)
      cardPayment.flatMap(_.paymentRequestId) should not be empty
    }

    "pay with the next card if the previous card has not succeeded" in {
      initTrustMockWithBoundCards(
        List(TinkoffCard, SberCard, AlfaCard),
        Map(TinkoffCard.id -> "not_enough_funds", SberCard.id -> "blacklisted")
      )
      val request = prepareRecurrentPaymentRequest()

      syncRecurrent(request, times = 3).futureValue

      val result = paymentService.requestRecurrent(PaymentSystemIds.Trust, UserId, request.source).futureValue
      result shouldBe RecurrentPaymentStatus.Success
      val payments = recurrentPaymentDao.findPaymentsByExternalId(request.source.externalId).futureValue
      // карты перебираются в лексикографическом порядке по id
      (payments.map(p => (p.paymentMethodId, p.status, p.errorCode)) should contain).theSameElementsInOrderAs(
        List(
          (WalletMethodId, RecurrentPaymentStatus.Failure, Some(ConsumeError.NOT_ENOUGH_FUNDS.name())),
          (TinkoffCard.id, RecurrentPaymentStatus.Failure, Some(RecurrentPaymentError.CARD_HAS_NO_ENOUGH_FUNDS.name())),
          (SberCard.id, RecurrentPaymentStatus.Failure, Some(RecurrentPaymentError.CARD_BLOCKED.name())),
          (AlfaCard.id, RecurrentPaymentStatus.Success, None)
        )
      )
    }

    "make 3 attempts per card for retryable failures, then mark the request as failed" in {
      initTrustMockWithBoundCards(
        List(TinkoffCard, SberCard),
        Map(TinkoffCard.id -> "authorization_reject", SberCard.id -> "unknown_error")
      )
      val request = prepareRecurrentPaymentRequest()

      syncRecurrent(request, times = 6).futureValue

      val result = paymentService.requestRecurrent(PaymentSystemIds.Trust, UserId, request.source).futureValue
      result shouldBe RecurrentPaymentStatus.Failure
      val payments = recurrentPaymentDao.findPaymentsByExternalId(request.source.externalId).futureValue
      val expectedPaymentResults =
        List((WalletMethodId, RecurrentPaymentStatus.Failure, Some(ConsumeError.NOT_ENOUGH_FUNDS.name()))) ++
          List.fill(3)(
            (TinkoffCard.id, RecurrentPaymentStatus.Failure, Some(RecurrentPaymentError.CARD_TECHNICAL_ERROR.name()))
          ) ++
          List.fill(3)(
            (SberCard.id, RecurrentPaymentStatus.Failure, Some(RecurrentPaymentError.CARD_TECHNICAL_ERROR.name()))
          )
      payments.map(p =>
        (p.paymentMethodId, p.status, p.errorCode)
      ) should contain theSameElementsAs expectedPaymentResults
    }

    "make overall 15 attempts to pay with card, then mark request as failed" in {
      val cards = List.fill(20)(TinkoffCard.copy(id = TrustCardIdGen.next))
      val errors = cards.map(card => card.id -> "unknown_error").toMap
      initTrustMockWithBoundCards(cards, errors)
      val request = prepareRecurrentPaymentRequest()

      syncRecurrent(request, times = 15).futureValue

      val result = paymentService.requestRecurrent(PaymentSystemIds.Trust, UserId, request.source).futureValue
      result shouldBe RecurrentPaymentStatus.Failure
      val payments = recurrentPaymentDao.findPaymentsByExternalId(request.source.externalId).futureValue
      val cardPayments = payments.filterNot(_.paymentMethodId == WalletMethodId)
      cardPayments.size shouldBe 15
      cardPayments.foreach { p =>
        p.status shouldBe RecurrentPaymentStatus.Failure
        p.errorCode shouldBe Some(RecurrentPaymentError.CARD_TECHNICAL_ERROR.name())
      }
    }
  }

  private def prepareRecurrentPaymentRequest(amount: Funds = 100L): RecurrentPaymentRequest = {
    val source = buildRecurrentPaymentSource()
    paymentService.requestRecurrent(PaymentSystemIds.Trust, UserId, source).futureValue
    recurrentPaymentDao.getRequest(source.externalId).futureValue.get
  }

  private def topupWallet(amount: Funds) = {
    val request = AccountTransactionRequest.IncomingRequest(
      id = HashAccountTransactionId(UUID.randomUUID().toString, AccountTransactions.Incoming),
      account = UserAccountId,
      amount = amount,
      target = Some(Targets.Wallet)
    )
    accountTransactionService.execute(request).futureValue
  }

  private def initTrustMockWithBoundCards(
      cards: List[BoundCardMethod],
      failureCodes: Map[String, String] = Map.empty): TrustState = {
    implicit val state: TrustState = initTrustMock(mockProducts, mockOrders, mockBaskets, mockBuildReceiptUrl)
    stub(trustMock.getPaymentMethods(_: PassportUid)(_: Traced)) { case (_, _) =>
      Future.successful(
        PaymentMethodsResponse(
          enabledPaymentMethods = List(
            EnabledCardMethod(
              currency = "RUB",
              firmId = 12,
              paymentSystems = List("MIR", "Maestro", "MasterCard", "VISA", "VISA_ELECTRON")
            )
          ),
          boundPaymentMethods = cards
        )
      )
    }
    stub(trustMock.basketStartPayment(_: String)(_: Traced)) { case (purchaseToken, _) =>
      Future {
        state.baskets
          .updateWith(purchaseToken) {
            case Some(basket) if basket.paymentStatus == PaymentStatus.NotStarted =>
              val started = basket.copy(
                paymentStatus = PaymentStatus.Started,
                startTs = Some(Instant.now())
              )
              val processed = basket.paymentMethodId match {
                case Some(cardId) if failureCodes.contains(cardId) =>
                  started.copy(
                    paymentStatus = PaymentStatus.NotAuthorized,
                    cancelTs = Some(Instant.now()),
                    authErrorCode = Some(failureCodes(cardId))
                  )
                case Some(_) =>
                  started.copy(
                    paymentStatus = PaymentStatus.Authorized,
                    paymentTs = Some(Instant.now())
                  )

                case None => started
              }
              Some(processed)
            case Some(basket) => Some(basket)
            case None => throw GetBasketError("payment_not_found", None)
          }
          .get
      }
    }
    import OfferRaiseProduct.{fiscalNds, fiscalTitle, id, name}
    trustMock.createProduct(CreateProductRequest(id, name, fiscalTitle, ToTrustTaxType(fiscalNds)))
    state
  }

  private def buildRecurrentPaymentSource(
      externalId: ExternalTransactionId = UUID.randomUUID().toString,
      amount: Funds = 1000L) =
    RecurrentPaymentSource(
      externalId = externalId,
      account = UserAccountId,
      amount = amount,
      payload = Payload.Json(
        JsObject(
          Map(
            "domain" -> JsString("autoru"),
            "transaction" -> JsString(externalId)
          )
        )
      ),
      receiptData = ReceiptData(
        goods = List(
          ReceiptGood(OfferRaiseProduct.name, 1, amount)
        ),
        email = Some(UserEmail),
        phone = Some(UserPhone)
      ),
      context = Context(Targets.Purchase),
      payGateContext = TrustContext(
        paymentMethodData = ApiPayment,
        orders = List(
          TrustContext.Order(OfferRaiseProduct.id, 1, amount)
        ),
        bonus = None
      ),
      yandexUid = Some(UserPassportUid)
    )

  private def syncRecurrent(request: RecurrentPaymentRequest, times: Int): Future[Unit] = {
    if (times > 0)
      paymentService
        .syncRecurrent(request)
        .flatMap(_ => syncRecurrent(request, times - 1))
    else Future.unit
  }
}

object PaymentServiceImplSpec {

  val UserId = "user:4227"
  val UserAccountId = "user:4227_account"
  val UserPassportUid = 22296196L
  val UserEmail = "banker@yandex.ru"
  val UserPhone = "+79991112233"
  val OfferRaiseProduct = Product("offer_raise", "Offer raise", "Поднятие объявления", TaxTypes.Nds20)

  val TinkoffCard = BoundCardMethod(
    id = "card-x1234",
    cardMask = "510000****1234",
    system = "MasterCard",
    regionId = 225,
    bindingTs = Instant.ofEpochMilli(1644577184056L),
    expirationYear = 2024,
    expirationMonth = 12,
    holder = "CARD HOLDER",
    expired = false,
    cardBank = Some("TINKOFF")
  )

  val SberCard = BoundCardMethod(
    id = "card-x1235",
    cardMask = "510000****1235",
    system = "MasterCard",
    regionId = 225,
    bindingTs = Instant.ofEpochMilli(1645577184056L),
    expirationYear = 2025,
    expirationMonth = 12,
    holder = "CARD HOLDER",
    expired = false,
    cardBank = Some("SBER")
  )

  val AlfaCard = BoundCardMethod(
    id = "card-x1236",
    cardMask = "510000****1236",
    system = "MasterCard",
    regionId = 225,
    bindingTs = Instant.ofEpochMilli(1644567184056L),
    expirationYear = 2026,
    expirationMonth = 12,
    holder = "CARD HOLDER",
    expired = false,
    cardBank = Some("ALFABANK")
  )
}
