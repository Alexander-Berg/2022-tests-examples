package ru.yandex.vertis.billing.banker.payment.impl

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.actor.{PaymentActor, PaymentSystemTransactionActor}
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.RequestFilter
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.RequestFilter.RefundsFor
import ru.yandex.vertis.billing.banker.dao.YandexKassaRecurrentDao.ForUser
import ru.yandex.vertis.billing.banker.dao.YandexKassaV3PaymentRequestExternalDao
import ru.yandex.vertis.billing.banker.dao.YandexKassaV3PaymentRequestExternalDao.{ForInternalId, RecordStatuses}
import ru.yandex.vertis.billing.banker.dao.gens.ExternalModelGens
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.{
  GlobalJdbcAccountTransactionDao,
  JdbcAccountDao,
  JdbcPaymentRequestMetaDao,
  JdbcPaymentSystemDao,
  JdbcYandexKassaV3PaymentRequestExternalDao,
  JdbcYandexKassaV3RecurrentDao,
  PaymentSystemJdbcAccountTransactionDao
}
import ru.yandex.vertis.billing.banker.dao.util._
import ru.yandex.vertis.billing.banker.exceptions.Exceptions._
import ru.yandex.vertis.billing.banker.model.PaymentRequest._
import ru.yandex.vertis.billing.banker.model.State._
import ru.yandex.vertis.billing.banker.model.YandexKassaV3Context._
import ru.yandex.vertis.billing.banker.model._
import ru.yandex.vertis.billing.banker.model.gens._
import ru.yandex.vertis.billing.banker.payment.bruteforce.CardBruteforceBreaker
import ru.yandex.vertis.billing.banker.payment.payload.YandexKassaV3PayloadExtractor
import ru.yandex.vertis.billing.banker.service.impl._
import ru.yandex.vertis.billing.banker.service.refund.processor.YandexKassaV3RefundProcessor
import ru.yandex.vertis.billing.banker.service.{
  EffectAccountTransactionService,
  EffectPaymentSystemService,
  TransparentValidator,
  YandexKassaV3RefundHelperService
}
import ru.yandex.vertis.billing.banker.util.{DateTimeUtils, UserContext}
import ru.yandex.vertis.billing.yandexkassa.api.YandexKassaApiV3
import ru.yandex.vertis.billing.yandexkassa.api.YandexKassaApiV3.AccessDenyException
import ru.yandex.vertis.billing.yandexkassa.api.model.IdempotencyKey
import ru.yandex.vertis.external.yandexkassa.ApiModel
import ru.yandex.vertis.external.yandexkassa.ApiModel.Confirmation
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import java.util.UUID
import java.util.concurrent.{CyclicBarrier, Executors}
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters._
import scala.util.Failure

/**
  * @author ruslansd
  */
