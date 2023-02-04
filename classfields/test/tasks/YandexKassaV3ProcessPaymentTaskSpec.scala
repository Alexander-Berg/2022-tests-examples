package ru.yandex.vertis.billing.banker.tasks

import org.joda.time.DateTime
import org.mockito.Mockito
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.banker.{ArtificialException, AsyncSpecBase}
import ru.yandex.vertis.billing.banker.dao.YandexKassaRecurrentDao.Record
import ru.yandex.vertis.billing.banker.dao.YandexKassaV3PaymentRequestExternalDao
import ru.yandex.vertis.billing.banker.dao.YandexKassaV3PaymentRequestExternalDao.{
  ForExternalId,
  ForInternalId,
  Patch,
  RecordStatuses
}
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.{
  GlobalJdbcAccountTransactionDao,
  JdbcAccountDao,
  JdbcPaymentRequestMetaDao,
  JdbcPaymentSystemDao,
  JdbcYandexKassaV3PaymentRequestExternalDao,
  JdbcYandexKassaV3RecurrentDao,
  PaymentSystemJdbcAccountTransactionDao
}
import ru.yandex.vertis.billing.banker.dao.util.CleanableJdbcYandexKassaV3PaymentRequestExternalDao
import ru.yandex.vertis.billing.banker.exceptions.Exceptions.CancellationPaymentException
import ru.yandex.vertis.billing.banker.model.PaymentRequest.Source
import ru.yandex.vertis.billing.banker.model.State.StateStatuses
import ru.yandex.vertis.billing.banker.model.YandexKassaV3Context.BankCard
import ru.yandex.vertis.billing.banker.model._
import ru.yandex.vertis.billing.banker.model.gens.{
  paymentRequestGen,
  paymentRequestSourceGen,
  readableString,
  yandexkassaV3PaygateContext,
  PaymentRequestSourceParams,
  Producer
}
import ru.yandex.vertis.billing.banker.payment.bruteforce.CardBruteforceBreaker
import ru.yandex.vertis.billing.banker.payment.impl.{YandexKassaV3PaymentHelper, YandexKassaV3PaymentSupport}
import ru.yandex.vertis.billing.banker.payment.payload.YandexKassaV3PayloadExtractor
import ru.yandex.vertis.billing.banker.service.YandexKassaV3RefundHelperService
import ru.yandex.vertis.billing.banker.service.impl._
import ru.yandex.vertis.billing.banker.service.refund.processor.YandexKassaV3RefundProcessor
import ru.yandex.vertis.billing.banker.tasks.ReceiptCommitTaskSpec.EpochServiceMock
import ru.yandex.vertis.billing.banker.tasks.YandexKassaV3ProcessPaymentTask.PendingPaymentsEpochMark
import ru.yandex.vertis.billing.banker.util.CollectionUtils.RichTraversableLike
import ru.yandex.vertis.billing.banker.util.{AutomatedContext, DateTimeUtils}
import ru.yandex.vertis.billing.yandexkassa.api.YandexKassaApiV3
import ru.yandex.vertis.billing.yandexkassa.api.model.{IdempotencyKey, PaymentId}
import ru.yandex.vertis.external.yandexkassa.ApiModel
import ru.yandex.vertis.external.yandexkassa.ApiModel.{
  Confirmation,
  Payment => KassaPayment,
  PaymentCapture,
  PaymentSource
}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
  * Spec on [[YandexKassaV3ProcessPaymentTask]]
  *
  * @author ruslansd
  */