class YandexKassaV3PaymentSupportSpec
  extends AnyWordSpec
  with Matchers
  with BeforeAndAfterEach
  with AsyncSpecBase
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority
  with YandexKassaV3Helper
  with JdbcSpecTemplate {

  implicit private val rc: UserContext =
    UserContext("YandexKassaV3PaymentSupportSpec", "I am human. Trust me :)")

  private val paymentStorage = mutable.HashMap.empty[PaymentRequestId, PaymentRequest]

  private val paymentsDao =
    new JdbcPaymentSystemDao(database, PaymentSystemIds.YandexKassaV3) with CleanableJdbcPaymentSystemDao

  private val accountTransactionsDao =
    new GlobalJdbcAccountTransactionDao(database) with CleanableJdbcAccountTransactionDao

  private val accountTransactionService =
    new GlobalAccountTransactionService(accountTransactionsDao, None)

  private val psTransactionsDao =
    new PaymentSystemJdbcAccountTransactionDao(database, PaymentSystemIds.YandexKassaV3)
      with CleanableJdbcAccountTransactionDao

  private val psTransactionsService =
    new PaymentSystemTransactionService(psTransactionsDao, TransparentValidator) with EffectAccountTransactionService {

      override def effect(tr: AccountTransaction): Future[Unit] =
        PaymentSystemTransactionActor
          .asRequestOpt(tr)
          .map(accountTransactionService.execute)
          .getOrElse(Future.successful(()))
          .flatMap(_ => this.processed(tr))

    }

  private val externalDao =
    new JdbcYandexKassaV3PaymentRequestExternalDao(database) with CleanableJdbcYandexKassaV3PaymentRequestExternalDao

  val refundProcessor = new YandexKassaV3RefundProcessor(yandexKassaApiV3)

  private val refundHelperService = new RefundHelperServiceImpl(
    database,
    accountTransactionsDao,
    refundProcessor,
    psTransactionsDao,
    paymentsDao
  ) with YandexKassaV3RefundHelperService {
    override val externalRequestDao: YandexKassaV3PaymentRequestExternalDao = externalDao
  }

  private val paymentSystemService =
    new PaymentSystemServiceImpl(
      database,
      paymentsDao,
      refundHelperService,
      new YandexKassaV3PayloadExtractor(externalDao)
    ) with EffectPaymentSystemService {

      override def effect(p: State.EnrichedPayment): Future[Unit] = {
        PaymentActor
          .asRequest(paymentsDao.psId, p)
          .map(psTransactionsService.execute)
          .getOrElse(Future.successful(()))
          .flatMap(_ => this.processed(p.payment))
      }

    }

  private val accountDao = new JdbcAccountDao(database) with CleanableJdbcAccountDao
  private val accountService = new AccountServiceImpl(accountDao)

  private val yandexKassaV3RecurrentDao =
    new JdbcYandexKassaV3RecurrentDao(database) with CleanableJdbcYandexKassaV3RecurrentDao
  private val paymentRequestMetaDao = new JdbcPaymentRequestMetaDao(database) with CleanableJdbcPaymentRequestMetaDao

  private val yandexKassaRecurrentService = new YandexKassaRecurrentServiceImpl(yandexKassaV3RecurrentDao)

  private val support = new YandexKassaV3PaymentSupport(
    paymentSystemService,
    accountService,
    accountTransactionService,
    yandexKassaRecurrentService,
    yandexKassaApiV3,
    paymentRequestMetaDao,
    externalDao,
    "https://test.ru",
    None,
    None,
    "",
    CardBruteforceBreaker.Noop,
    is3dsEnabledForYandexKassa = false
  )

  override protected def beforeEach(): Unit = {
    Mockito.reset(yandexKassaApiV3)
    paymentStorage.clear()
    super.beforeEach()
  }

  def sourceGen(
      context: Option[YandexKassaV3Context] = None,
      id: Option[PaymentRequestId] = None,
      amount: Option[Funds] = None): Gen[PaymentRequest.Source] =
    for {
      paygateContext <- context.map(Gen.const).getOrElse(yandexkassaV3PaygateContext())
      options = Options(id = id, defaultURL = Some("default-url"))
      source <- paymentRequestSourceGen(
        PaymentRequestSourceParams(
          amount = amount,
          withReceipt = Some(true),
          withPayGateContext = Some(false)
        )
      )
      _ = createAccount(source)
    } yield source.copy(options = options, payGateContext = Some(paygateContext))

  private def createAccount(source: Source) =
    accountService.create(Account(source.account, source.account)).futureValue

  def executorForConsumers(count: Int): ExecutionContextExecutor =
    ExecutionContext.fromExecutor(
      Executors.newFixedThreadPool(
        count,
        new ThreadFactoryBuilder()
          .setNameFormat(s"test-paralles")
          .build()
      )
    )

  def testStatuses(
      customer: User,
      id: PaymentRequestId,
      expectedStatus: Status,
      expectedStateStatus: StateStatus,
      expectedRecordStatus: RecordStatuses.Value): Unit = {
    val afterPartRefund = support.getPaymentRequest(customer, id).futureValue
    afterPartRefund.state.map(_.status) shouldBe Some(expectedStatus)
    afterPartRefund.state.map(_.stateStatus) shouldBe Some(expectedStateStatus)
    val externalsAfterPartRefund = externalDao.get(ForInternalId(afterPartRefund.id)).futureValue
    externalsAfterPartRefund should have size 1
    val externalAfterPartRefund = externalsAfterPartRefund.head
    externalAfterPartRefund.status shouldBe expectedRecordStatus: Unit
  }

  def checkPartlyRefund(
      paymentRequestId: PaymentRequestId,
      user: User,
      amount: Funds,
      sourceData: RefundPaymentRequest.SourceData,
      expectedRefundStateStatus: StateStatus,
      expectedRecordStatus: RecordStatuses.Value): Future[Unit] = {
    for {
      _ <- support.refund(user, paymentRequestId, amount, sourceData)
      _ = testStatuses(
        user,
        paymentRequestId,
        Statuses.Processed,
        expectedRefundStateStatus,
        expectedRecordStatus
      )
    } yield ()
  }

  private val RefundedPaymentAmount = 100000L
  private val RefundAmountParts = Seq(50000L, 25000L, 12500L, 6500L, 6000L)

  def testPartlyRefunds(action: (Seq[Funds], PaymentRequest) => Unit): Unit = {
    forAll(yandexkassaV3PaygateContext()) { context =>
      val source = sourceGen(
        Some(context),
        Some(RequestIdGen.next),
        Some(RefundedPaymentAmount)
      ).next

      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          Future.successful(successPayment(s))
      }

      val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue

      val paymentRequest = support.getPaymentRequest(source.account, form.id).futureValue
      paymentRequest.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)

      stub(yandexKassaApiV3.refund(_: ApiModel.RefundRequest)(_: IdempotencyKey, _: Traced)) {
        case (rr, _, _) if notNull(rr) =>
          Future.successful(successRefund(rr))
      }

      action(RefundAmountParts, paymentRequest)

      Mockito.verify(yandexKassaApiV3).createPayment(?)(?, ?)
      Mockito.clearInvocations(yandexKassaApiV3)
    }
  }

  def testRefundRetryAfterFail(
      processFail: PaymentRequest => Unit,
      refundCreator: ApiModel.RefundRequest => ApiModel.Refund,
      processAfterFail: PaymentRequest => Unit
    )(refundCheck: (PaymentRequest, Seq[RefundPaymentRequest]) => Unit): Unit = {
    forAll(yandexkassaV3PaygateContext()) { context =>
      val paymentAmount = 100000L
      val source = sourceGen(
        Some(context),
        Some(RequestIdGen.next),
        Some(paymentAmount)
      ).next

      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          Future.successful(successPayment(s))
      }

      val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue

      val paymentRequest = support.getPaymentRequest(source.account, form.id).futureValue
      paymentRequest.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)

      stub(yandexKassaApiV3.refund(_: ApiModel.RefundRequest)(_: IdempotencyKey, _: Traced)) {
        case (rr, _, _) if notNull(rr) =>
          Future.failed(new IllegalArgumentException("fail"))
      }

      processFail(paymentRequest)

      stub(yandexKassaApiV3.refund(_: ApiModel.RefundRequest)(_: IdempotencyKey, _: Traced)) {
        case (rr, _, _) if notNull(rr) =>
          Future.successful(refundCreator(rr))
      }

      processAfterFail(paymentRequest)

      Mockito.verify(yandexKassaApiV3).createPayment(?)(?, ?)
      Mockito.clearInvocations(yandexKassaApiV3)
    }
  }

  def testAllRefundsAfterFail(full: Boolean, success: Boolean): Unit = {
    val refundAction = { r: PaymentRequest =>
      if (full) {
        support.fullRefund(r.source.account, r.id, None, None)
      } else {
        val refundAmount = r.source.amount / 2
        val sourceData = refundRequestSourceDataGen(amount = Some(refundAmount)).next
        support.refund(r.source.account, r.id, refundAmount, sourceData)
      }
    }

    val failRefund: PaymentRequest => Unit = { r: PaymentRequest =>
      refundAction(r).toTry match {
        case Failure(_) => info(s"Done")
        case other => fail(s"Unexpected $other")
      }
      testStatuses(
        r.source.account,
        r.id,
        Statuses.Processed,
        StateStatuses.Valid,
        RecordStatuses.Succeeded
      )
    }

    val afterRefundFail: PaymentRequest => Unit = { r: PaymentRequest =>
      refundAction(r).futureValue
      val (expectedStateStatus, expectedRecordStatus) =
        if (!success) {
          (StateStatuses.Valid, RecordStatuses.Succeeded)
        } else if (full) {
          (StateStatuses.Refunded, RecordStatuses.Refunded)
        } else {
          (StateStatuses.PartlyRefunded, RecordStatuses.PartlyRefunded)
        }
      testStatuses(
        r.source.account,
        r.id,
        Statuses.Processed,
        expectedStateStatus,
        expectedRecordStatus
      )
    }

    val refundProvider =
      if (!success) {
        canceledRefund _
      } else {
        successRefund _
      }

    testRefundRetryAfterFail(
      failRefund,
      refundProvider,
      afterRefundFail
    ) { (paymentRequest, refunds) =>
      refunds.size shouldBe 1
      val refund = refunds.head
      refund.state.isDefined shouldBe true
      refund.source.refundFor shouldBe paymentRequest.id
      val refundState = refund.state.get
      refundState.status shouldBe Statuses.Processed

      val expectedRefundStateStatus =
        if (!success) {
          StateStatuses.Cancelled
        } else {
          StateStatuses.Valid
        }

      refundState.stateStatus shouldBe expectedRefundStateStatus

      val expectedRefundAmount =
        if (!full) {
          paymentRequest.source.amount / 2
        } else {
          paymentRequest.source.amount
        }

      refundState.amount shouldBe expectedRefundAmount: Unit
    }
  }

  def checkProcessRefundAfterCancelation(context: YandexKassaV3Context, cancellationCount: Int, full: Boolean): Unit = {
    val paymentAmount = 100000L
    val refundAmount = 60000L
    val source = sourceGen(
      Some(context),
      Some(RequestIdGen.next),
      Some(paymentAmount)
    ).next

    stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
      case (s, _, _) if notNull(s) =>
        Future.successful(successPayment(s))
    }

    val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue

    val paymentRequest = support.getPaymentRequest(source.account, form.id).futureValue
    paymentRequest.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)

    val refundSource = refundRequestSourceDataGen(
      amount = Some(refundAmount)
    ).next

    (1 to cancellationCount).foreach { _ =>
      stub(yandexKassaApiV3.refund(_: ApiModel.RefundRequest)(_: IdempotencyKey, _: Traced)) {
        case (rr, _, _) if notNull(rr) =>
          Future.successful(canceledRefund(rr))
      }

      if (full) {
        support.fullRefund(source.account, form.id, None, None).futureValue
      } else {
        support.refund(source.account, form.id, refundAmount, refundSource).futureValue
      }

      testStatuses(
        source.account,
        form.id,
        Statuses.Processed,
        StateStatuses.Valid,
        RecordStatuses.Succeeded
      )
    }

    stub(yandexKassaApiV3.refund(_: ApiModel.RefundRequest)(_: IdempotencyKey, _: Traced)) {
      case (rr, _, _) if notNull(rr) =>
        Future.successful(successRefund(rr))
    }

    if (full) {
      support.fullRefund(source.account, form.id, None, None).futureValue

      testStatuses(
        source.account,
        form.id,
        Statuses.Processed,
        StateStatuses.Refunded,
        RecordStatuses.Refunded
      )
    } else {
      support.refund(source.account, form.id, refundAmount, refundSource).futureValue

      testStatuses(
        source.account,
        form.id,
        Statuses.Processed,
        StateStatuses.PartlyRefunded,
        RecordStatuses.PartlyRefunded
      )
    }

    val refundCallsCount = cancellationCount + 1
    val refunds = paymentSystemService.getRefundPaymentRequests(Seq(RefundsFor(form.id))).futureValue
    refunds.size shouldBe refundCallsCount
    val cancelled = refunds.filter(_.state.exists(_.stateStatus == StateStatuses.Cancelled))
    cancelled.size shouldBe cancellationCount
    val valid = refunds.filter(_.state.exists(_.stateStatus == StateStatuses.Valid))
    valid.size shouldBe 1
    val cancelationIds = cancelled.map(_.id).toSet
    cancelationIds.contains(valid.head.id) shouldBe false

    Mockito.verify(yandexKassaApiV3).createPayment(?)(?, ?)
    Mockito.verify(yandexKassaApiV3, Mockito.times(refundCallsCount)).refund(?)(?, ?)
    Mockito.clearInvocations(yandexKassaApiV3)
  }

  "YandexKassaV3PaymentSupport" should {
    "process valid requests pending" in {
      val context = YandexKassaV3Context(BankCard(None), Some("some_token"), None)
      val source = sourceGen(Some(context)).next
      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          Future.successful(pendingPayment(s))
      }
      val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue

      Mockito.verify(yandexKassaApiV3).createPayment(?)(?, ?)
      val pr = paymentSystemService.getPaymentRequest(form.id).futureValue
      pr.state shouldBe None

      Mockito.clearInvocations(yandexKassaApiV3)
    }

    "process valid success requests" in {
      val context = YandexKassaV3Context(BankCard(None), Some("some_token"), None)
      val source = sourceGen(Some(context)).next
      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          Future.successful(successPayment(s))
      }

      val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue

      Mockito.verify(yandexKassaApiV3).createPayment(?)(?, ?)
      val pr = paymentSystemService.getPaymentRequest(form.id).futureValue
      pr.state.isDefined shouldBe true

      Mockito.clearInvocations(yandexKassaApiV3)
    }

    "process valid cancelled requests" in {
      val context = YandexKassaV3Context(Qiwi(Some("+79123456789")), None, None)
      val source = sourceGen(Some(context)).next
      var lastSource: Option[ApiModel.Payment] = None
      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          lastSource = Some(cancelledPayment(s))
          Future.successful(lastSource.get)
      }

      intercept[CancellationPaymentException] {
        support.request(source.account, context.paymentMethodData.`type`.toString, source).await
      }

      Mockito.verify(yandexKassaApiV3).createPayment(?)(?, ?)
      Mockito.clearInvocations(yandexKassaApiV3)
    }

    "process success notification from kassa" in {
      val context = YandexKassaV3Context(BankCard(None), Some("some_token"), None)
      val source = sourceGen(Some(context)).next
      val captor: ArgumentCaptor[ApiModel.PaymentSource] =
        ArgumentCaptor.forClass(classOf[ApiModel.PaymentSource])
      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          Future.successful(pendingPayment(s))
      }

      val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue
      Mockito.verify(yandexKassaApiV3).createPayment(captor.capture())(any[IdempotencyKey](), any[Traced]())
      val notification = paymentNotificationGen(successPayment(captor.getValue)).next

      support.parse(AbstractNotificationSource(notification)).futureValue
      support.getPaymentRequest(source.account, form.id).futureValue.state.isDefined shouldBe true
      Mockito.clearInvocations(yandexKassaApiV3)
    }

    "process pending notification from kassa" in {
      val context = YandexKassaV3Context(Qiwi(Some("+79123456789")), None, None)
      val source = sourceGen(Some(context)).next
      val captor: ArgumentCaptor[ApiModel.PaymentSource] =
        ArgumentCaptor.forClass(classOf[ApiModel.PaymentSource])
      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          Future.successful(pendingPayment(s))
      }

      val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue
      Mockito.verify(yandexKassaApiV3).createPayment(captor.capture())(any[IdempotencyKey](), any[Traced]())
      val notification = paymentNotificationGen(pendingPayment(captor.getValue)).next
      support.parse(AbstractNotificationSource(notification)).futureValue
      support.getPaymentRequest(source.account, form.id).futureValue.state.isDefined shouldBe false
      Mockito.clearInvocations(yandexKassaApiV3)
    }

    "process success refunds" in {
      val context = YandexKassaV3Context(BankCard(None), Some("some_token"), None)
      val source = sourceGen(Some(context)).next
      val captor: ArgumentCaptor[ApiModel.PaymentSource] =
        ArgumentCaptor.forClass(classOf[ApiModel.PaymentSource])
      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          Future.successful(pendingPayment(s))
      }

      val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue
      Mockito.verify(yandexKassaApiV3).createPayment(captor.capture())(any[IdempotencyKey](), any[Traced]())
      val notification = paymentNotificationGen(successPayment(captor.getValue)).next

      support.parse(AbstractNotificationSource(notification)).futureValue
      val paymentRequest = support.getPaymentRequest(source.account, form.id).futureValue
      paymentRequest.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)

      stub(yandexKassaApiV3.refund(_: ApiModel.RefundRequest)(_: IdempotencyKey, _: Traced)) {
        case (rr, _, _) if notNull(rr) =>
          Future.successful(successRefund(rr))
      }

      support.fullRefund(source.account, form.id, None, None).futureValue

      val refundRequests = paymentSystemService
        .getRefundPaymentRequests(
          Seq(RequestFilter.RefundsFor(form.id))
        )
        .futureValue
      refundRequests should have size 1
      val refundRequest = refundRequests.head
      refundRequest.state.isDefined shouldBe true
      val refundState = refundRequest.state.get
      refundState.amount shouldBe paymentRequest.source.amount
      refundState.status shouldBe Statuses.Processed
      refundState.stateStatus shouldBe StateStatuses.Valid

      support.getPaymentRequest(source.account, form.id).futureValue.state.map(_.stateStatus) shouldBe Some(
        StateStatuses.Refunded
      )

      Mockito.clearInvocations(yandexKassaApiV3)
    }

    "process canceled refunds" in {
      val params = StateParams(
        invoiceId = Some(Some("test")),
        stateStatus = Set(StateStatuses.Valid)
      )
      val paymentRequestParams =
        PaymentRequestParams(source = PaymentRequestSourceParams(withPayGateContext = Some(false)))
      forAll(incomingPaymentGen(params), paymentRequestGen(paymentRequestParams.withState(params))) { (p, r) =>
        val source = r.source.copy(amount = p.amount, optReceiptData = None)
        val pr = r.copy(source = source)
        paymentStorage += (pr.id -> pr)
        pr.state.foreach { _ =>
          stub(yandexKassaApiV3.refund(_: ApiModel.RefundRequest)(_: IdempotencyKey, _: Traced)) {
            case (rr, _, _) if notNull(rr) =>
              Future.successful(canceledRefund(rr))
          }
        }
        (support.fullRefund(source.account, pr.id, None, None).toTry should be).a(Symbol("Failure"))
      }
    }

    "fallback on save=false" in {
      val context = YandexKassaV3Context(BankCard(None), Some("token"), None, save = true)
      val source = sourceGen(Some(context)).next
      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          if (s.getSavePaymentMethod) {
            Future.failed(AccessDenyException(ApiModel.ApiError.newBuilder().build()))
          } else {
            Future.successful(pendingPayment(s))
          }
      }
      val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue

      Mockito.verify(yandexKassaApiV3, Mockito.times(2)).createPayment(?)(?, ?)
      context.paymentMethodData match {
        case BankCard(None) =>
          yandexKassaV3RecurrentDao.get(ForUser(source.account)).futureValue.isEmpty shouldBe true
        case _ =>
      }
      val pr = paymentSystemService.getPaymentRequest(form.id).futureValue
      pr.state shouldBe None

      Mockito.clearInvocations(yandexKassaApiV3)
    }

    "process repeat request call with success payment" in {
      var lastPayment: Option[ApiModel.Payment] = None
      val context = YandexKassaV3Context(BankCard(None), Some("some_token"), None)
      val paymentRequestId = "some_pr_id"
      val source = sourceGen(Some(context), Some(paymentRequestId)).next
      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          lastPayment = Some(successPayment(s))
          Future.successful(lastPayment.get)
      }

      val firstForm = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue

      firstForm shouldBe expectedForm(lastPayment.get)

      val pr = paymentSystemService.getPaymentRequest(firstForm.id).futureValue
      pr.state.isDefined shouldBe true
      pr.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)

      val secondForm = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue

      secondForm shouldBe EmptyForm(firstForm.id)

      Mockito.verify(yandexKassaApiV3).createPayment(?)(?, ?)
      Mockito.clearInvocations(yandexKassaApiV3)
    }

    "fail process repeat request call with full/partly refunded payment" in {
      val context = YandexKassaV3Context(BankCard(None), Some("some_token"), None)
      val paymentAmount = 100000L
      val half = paymentAmount / 2
      val source = sourceGen(
        Some(context),
        Some(RequestIdGen.next),
        Some(paymentAmount)
      ).next

      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          Future.successful(successPayment(s))
      }

      val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue

      val paymentRequest = support.getPaymentRequest(source.account, form.id).futureValue
      paymentRequest.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)

      stub(yandexKassaApiV3.refund(_: ApiModel.RefundRequest)(_: IdempotencyKey, _: Traced)) {
        case (rr, _, _) if notNull(rr) =>
          Future.successful(successRefund(rr))
      }

      val firstSourceData = refundRequestSourceDataGen(amount = Some(half)).next
      support.refund(source.account, form.id, half, firstSourceData).futureValue

      testStatuses(
        source.account,
        form.id,
        Statuses.Processed,
        StateStatuses.PartlyRefunded,
        RecordStatuses.PartlyRefunded
      )

      intercept[PaymentPartlyRefundedException] {
        support.request(source.account, context.paymentMethodData.`type`.toString, source).await
      }

      val secondSourceData = refundRequestSourceDataGen(amount = Some(half)).next
      support.refund(source.account, form.id, paymentAmount, secondSourceData).futureValue

      testStatuses(
        source.account,
        form.id,
        Statuses.Processed,
        StateStatuses.Refunded,
        RecordStatuses.Refunded
      )

      intercept[PaymentAlreadyRefundedException] {
        support.request(source.account, context.paymentMethodData.`type`.toString, source).await
      }

      Mockito.verify(yandexKassaApiV3).createPayment(?)(?, ?)
      Mockito.verify(yandexKassaApiV3, Mockito.times(2)).refund(?)(?, ?)
      Mockito.clearInvocations(yandexKassaApiV3)
    }

    "process repeat request call with cancelled payment" in {
      val cancelledPaymentStorage = mutable.HashMap.empty[ApiModel.PaymentSource, ApiModel.Payment]
      forAll(yandexkassaV3PaygateContext(), readableString()) { (context, id) =>
        val source = sourceGen(Some(context), Some(id)).next

        stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
          case (s, _, _) if notNull(s) =>
            val result = cancelledPaymentStorage.get(s) match {
              case Some(response) =>
                response
              case None =>
                val response = cancelledPayment(s)
                cancelledPaymentStorage += s -> response
                response
            }
            Future.successful(result)
        }

        val firstCall = support.request(source.account, context.paymentMethodData.`type`.toString, source).toTry
        val firstFailure = firstCall match {
          case Failure(e: CancellationPaymentException) =>
            e
          case r =>
            fail(s"Unexpected result $r")
        }

        val secondCall = support.request(source.account, context.paymentMethodData.`type`.toString, source).toTry
        secondCall match {
          case Failure(secondFailure: CancellationPaymentException) =>
            secondFailure.code shouldBe firstFailure.code
            secondFailure.msg shouldBe firstFailure.msg
          case _ =>
            fail("Unexpected result")
        }

        Mockito.verify(yandexKassaApiV3, Mockito.times(2)).createPayment(?)(?, ?)
        Mockito.clearInvocations(yandexKassaApiV3)
      }
    }

    "fail refund on unprocessed payment" in {
      forAll(yandexkassaV3PaygateContext()) { context =>
        val source = sourceGen(Some(context)).next

        stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
          case (s, _, _) if notNull(s) =>
            Future.successful(pendingPayment(s))
        }

        val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue

        val paymentRequest = support.getPaymentRequest(source.account, form.id).futureValue
        paymentRequest.state.map(_.stateStatus) shouldBe None

        intercept[IllegalStateException] {
          support.fullRefund(source.account, form.id, None, None).await
        }

        Mockito.verify(yandexKassaApiV3).createPayment(?)(?, ?)
        Mockito.verify(yandexKassaApiV3, Mockito.times(0)).refund(?)(?, ?)
        Mockito.clearInvocations(yandexKassaApiV3)
      }
    }

    "fail refund on cancelled payment" in {
      val context = YandexKassaV3Context(BankCard(None), Some("some_token"), None)
      val source = sourceGen(Some(context)).next
      val captor: ArgumentCaptor[ApiModel.PaymentSource] =
        ArgumentCaptor.forClass(classOf[ApiModel.PaymentSource])
      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          Future.successful(pendingPayment(s))
      }

      val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue
      Mockito.verify(yandexKassaApiV3).createPayment(captor.capture())(any[IdempotencyKey](), any[Traced]())
      val notification = paymentNotificationGen(cancelledPayment(captor.getValue)).next

      support.parse(AbstractNotificationSource(notification)).futureValue
      val paymentRequest = support.getPaymentRequest(source.account, form.id).futureValue
      paymentRequest.state.map(_.stateStatus) shouldBe Some(StateStatuses.Cancelled)

      intercept[IllegalStateException] {
        support.fullRefund(source.account, form.id, None, None).await
      }

      Mockito.verify(yandexKassaApiV3).createPayment(?)(?, ?)
      Mockito.verify(yandexKassaApiV3, Mockito.times(0)).refund(?)(?, ?)
      Mockito.clearInvocations(yandexKassaApiV3)
    }

    "fail full/partly refunds when payment already full refunded by parts" in {
      val context = YandexKassaV3Context(BankCard(None), Some("some_token"), None)
      val paymentAmount = 100000L
      val refundAmount = 60000L
      val source = sourceGen(
        Some(context),
        Some(RequestIdGen.next),
        Some(paymentAmount)
      ).next

      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          Future.successful(successPayment(s))
      }

      val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue

      val paymentRequest = support.getPaymentRequest(source.account, form.id).futureValue
      paymentRequest.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)

      stub(yandexKassaApiV3.refund(_: ApiModel.RefundRequest)(_: IdempotencyKey, _: Traced)) {
        case (rr, _, _) if notNull(rr) =>
          Future.successful(successRefund(rr))
      }

      val firstSourceData = refundRequestSourceDataGen(amount = Some(refundAmount)).next
      support.refund(source.account, form.id, refundAmount, firstSourceData).futureValue

      testStatuses(
        source.account,
        form.id,
        Statuses.Processed,
        StateStatuses.PartlyRefunded,
        RecordStatuses.PartlyRefunded
      )

      val failNotEnoughFundsSourceData = refundRequestSourceDataGen(
        amount = Some(paymentAmount - refundAmount)
      ).next
      intercept[IllegalArgumentException] {
        support.refund(source.account, form.id, paymentAmount + refundAmount, failNotEnoughFundsSourceData).await
      }

      intercept[IllegalArgumentException] {
        support.fullRefund(source.account, form.id, None, None).await
      }

      val restSourceData = refundRequestSourceDataGen(
        amount = Some(paymentAmount - refundAmount),
        withReceipt = Some(true)
      ).next
      support.refund(source.account, form.id, paymentAmount, restSourceData).futureValue

      testStatuses(
        source.account,
        form.id,
        Statuses.Processed,
        StateStatuses.Refunded,
        RecordStatuses.Refunded
      )

      val failAlreadyRefundedSourceData = refundRequestSourceDataGen(
        amount = Some(refundAmount)
      ).next
      intercept[PaymentAlreadyRefundedException] {
        support.refund(source.account, form.id, paymentAmount + refundAmount, failAlreadyRefundedSourceData).await
      }
      intercept[PaymentAlreadyRefundedException] {
        support.fullRefund(source.account, form.id, None, None).await
      }

      Mockito.verify(yandexKassaApiV3).createPayment(?)(?, ?)
      Mockito.verify(yandexKassaApiV3, Mockito.times(2)).refund(?)(?, ?)
      Mockito.clearInvocations(yandexKassaApiV3)
    }

    "fail full/partly refunds when payment already full refunded" in {
      val context = YandexKassaV3Context(BankCard(None), Some("some_token"), None)
      val paymentAmount = 100000L
      val refundAmount = 60000L
      val source = sourceGen(
        Some(context),
        Some(RequestIdGen.next),
        Some(paymentAmount)
      ).next

      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          Future.successful(successPayment(s))
      }

      val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue

      val paymentRequest = support.getPaymentRequest(source.account, form.id).futureValue
      paymentRequest.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)

      stub(yandexKassaApiV3.refund(_: ApiModel.RefundRequest)(_: IdempotencyKey, _: Traced)) {
        case (rr, _, _) if notNull(rr) =>
          Future.successful(successRefund(rr))
      }

      support.fullRefund(source.account, form.id, None, None).futureValue

      testStatuses(
        source.account,
        form.id,
        Statuses.Processed,
        StateStatuses.Refunded,
        RecordStatuses.Refunded
      )

      val failAlreadyRefundedSourceData = refundRequestSourceDataGen(
        amount = Some(refundAmount),
        withReceipt = Some(true)
      ).next
      intercept[PaymentAlreadyRefundedException] {
        support.refund(source.account, form.id, refundAmount, failAlreadyRefundedSourceData).await
      }
      intercept[PaymentAlreadyRefundedException] {
        support.fullRefund(source.account, form.id, None, None).await
      }

      Mockito.verify(yandexKassaApiV3).createPayment(?)(?, ?)
      Mockito.verify(yandexKassaApiV3).refund(?)(?, ?)
      Mockito.clearInvocations(yandexKassaApiV3)
    }

    "fail when refund more than payment amount" in {
      val context = YandexKassaV3Context(BankCard(None), Some("some_token"), None)
      val paymentAmount = 100000L
      val refundAmount = 2 * paymentAmount
      val source = sourceGen(
        Some(context),
        Some(RequestIdGen.next),
        Some(paymentAmount)
      ).next

      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          Future.successful(successPayment(s))
      }

      val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue

      val paymentRequest = support.getPaymentRequest(source.account, form.id).futureValue
      paymentRequest.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)

      val failNotEnoughFundsSourceData = refundRequestSourceDataGen(
        amount = Some(refundAmount),
        withReceipt = Some(true)
      ).next
      intercept[IllegalArgumentException] {
        support.refund(source.account, form.id, refundAmount, failNotEnoughFundsSourceData).await
      }

      testStatuses(
        source.account,
        form.id,
        Statuses.Processed,
        StateStatuses.Valid,
        RecordStatuses.Succeeded
      )

      Mockito.verify(yandexKassaApiV3).createPayment(?)(?, ?)
      Mockito.verify(yandexKassaApiV3, Mockito.times(0)).refund(?)(?, ?)
      Mockito.clearInvocations(yandexKassaApiV3)
    }

    "process repeated full refunds" in {
      val context = YandexKassaV3Context(BankCard(None), Some("some_token"), None)
      val paymentAmount = 100000L
      val source = sourceGen(
        Some(context),
        Some(RequestIdGen.next),
        Some(paymentAmount)
      ).next

      stub(yandexKassaApiV3.createPayment(_: ApiModel.PaymentSource)(_: IdempotencyKey, _: Traced)) {
        case (s, _, _) if notNull(s) =>
          Future.successful(successPayment(s))
      }

      val form = support.request(source.account, context.paymentMethodData.`type`.toString, source).futureValue

      val paymentRequest = support.getPaymentRequest(source.account, form.id).futureValue
      paymentRequest.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)

      stub(yandexKassaApiV3.refund(_: ApiModel.RefundRequest)(_: IdempotencyKey, _: Traced)) {
        case (rr, _, _) if notNull(rr) =>
          Future.successful(successRefund(rr))
      }

      val consumesCounts = 3
      val barrier = new CyclicBarrier(consumesCounts)
      val futures = (1 to consumesCounts).map { _ =>
        Future(barrier.await()).flatMap { _ =>
          support.fullRefund(source.account, form.id, None, None)
        }
      }
      Future.sequence(futures).futureValue

      testStatuses(
        source.account,
        form.id,
        Statuses.Processed,
        StateStatuses.Refunded,
        RecordStatuses.Refunded
      )

      Mockito.verify(yandexKassaApiV3).createPayment(?)(?, ?)
      Mockito.verify(yandexKassaApiV3).refund(?)(?, ?)
      Mockito.clearInvocations(yandexKassaApiV3)
    }

    "process repeated partly refunds" in {
      val consumesCounts = 3
      implicit val ec = executorForConsumers(consumesCounts)
      testPartlyRefunds { (refundAmountParts, paymentRequest) =>
        var sum = 0L
        var previousRefundSource: Option[RefundPaymentRequest.SourceData] = None
        refundAmountParts.foreach { refundPart =>
          sum = sum + refundPart

          val (expectedRefundStateStatus, expectedRecordStatus) =
            if (sum == paymentRequest.source.amount) {
              (StateStatuses.Refunded, RecordStatuses.Refunded)
            } else {
              (StateStatuses.PartlyRefunded, RecordStatuses.PartlyRefunded)
            }

          def safeExecute(amount: Funds, sourceData: RefundPaymentRequest.SourceData): Future[Unit] = {
            checkPartlyRefund(
              paymentRequest.id,
              paymentRequest.source.account,
              amount,
              sourceData,
              expectedRefundStateStatus,
              expectedRecordStatus
            ).recoverWith {
              case _: PaymentAlreadyRefundedException if expectedRefundStateStatus == StateStatuses.Refunded =>
                Future.unit
            }
          }

          val barrier = new CyclicBarrier(consumesCounts)
          val sourceData = refundRequestSourceDataGen(amount = Some(refundPart)).next
          val futures = (1 to consumesCounts).map { _ =>
            Future(barrier.await()).flatMap { _ =>
              safeExecute(sum, sourceData)
            }
          }
          Future.sequence(futures).futureValue

          safeExecute(sum, sourceData).futureValue

          previousRefundSource.foreach { prev =>
            safeExecute(sum - refundPart, prev).futureValue
          }

          previousRefundSource = Some(sourceData)
          val refundRequests =
            support.getRefundRequestsFor(paymentRequest.source.account, paymentRequest.id).futureValue
          val processedRefundRequests = refundRequests.filter { req =>
            req.state.exists { state =>
              state.status == Statuses.Processed && state.stateStatus == StateStatuses.Valid
            }
          }
          processedRefundRequests.size shouldBe refundRequests.size
        }

        Mockito.verify(yandexKassaApiV3, Mockito.times(refundAmountParts.size)).refund(?)(?, ?): Unit
      }
    }

    "correctly process partly refunds with different amounts" in {
      val perAmount = 2
      val consumersCount = RefundAmountParts.size * perAmount
      implicit val ec = executorForConsumers(consumersCount)
      testPartlyRefunds { (refundAmountParts, paymentRequest) =>
        val barrier = new CyclicBarrier(consumersCount)
        val futures = refundAmountParts.flatMap { amount =>
          (1 to perAmount).map { _ =>
            val sourceData = refundRequestSourceDataGen(amount = Some(amount)).next
            Future(barrier.await()).flatMap { _ =>
              support
                .refund(paymentRequest.source.account, paymentRequest.id, amount, sourceData)
                .map(_ => Option.empty[Funds])
                .recoverWith {
                  case _: PaymentAlreadyRefundedException | _: RefundAlreadyProcessedException =>
                    Future.successful(None)
                  case _ =>
                    Future.successful(Some(amount))
                }
            }
          }
        }
        val failed = Future.sequence(futures).futureValue.flatten
        failed.size < consumersCount shouldBe true

        Mockito.verify(yandexKassaApiV3, Mockito.atMost(consumersCount - failed.size)).refund(?)(?, ?): Unit
      }
    }

    "process success partly refund after fail" in {
      testAllRefundsAfterFail(full = false, success = true)
    }

    "process success full refund after fail" in {
      testAllRefundsAfterFail(full = true, success = true)
    }

    "process canceled partly refund after fail" in {
      testAllRefundsAfterFail(full = false, success = false)
    }

    "process canceled full refund after fail" in {
      testAllRefundsAfterFail(full = true, success = false)
    }

    "process full refund request with same source when first attempt cancelled" in {
      forAll(yandexkassaV3PaygateContext(), Gen.chooseNum(1, 10)) { (context, cancellationCount) =>
        checkProcessRefundAfterCancelation(context, cancellationCount, full = true)
      }
    }

    "process partly refund request with same source when first attempt cancelled" in {
      forAll(yandexkassaV3PaygateContext(), Gen.chooseNum(1, 10)) { (context, cancellationCount) =>
        checkProcessRefundAfterCancelation(context, cancellationCount, full = false)
      }
    }
  }
}