class YandexKassaV3ProcessPaymentTaskSpec
  extends Matchers
  with AnyWordSpecLike
  with JdbcSpecTemplate
  with MockitoSupport
  with AsyncSpecBase
  with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    Mockito.clearInvocations(kassaMock)
    payments.clear()
    epochService.reset()
    externalDao.clean().futureValue
    super.beforeEach()
  }

  implicit private val ac = AutomatedContext("YandexKassaV3PaymentSupportSpec")

  private val paymentDao = new JdbcPaymentSystemDao(database, PaymentSystemIds.YandexKassaV3)

  private val accountTransactionsDao =
    new GlobalJdbcAccountTransactionDao(database)

  private val psTransactionsDao =
    new PaymentSystemJdbcAccountTransactionDao(database, PaymentSystemIds.YandexKassaV3)

  private val externalDao =
    new JdbcYandexKassaV3PaymentRequestExternalDao(database) with CleanableJdbcYandexKassaV3PaymentRequestExternalDao

  private val kassaMock = mock[YandexKassaApiV3]

  val refundProcessor = new YandexKassaV3RefundProcessor(kassaMock)

  private val refundHelperService = new RefundHelperServiceImpl(
    database,
    accountTransactionsDao,
    refundProcessor,
    psTransactionsDao,
    paymentDao
  ) with YandexKassaV3RefundHelperService {
    override val externalRequestDao: YandexKassaV3PaymentRequestExternalDao = externalDao
  }

  private val paymentService = new PaymentSystemServiceImpl(
    database,
    paymentDao,
    refundHelperService,
    new YandexKassaV3PayloadExtractor(externalDao)
  )

  private val accountDao = new JdbcAccountDao(database)
  private val accountService = new AccountServiceImpl(accountDao)

  private val accountTransactions = new GlobalAccountTransactionService(accountTransactionsDao, None)

  private val recurrentDao = new JdbcYandexKassaV3RecurrentDao(database)
  private val metaDao = new JdbcPaymentRequestMetaDao(database)

  private val recurrentService = new YandexKassaRecurrentServiceImpl(recurrentDao)

  private val helper = new YandexKassaV3PaymentHelper(
    kassaMock,
    externalDao,
    recurrentService,
    paymentService,
    ""
  )

  private val support = new YandexKassaV3PaymentSupport(
    paymentService,
    accountService,
    accountTransactions,
    recurrentService,
    kassaMock,
    metaDao,
    externalDao,
    "https://test.ru",
    None,
    None,
    "",
    CardBruteforceBreaker.Noop,
    is3dsEnabledForYandexKassa = false
  )

  private val payments = collection.mutable.HashMap.empty[String, KassaPayment]

  private val epochService = EpochServiceMock()

  private val batchSize = 5

  private val task =
    new YandexKassaV3ProcessPaymentTask(externalDao, helper, kassaMock, epochService, -10.minutes, batchSize)

  "YandexKassaV3ProcessPaymentTask" should {
    "correctly work on empty set" in {
      task.execute().futureValue
    }

    "correctly work on waiting for capture" in {
      val count = 5
      val prs = prGen().next(count).toList
      val requests = prepareWaitingForCapturePayments(prs)

      mockCapture(KassaPayment.PaymentStatus.waiting_for_capture)
      task.execute().futureValue

      requests.foreach { r =>
        val record = externalDao.getOpt(ForInternalId(r.id)).futureValue
        record.map(_.status) shouldBe Some(RecordStatuses.Succeeded)
      }

      prs.zip(requests).foreach { case (s, pr) =>
        val request = support.getPaymentRequest(s.source.account, pr.id).futureValue
        request.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)
      }

      Mockito.verify(kassaMock, Mockito.times(count)).capture(?, ?)(?, ?)
      Mockito.verify(kassaMock, Mockito.times(0)).cancel(?)(?, ?)
    }

    "correctly work on success records" in {
      mockPayments()
      mockCapture(KassaPayment.PaymentStatus.succeeded)
      val count = 5
      val prs = prGen().next(count).toList
      val requests = prs.map { pr =>
        support.request(pr.source.account, method(pr.source), pr.source).futureValue
      }

      requests.foreach { r =>
        val record = externalDao.getOpt(ForInternalId(r.id)).futureValue
        record.map(_.status) shouldBe Some(RecordStatuses.Succeeded)
      }

      Mockito.verify(kassaMock, Mockito.times(count)).capture(?, ?)(?, ?)
      Mockito.clearInvocations(kassaMock)

      task.execute().futureValue

      requests.foreach { r =>
        val record = externalDao.getOpt(ForInternalId(r.id)).futureValue
        record.map(_.status) shouldBe Some(RecordStatuses.Succeeded)
      }

      prs.zip(requests).foreach { case (s, pr) =>
        val request = support.getPaymentRequest(s.source.account, pr.id).futureValue
        request.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)
      }

      Mockito.verifyNoInteractions(kassaMock)
    }

    "correctly work on pending records" in {
      mockPayments(status = KassaPayment.PaymentStatus.pending)
      val count = 5
      val prs = prGen().next(count).toList
      val requests = prs.map { pr =>
        support.request(pr.source.account, method(pr.source), pr.source).futureValue
      }

      requests.foreach { r =>
        val record = externalDao.getOpt(ForInternalId(r.id)).futureValue
        record.map(_.status) shouldBe Some(RecordStatuses.Created)
      }

      prs.zip(requests).foreach { case (s, pr) =>
        val request = support.getPaymentRequest(s.source.account, pr.id).futureValue
        request.state shouldBe None
      }

      mockGetPayment(KassaPayment.PaymentStatus.succeeded)
      task.execute().futureValue

      requests.foreach { r =>
        val record = externalDao.getOpt(ForInternalId(r.id)).futureValue
        record.map(_.status) shouldBe Some(RecordStatuses.Succeeded)
      }

      prs.zip(requests).foreach { case (s, pr) =>
        val request = support.getPaymentRequest(s.source.account, pr.id).futureValue
        request.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)
      }

      Mockito.verify(kassaMock, Mockito.times(count)).get(?)(?)
    }

    "correctly work on pending records with duplicate payment" in {
      mockPayments(KassaPayment.PaymentStatus.pending)
      val count = 5
      val externalId = uuid
      val prs = prGen(Some(externalId)).next(count).toList
      val requests = prs.map { pr =>
        support.request(pr.source.account, method(pr.source), pr.source).futureValue
      }

      mockCapture(KassaPayment.PaymentStatus.succeeded)
      mockGetPayment(KassaPayment.PaymentStatus.waiting_for_capture)
      task.execute().futureValue

      val records = externalDao.get(ForExternalId(externalId)).futureValue
      records.count(_.status == RecordStatuses.Cancelled) shouldBe count - 1
      records.count(_.status == RecordStatuses.Succeeded) shouldBe 1

      val statuses = prs.zip(requests).flatMap { case (s, pr) =>
        support.getPaymentRequest(s.source.account, pr.id).futureValue.state.map(_.stateStatus)
      }

      statuses.count(_ == StateStatuses.Valid) shouldBe 1
      statuses.count(_ == StateStatuses.Cancelled) shouldBe count - 1

      Mockito.verify(kassaMock).capture(?, ?)(?, ?)
      Mockito.verify(kassaMock, Mockito.times(count - 1)).cancel(?)(?, ?)
    }

    "try capture all waiting_for_capture payments" in {
      val count = 5
      val prs = prGen().next(count).toList
      val requests = prepareWaitingForCapturePayments(prs)

      val failToCaptureRecord = externalDao.getOpt(ForInternalId(requests.head.id)).futureValue.get

      mockCapture(KassaPayment.PaymentStatus.waiting_for_capture, Set(failToCaptureRecord.pId.get))
      mockGetPayment(KassaPayment.PaymentStatus.waiting_for_capture)
      intercept[ArtificialException] {
        task.execute().await
      }

      requests.foreach { r =>
        val record = externalDao.getOpt(ForInternalId(r.id)).futureValue
        val expectedStatus =
          if (failToCaptureRecord.prId == r.id) {
            RecordStatuses.WaitingForCapture
          } else {
            RecordStatuses.Succeeded
          }
        record.map(_.status) shouldBe Some(expectedStatus)
      }

      prs.zip(requests).foreach { case (s, pr) =>
        val request = support.getPaymentRequest(s.source.account, pr.id).futureValue
        request.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)
      }

      Mockito.verify(kassaMock, Mockito.times(count)).capture(?, ?)(?, ?)

      mockCapture(KassaPayment.PaymentStatus.waiting_for_capture)
      Mockito.clearInvocations(kassaMock)
      task.execute().futureValue
      Mockito.verify(kassaMock).capture(?, ?)(?, ?)
    }

    "do not cancel cancelled payments" in {
      mockPayments(KassaPayment.PaymentStatus.canceled)
      mockGetPayment(KassaPayment.PaymentStatus.canceled)
      val count = 5
      val prs = prGen().next(count).toList
      prs.map { pr =>
        intercept[CancellationPaymentException] {
          support.request(pr.source.account, method(pr.source), pr.source).await
        }
      }

      Mockito.verify(kassaMock, Mockito.times(0)).cancel(?)(?, ?)
      Mockito.verify(kassaMock, Mockito.times(0)).capture(?, ?)(?, ?)

      task.execute().futureValue
      Mockito.verify(kassaMock, Mockito.times(0)).cancel(?)(?, ?)
      Mockito.verify(kassaMock, Mockito.times(0)).capture(?, ?)(?, ?)
    }

    "fail if not found pending payment" in {
      mockPayments(status = KassaPayment.PaymentStatus.pending)
      val pr = prGen().next
      val request = support.request(pr.source.account, method(pr.source), pr.source).futureValue
      val record = externalDao.getOpt(ForInternalId(request.id)).futureValue.get

      mockGetPayment(KassaPayment.PaymentStatus.pending, Set(record.pId.get))

      intercept[NoSuchElementException] {
        task.execute().await
      }

      mockGetPayment(KassaPayment.PaymentStatus.pending, createTime = Some(DateTimeUtils.now().minusDays(1)))
      task.execute().await

      val cancelled = externalDao.getOpt(ForInternalId(request.id)).futureValue.get
      cancelled.status shouldBe RecordStatuses.Cancelled
    }

    "do not capture on success payments" in {
      val count = 5
      val prs = prGen().next(count).toList
      val requests = prepareWaitingForCapturePayments(prs)

      mockCapture(KassaPayment.PaymentStatus.succeeded)
      task.execute().futureValue

      requests.foreach { r =>
        val record = externalDao.getOpt(ForInternalId(r.id)).futureValue
        record.map(_.status) shouldBe Some(RecordStatuses.Succeeded)
      }

      prs.zip(requests).foreach { case (s, pr) =>
        val request = support.getPaymentRequest(s.source.account, pr.id).futureValue
        request.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)
      }

      Mockito.verify(kassaMock, Mockito.times(0)).capture(?, ?)(?, ?)
      Mockito.verify(kassaMock, Mockito.times(0)).cancel(?)(?, ?)
    }

    "fail if unexpected status" in {
      val count = 5
      val prs = prGen().next(count).toList
      prepareWaitingForCapturePayments(prs)
      (KassaPayment.PaymentStatus.values().toSet --
        Set(KassaPayment.PaymentStatus.succeeded, KassaPayment.PaymentStatus.waiting_for_capture)).foreach { s =>
        mockGetPayment(s)

        intercept[IllegalArgumentException] {
          task.execute().await
        }
      }
    }

    "fail too old pending payments" in {
      val count = 5
      val prs = prGen().next(count).toList
      val payments = preparePendingPayments(prs)

      task.execute().futureValue

      payments.foreach { p =>
        val record = externalDao.getOpt(ForInternalId(p.id)).futureValue
        record.map(_.status) shouldBe Some(RecordStatuses.Cancelled)
      }
    }

    "do nothing on cancel payment with undefined payment id" in {
      when(kassaMock.createPayment(?)(?, ?))
        .thenReturn(Future.failed(ArtificialException()))
      val externalId = uuid
      val pr = prGen(externalId = Some(externalId)).next
      intercept[ArtificialException] {
        support.request(pr.source.account, method(pr.source), pr.source).await
      }

      Mockito.verify(kassaMock, Mockito.times(0)).cancel(?)(?, ?)
      Mockito.verify(kassaMock, Mockito.times(0)).capture(?, ?)(?, ?)

      val record = externalDao.get(ForExternalId(externalId)).futureValue.exactlyOne
      externalDao.updateStatus(ForInternalId(record.prId), Patch(RecordStatuses.Cancelled)).futureValue

      task.execute().futureValue
      Mockito.verify(kassaMock, Mockito.times(0)).cancel(?)(?, ?)
      Mockito.verify(kassaMock, Mockito.times(0)).capture(?, ?)(?, ?)

    }

    "correctly work in batches" in {
      // изначально 20 платежей
      val count = 20
      mockPayments(status = KassaPayment.PaymentStatus.pending)
      val prs = prGen().next(count).toList
      val requests = prs.map { pr =>
        support.request(pr.source.account, method(pr.source), pr.source).futureValue
      }

      // пусть платёж #5 и #20 фейлятся
      val fifthRecord = externalDao.getOpt(ForInternalId(requests(4).id)).futureValue.get
      val lastRecord = externalDao.getOpt(ForInternalId(requests.last.id)).futureValue.get
      mockGetPayment(KassaPayment.PaymentStatus.succeeded, Set(fifthRecord.pId.get, lastRecord.pId.get))

      // сначала все 20 платежей находятся в статусе ожидания
      val records = requests.flatMap(r => externalDao.getOpt(ForInternalId(r.id)).futureValue)
      records.foreach { record =>
        record.status shouldBe RecordStatuses.Created
      }

      // изначально эпоха таски нулевая, запускаем таску первый раз и ждём падения
      epochService.get(PendingPaymentsEpochMark).futureValue shouldBe 0L
      intercept[NoSuchElementException] {
        task.execute().await
      }

      // эпоха таски продвинулась до 4-го платежа, все платежи кроме 5-го и 10-го перешли в статус успеха
      epochService.get(PendingPaymentsEpochMark).futureValue shouldBe records(3).epoch.get
      val succeededRequests = requests
        .flatMap(request => externalDao.getOpt(ForInternalId(request.id)).futureValue)
        .filter(record => record.status == RecordStatuses.Succeeded)
      succeededRequests.size shouldBe 18

      // оставшиеся 2 платежа протухли, запускаем таску повторно
      mockGetPayment(KassaPayment.PaymentStatus.pending, createTime = Some(DateTimeUtils.now().minusDays(1)))
      task.execute().futureValue

      // теперь все платежи перешли в терминальный статус, и эпоха таски снова сдвинулась
      val cancelledRequests = requests
        .flatMap(request => externalDao.getOpt(ForInternalId(request.id)).futureValue)
        .filter(record => record.status == RecordStatuses.Cancelled)
      cancelledRequests.size shouldBe 2
      epochService.get(PendingPaymentsEpochMark).futureValue shouldBe lastRecord.epoch.get
    }
  }

  def uuid: String = UUID.randomUUID().toString

  private def tooOldPendingDateTime: DateTime = {
    val duration = YandexKassaV3ProcessPaymentTask.PaymentPendingTTL + 1.hour
    DateTimeUtils.now().minus(duration.toMillis)
  }

  private def prepareWaitingForCapturePayments(prs: Iterable[PaymentRequest]) = {
    mockPayments()
    mockCapture()
    val count = prs.size
    val requests = prs.map { pr =>
      support.request(pr.source.account, method(pr.source), pr.source).futureValue
    }
    requests.foreach { r =>
      val record = externalDao.getOpt(ForInternalId(r.id)).futureValue
      record.map(_.status) shouldBe Some(RecordStatuses.WaitingForCapture)
    }
    Mockito.verify(kassaMock, Mockito.times(count)).capture(?, ?)(?, ?)
    Mockito.clearInvocations(kassaMock)
    requests
  }

  private def preparePendingPayments(prs: Iterable[PaymentRequest]) = {
    mockPayments(KassaPayment.PaymentStatus.pending)
    mockGetPayment(KassaPayment.PaymentStatus.pending, createTime = Some(tooOldPendingDateTime))

    val requests = prs.map { pr =>
      support.request(pr.source.account, method(pr.source), pr.source).futureValue
    }

    Mockito.verify(kassaMock, Mockito.times(0)).capture(?, ?)(?, ?)
    Mockito.clearInvocations(kassaMock)
    requests
  }

  def prGen(externalId: Option[String] = None): Gen[PaymentRequest] =
    for {
      request <- paymentRequestGen()
      token <- readableString()
      context <- yandexkassaV3PaygateContext(paymentToken = Some(token))
      options = request.source.options.copy(id = externalId)
      source = request.source.copy(options = options, payGateContext = Some(context))
      result = request.copy(source = source)
      _ = createAccount(source)
    } yield result

  def sourceGen(externalId: Option[String] = None): Gen[Source] =
    for {
      source <- paymentRequestSourceGen(PaymentRequestSourceParams(id = externalId))
      context <- yandexkassaV3PaygateContext(paymentToken = Some(uuid))
      withGateContext = source.copy(payGateContext = Some(context))
      _ = createAccount(source)
      _ = addCard(source, context)
    } yield withGateContext

  def createAccount(source: Source): Account =
    accountService.create(Account(source.account, source.account)).futureValue

  def method(source: Source): String =
    source.payGateContext.get.asInstanceOf[YandexKassaV3Context].paymentMethodData.`type`.toString

  def addCard(source: Source, context: YandexKassaV3Context): Unit = context.paymentMethodData match {
    case BankCard(Some(mask)) =>
      recurrentDao
        .upsert(Record(source.account, "12345", mask, DateTimeUtils.now().plusMonths(1).toLocalDate))
        .futureValue
      ()
    case _ =>
  }

  private def mockPayments(
      status: ApiModel.Payment.PaymentStatus = ApiModel.Payment.PaymentStatus.waiting_for_capture,
      confirmationType: Option[Confirmation.Type] = None) = {
    stub(kassaMock.createPayment(_: PaymentSource)(_: IdempotencyKey, _: Traced)) {
      case (source, _, _) if notNull(source) =>
        val builder = ApiModel.Payment
          .newBuilder()
          .setAmount(source.getAmount)
          .putAllMetadata(source.getMetadataMap)
          .setStatus(status)
          .setCreatedAt(nowAsString)
          .setId(uuid)
        if (status != ApiModel.Payment.PaymentStatus.pending) {
          builder.setCapturedAt(nowAsString)
        }
        val confirmation = confirmationType.map { ct =>
          Confirmation
            .newBuilder()
            .setType(ct)
            .build()
        }
        confirmation.foreach(builder.setConfirmation)
        val payment = builder.build()
        payments += (payment.getId -> payment)
        Future.successful(payment)

    }

    stub(kassaMock.cancel(_: PaymentId)(_: IdempotencyKey, _: Traced)) { case (_, _, _) =>
      Future.successful(ApiModel.Payment.newBuilder().build())
    }

  }

  private def mockCapture(
      status: KassaPayment.PaymentStatus = KassaPayment.PaymentStatus.waiting_for_capture,
      failToCaptureIds: Set[String] = Set.empty) = {
    stub(kassaMock.capture(_: PaymentId, _: PaymentCapture)(_: IdempotencyKey, _: Traced)) {
      case (id, pc, _, _) if failToCaptureIds(id) =>
        Future.failed(new ArtificialException)
      case (id, pc, _, _) =>
        payments.foreach { case (pId, payment) =>
          payments.put(pId, payment.toBuilder.setCapturedAt(nowAsString).build())
        }
        Future.successful(
          ApiModel.Payment
            .newBuilder()
            .setStatus(status)
            .build()
        )
    }
    mockGetPayment(status)
  }

  private def mockGetPayment(
      status: KassaPayment.PaymentStatus,
      notFoundPayments: Set[String] = Set.empty,
      createTime: Option[DateTime] = None) = {
    stub(kassaMock.get(_: PaymentId)(_: Traced)) {
      case (id, _) if notFoundPayments(id) =>
        Future.successful(None)
      case (id, _) =>
        val payment = payments.get(id).map { p =>
          val b = p.toBuilder.setStatus(status)
          createTime.foreach(ct => b.setCreatedAt(DateTimeUtils.IsoDateTimeFormatter.print(ct)))
          b
        }
        if (status != KassaPayment.PaymentStatus.pending) {
          Future.successful(payment.map(_.setCapturedAt(nowAsString).build()))
        } else {
          Future.successful(payment.map(_.build()))
        }
    }
  }

  private def nowAsString: String =
    DateTimeUtils.IsoDateTimeFormatter.print(DateTimeUtils.now())

}