trait YandexKassaV3Helper extends MockitoSupport with ExternalModelGens {

  val yandexKassaApiV3 = mock[YandexKassaApiV3]

  def successPayment(source: ApiModel.PaymentSource): ApiModel.Payment =
    sourceToPayment(source)

  def pendingPayment(source: ApiModel.PaymentSource): ApiModel.Payment =
    sourceToPayment(source, ApiModel.Payment.PaymentStatus.pending)

  def cancelledPayment(source: ApiModel.PaymentSource): ApiModel.Payment =
    sourceToPayment(source, ApiModel.Payment.PaymentStatus.canceled)

  def requestToRefund(rr: ApiModel.RefundRequest, status: ApiModel.Refund.RefundStatus): ApiModel.Refund =
    ApiModel.Refund
      .newBuilder()
      .setAmount(rr.getAmount)
      .setDescription(rr.getDescription)
      .setPaymentId(rr.getPaymentId)
      .setStatus(status)
      .setId("test-refund")
      .setCreatedAt(DateTimeUtils.IsoDateTimeFormatter.print(DateTimeUtils.now()))
      .build()

  def successRefund(rr: ApiModel.RefundRequest): ApiModel.Refund =
    requestToRefund(rr, ApiModel.Refund.RefundStatus.succeeded)

  def canceledRefund(rr: ApiModel.RefundRequest): ApiModel.Refund =
    requestToRefund(rr, ApiModel.Refund.RefundStatus.canceled)

  def sourceToPayment(
      source: ApiModel.PaymentSource,
      status: ApiModel.Payment.PaymentStatus = ApiModel.Payment.PaymentStatus.succeeded): ApiModel.Payment = {
    val now = DateTimeUtils.IsoDateTimeFormatter.print(DateTimeUtils.now())
    val b = ApiModel.Payment
      .newBuilder()
      .setAmount(source.getAmount)
      .setStatus(status)
      .setId(UUID.randomUUID().toString)
      .setDescription(source.getDescription)
      .setPaymentMethod(ApiModel.PaymentMethod.newBuilder())
      .setPaid(status == ApiModel.Payment.PaymentStatus.succeeded)
      .putAllMetadata(source.getMetadataMap)
      .setCapturedAt(now)
      .setCreatedAt(now)
      .setExpiresAt(now)
    if (source.hasConfirmation) {
      val confirmation = source.getConfirmation.toBuilder
      confirmation.setConfirmationUrl("confirmation-test")
      b.setConfirmation(confirmation)
    }
    if (status == ApiModel.Payment.PaymentStatus.canceled) {
      val party = protoEnum(ApiModel.Payment.CancellationDetail.Party.values().toSeq).next
      val without3dsFailed = CancellationReasons.values.toSeq
        .filterNot(_ == CancellationReasons.`3dsFailed`) // otherwise we get Force3DSException
      val reason = Gen.oneOf(without3dsFailed).next
      val cb = ApiModel.Payment.CancellationDetail.newBuilder()
      cb.setParty(party)
      cb.setReason(reason.toString)
      b.setCancellationDetails(cb)
    }

    b.build()
  }

  def expectedForm(payment: ApiModel.Payment): Form = {
    val meta = payment.getMetadataMap.asScala
    val prId = meta("payment_request_id")
    if (!payment.hasConfirmation) {
      EmptyForm(prId)
    } else if (payment.getConfirmation.getType == Confirmation.Type.redirect) {
      UrlForm(prId, payment.getConfirmation.getConfirmationUrl)
    } else if (payment.getConfirmation.getType == Confirmation.Type.external) {
      EmptyForm(prId, confirmationType = Some(Confirmation.Type.external))
    } else {
      throw new IllegalArgumentException(s"Unexpected confirmation [${payment.getConfirmation}]")
    }
  }

}
