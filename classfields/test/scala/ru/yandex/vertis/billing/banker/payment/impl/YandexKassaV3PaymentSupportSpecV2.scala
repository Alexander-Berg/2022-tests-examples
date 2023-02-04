package ru.yandex.vertis.billing.banker.payment.impl

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.joda.time.LocalDate
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.YandexKassaRecurrentDao.Record
import ru.yandex.vertis.billing.banker.dao.YandexKassaV3PaymentRequestExternalDao.{
  ForExternalId,
  ForInternalId,
  RecordStatuses
}
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.{
  GlobalJdbcAccountTransactionDao,
  JdbcAccountDao,
  JdbcDowntimeMethodDao,
  JdbcPaymentRequestMetaDao,
  JdbcPaymentSystemDao,
  JdbcYandexKassaV3PaymentRequestExternalDao,
  JdbcYandexKassaV3RecurrentDao,
  PaymentSystemJdbcAccountTransactionDao
}
import ru.yandex.vertis.billing.banker.dao.util._
import ru.yandex.vertis.billing.banker.dao.{YandexKassaRecurrentDao, YandexKassaV3PaymentRequestExternalDao}
import ru.yandex.vertis.billing.banker.model.PaymentMethod.CardProperties
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{EmptyForm, Source}
import ru.yandex.vertis.billing.banker.model.State.{AbstractNotificationSource, StateStatuses}
import ru.yandex.vertis.billing.banker.model.YandexKassaV3Context.BankCard
import ru.yandex.vertis.billing.banker.model._
import ru.yandex.vertis.billing.banker.model.gens._
import ru.yandex.vertis.billing.banker.payment.bruteforce.CardBruteforceBreaker
import ru.yandex.vertis.billing.banker.payment.payload.YandexKassaV3PayloadExtractor
import ru.yandex.vertis.billing.banker.service.PaymentSystemSupport.MethodFilter
import ru.yandex.vertis.billing.banker.service.impl._
import ru.yandex.vertis.billing.banker.service.refund.processor.YandexKassaV3RefundProcessor
import ru.yandex.vertis.billing.banker.service.{async, YandexKassaV3RefundHelperService}
import ru.yandex.vertis.billing.banker.util.CollectionUtils.RichTraversableLike
import ru.yandex.vertis.billing.banker.util.{DateTimeUtils, UserContext}
import ru.yandex.vertis.billing.yandexkassa.api.YandexKassaApiV3
import ru.yandex.vertis.billing.yandexkassa.api.model.{IdempotencyKey, PaymentId}
import ru.yandex.vertis.external.yandexkassa.ApiModel
import ru.yandex.vertis.external.yandexkassa.ApiModel.{Confirmation, PaymentCapture, PaymentSource, PaymentType}
import ru.yandex.vertis.external.yandexkassa.NotificationModel.PaymentNotification
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import java.util.UUID
import java.util.concurrent.Executors
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

/**
  * Spec on [[YandexKassaV3PaymentSupport]]
  *
  * @author ruslansd
  */
class YandexKassaV3PaymentSupportSpecV2
  extends AnyWordSpec
  with Matchers
  with BeforeAndAfterEach
  with AsyncSpecBase
  with JdbcSpecTemplate
  with MockitoSupport {

  private val requestsCount = 20

  implicit override def ec: ExecutionContext =
    ExecutionContext.fromExecutor(
      Executors.newFixedThreadPool(
        requestsCount,
        new ThreadFactoryBuilder()
          .setNameFormat("YandexKassaV3PaymentSupportSpecV2-%d")
          .build()
      )
    )

  implicit private val ac: UserContext =
    UserContext("YandexKassaV3PaymentSupportSpec", "I am human. Trust me :)")

  private val paymentsDao =
    new JdbcPaymentSystemDao(database, PaymentSystemIds.YandexKassaV3) with CleanableJdbcPaymentSystemDao

  private val accountTransactionsDao =
    new GlobalJdbcAccountTransactionDao(database) with CleanableJdbcAccountTransactionDao

  private val psTransactionsDao =
    new PaymentSystemJdbcAccountTransactionDao(database, PaymentSystemIds.YandexKassaV3)
      with CleanableJdbcAccountTransactionDao

  private val externalDao =
    new JdbcYandexKassaV3PaymentRequestExternalDao(database) with CleanableJdbcYandexKassaV3PaymentRequestExternalDao

  private val kassaMock = mock[YandexKassaApiV3]

  private val refundProcessor = new YandexKassaV3RefundProcessor(kassaMock)

  private val refundHelperService = new RefundHelperServiceImpl(
    database,
    accountTransactionsDao,
    refundProcessor,
    psTransactionsDao,
    paymentsDao
  ) with YandexKassaV3RefundHelperService {
    override val externalRequestDao: YandexKassaV3PaymentRequestExternalDao = externalDao
  }

  private val paymentService =
    new PaymentSystemServiceImpl(
      database,
      paymentsDao,
      refundHelperService,
      new YandexKassaV3PayloadExtractor(externalDao)
    )

  private val accountDao = new JdbcAccountDao(database) with CleanableJdbcAccountDao
  private val accountService = new AccountServiceImpl(accountDao)

  private val accountTransactionDao =
    new GlobalJdbcAccountTransactionDao(database) with CleanableJdbcAccountTransactionDao
  private val accountTransactions = new GlobalAccountTransactionService(accountTransactionDao, None)

  private val recurrentDao = new JdbcYandexKassaV3RecurrentDao(database) with CleanableJdbcYandexKassaV3RecurrentDao

  private val externalRequestDao =
    new JdbcYandexKassaV3PaymentRequestExternalDao(database) with CleanableJdbcYandexKassaV3PaymentRequestExternalDao
  private val metaDao = new JdbcPaymentRequestMetaDao(database) with CleanableJdbcPaymentRequestMetaDao

  private val downtimePaymentService =
    new DowntimePaymentServiceImpl(
      paymentsDao.psId,
      new JdbcDowntimeMethodDao(database, paymentsDao.psId)
    )

  private val payments = ArrayBuffer.empty[ApiModel.Payment]

  private def payment(id: PaymentRequestId): Option[ApiModel.Payment] =
    payments.find(p => p.getMetadataMap.containsValue(id))

  private def paymentNotification(
      event: ApiModel.Payment.PaymentStatus,
      payment: ApiModel.Payment): PaymentNotification = {
    val paymentTime = DateTimeUtils.IsoDateTimeFormatter.print(DateTimeUtils.now())
    val p = payment.toBuilder
      .setStatus(event)
      .setCapturedAt(paymentTime)
      .setCreatedAt(paymentTime)
      .build()
    PaymentNotification
      .newBuilder()
      .setEvent(event.toString)
      .setObject(p)
      .build()
  }

  private val recurrentService = new YandexKassaRecurrentServiceImpl(recurrentDao)

  private val DefaultGatewayId = "default_gateway_id_666"

  private val support = new YandexKassaV3PaymentSupport(
    paymentService,
    accountService,
    accountTransactions,
    recurrentService,
    kassaMock,
    metaDao,
    externalRequestDao,
    "https://test.ru",
    None,
    None,
    DefaultGatewayId,
    CardBruteforceBreaker.Noop,
    is3dsEnabledForYandexKassa = false
  )

  private val user = "testUser"

  override def beforeEach(): Unit = {
    Mockito.clearInvocations(kassaMock)
    payments.clear()
    recurrentDao.clean(): Unit
  }

  def prGen(
      externalId: Option[String] = None,
      pt: PaymentType = PaymentType.bank_card,
      cardMask: Option[String] = None): Gen[PaymentRequest] =
    for {
      request <- paymentRequestGen()
      context <- yandexkassaV3PaygateContext(Some(pt), cardMask = cardMask)
      options = request.source.options.copy(id = externalId)
      source = request.source.copy(options = options, payGateContext = Some(context))
      result = request.copy(source = source)
      _ = createAccount(source)
    } yield result

  def sourceGen(
      externalId: Option[String] = None,
      pt: PaymentType = PaymentType.bank_card,
      token: Option[String] = None,
      cardMask: Option[String] = None,
      extAmount: Option[Long] = None): Gen[Source] =
    for {
      source <- paymentRequestSourceGen(
        PaymentRequestSourceParams(id = externalId, amount = extAmount, withPayGateContext = Some(false))
      )
      context <- yandexkassaV3PaygateContext(Some(pt), withCardMask = cardMask.isDefined, cardMask = cardMask)
      resultContext = context.copy(paymentToken = token)
      withGateContext = source.copy(payGateContext = Some(resultContext))
      _ = createAccount(source)
      _ = addCard(source, resultContext)
    } yield withGateContext

  def createAccount(source: Source) =
    accountService.create(Account(source.account, source.account)).futureValue

  def method(source: Source) =
    source.payGateContext.get.asInstanceOf[YandexKassaV3Context].paymentMethodData.`type`.toString

  def addCard(source: Source, context: YandexKassaV3Context) = context.paymentMethodData match {
    case BankCard(Some(mask)) =>
      recurrentDao
        .upsert(Record(source.account, "12345", mask, DateTimeUtils.now().plusMonths(1).toLocalDate))
        .futureValue
    case _ =>
  }

  def mockSuccessPayment(
      status: ApiModel.Payment.PaymentStatus = ApiModel.Payment.PaymentStatus.waiting_for_capture,
      confirmationType: Option[Confirmation.Type] = None) = {
    stub(kassaMock.createPayment(_: PaymentSource)(_: IdempotencyKey, _: Traced)) {
      case (source, _, _) if notNull(source) =>
        val now = DateTimeUtils.IsoDateTimeFormatter.print(DateTimeUtils.now())
        val builder = ApiModel.Payment
          .newBuilder()
          .setAmount(source.getAmount)
          .putAllMetadata(source.getMetadataMap)
          .setStatus(status)
          .setId(uuid)
        status match {
          case ApiModel.Payment.PaymentStatus.succeeded => builder.setCapturedAt(now)
          case ApiModel.Payment.PaymentStatus.waiting_for_capture => builder.setCreatedAt(now)
          case ApiModel.Payment.PaymentStatus.canceled => builder.setCreatedAt(now)
          case _ => // noop
        }
        val confirmation = confirmationType.map { ct =>
          Confirmation
            .newBuilder()
            .setType(ct)
            .build()
        }
        confirmation.foreach(builder.setConfirmation)
        val payment = builder.build()
        payments += payment

        Future.successful(payment)

    }

    stub(kassaMock.capture(_: PaymentId, _: PaymentCapture)(_: IdempotencyKey, _: Traced)) { case (id, pc, _, _) =>
      Future.successful(
        ApiModel.Payment
          .newBuilder()
          .setStatus(ApiModel.Payment.PaymentStatus.succeeded)
          .build()
      )
    }

    stub(kassaMock.cancel(_: PaymentId)(_: IdempotencyKey, _: Traced)) { case (_, _, _) =>
      Future.successful(ApiModel.Payment.newBuilder().build())
    }
  }

  def uuid: String = UUID.randomUUID().toString

  "YandexKassaV3PaymentSupport" should {

    "set default gateway_id into" in {
      val pt = PaymentType.bank_card
      val source = sourceGen(Some(uuid), token = Some("1"), pt = pt).next
      mockSuccessPayment(ApiModel.Payment.PaymentStatus.succeeded)
      support.request(source.account, pt.toString, source).futureValue
      val paymentSourceCaptor = ArgumentCaptor.forClass[PaymentSource, PaymentSource](classOf[PaymentSource])
      Mockito.verify(kassaMock).createPayment(paymentSourceCaptor.capture())(?, ?)
      paymentSourceCaptor.getValue.getRecipient.getGatewayId shouldBe DefaultGatewayId
    }

    "correctly handle payment request" in {
      checkPaymentWithoutUserApproveWithoutExternal(Seq(sourceGen().next))
    }

    "make two payments without external id" in {
      val sources = sourceGen().next(2).toList
      checkPaymentWithoutUserApproveWithoutExternal(sources)
    }

    "do not make few payments with same source (without user approve)" in {
      val externalId = uuid
      val sources = sourceGen(externalId = Some(externalId)).next(2).toList
      checkPaymentWithoutUserApproveWithSameSource(sources)
    }

    "do not make few payments with same source (with user approve)" in {
      val externalId = uuid
      val sources = sourceGen(externalId = Some(externalId)).next(2).toList
      checkPaymentWithUserApproveWithSameSource(sources)
    }

    "do make few payment with different tokens" in {
      val externalId = uuid
      val sources = Seq(
        sourceGen(Some(externalId), token = Some("1")).next,
        sourceGen(Some(externalId), token = Some("2")).next
      )
      checkPaymentWithDifferentMethods(sources)
    }

    "provide correct confirmation type" in {
      val externalId = uuid
      val source = sourceGen(Some(externalId), token = Some("1"), pt = PaymentType.alfabank).next

      mockSuccessPayment(ApiModel.Payment.PaymentStatus.pending, Some(Confirmation.Type.external))

      val form = support.request(source.account, PaymentType.alfabank.toString, source).futureValue
      form.confirmationType shouldBe Some(Confirmation.Type.external)
    }

    "return methods depending on source amount " in {
      val externalId = uuid
      val sb = "sberbank"
      val ym = "yandex_money"
      val wm = "webmoney"
      val amountToMissing = Map(
        100L -> Seq.empty,
        1000000L -> Seq.empty,
        1000001L -> Seq(sb),
        1500000L -> Seq(sb),
        1500001L -> Seq(sb, ym),
        6000000L -> Seq(sb, ym),
        6000001L -> Seq(sb, ym, wm)
      )

      val sources = amountToMissing.keys.flatMap { amount =>
        sourceGen(externalId = Some(externalId), extAmount = Some(amount)).next(1)
      }

      sources.foreach { source =>
        val res = support.getMethods(user, MethodFilter.ForSource(source)).futureValue
        res.map(pm => pm.id) should contain noElementsOf amountToMissing(source.amount)
      }
    }

    "not return recurrents if card payment is disabled" in {
      val expireAt = LocalDate.now().plusYears(1)

      val recurrentRecord = YandexKassaRecurrentDao.Record(user, "1", "444444|4444", expireAt)
      recurrentService.upsert(recurrentRecord).futureValue

      val expectedMethod = PaymentMethod(
        ps = PaymentSystemIds.YandexKassaV3,
        id = s"${PaymentType.bank_card}",
        editable = true,
        preferred = Some(false),
        properties = Some(
          CardProperties(
            cddPanMask = "444444|4444",
            expireAt = Some(expireAt),
            verificationRequired = false,
            invoiceId = "1"
          )
        )
      )

      downtimePaymentService.disable(PaymentType.bank_card.toString).futureValue
      val methodsDisabledCard = support.getMethods(user, MethodFilter.All).futureValue
      methodsDisabledCard should not contain expectedMethod

      downtimePaymentService.enable(PaymentType.bank_card.toString).futureValue
      val methodsEnabledCard = support.getMethods(user, MethodFilter.All).futureValue
      methodsEnabledCard should contain(expectedMethod)
    }

    "return restriction with upper bound limit if source not specified" in {
      val res = support.getMethods(user, MethodFilter.All).futureValue
      res.foreach { pm =>
        pm.id match {
          case "sberbank" => pm.restriction shouldBe Some(PaymentMethodRestriction(Some(1000000L)))
          case "yandex_money" => pm.restriction shouldBe Some(PaymentMethodRestriction(Some(1500000L)))
          case "webmoney" => pm.restriction shouldBe Some(PaymentMethodRestriction(Some(6000000L)))
          case _ => pm.restriction shouldBe None
        }
      }
    }

    "two pending payment with different payment methods and with same external id" in {
      val externalId = uuid
      val source1 = sourceGen(Some(externalId), token = Some("1")).next
      val source2 = sourceGen(Some(externalId), token = Some("2")).next

      mockSuccessPayment(ApiModel.Payment.PaymentStatus.pending)

      val request1 = support.request(source1.account, method(source1), source1).futureValue
      val request2 = support.request(source2.account, method(source2), source2).futureValue

      request1.id shouldNot be(request2.id)

      val payment1 = payment(request1.id).get
      val payment2 = payment(request2.id).get

      val successNotification1 = paymentNotification(ApiModel.Payment.PaymentStatus.waiting_for_capture, payment1)
      val successNotification2 = paymentNotification(ApiModel.Payment.PaymentStatus.waiting_for_capture, payment2)

      support.parse(AbstractNotificationSource(successNotification1)).futureValue
      support.parse(AbstractNotificationSource(successNotification2)).futureValue

      val requests = externalRequestDao.get(ForExternalId(externalId)).futureValue
      requests.size shouldBe 2
      requests.count(_.status == RecordStatuses.Succeeded) shouldBe 1
      requests.count(_.status == RecordStatuses.Cancelled) shouldBe 1

      Mockito.verify(kassaMock, Mockito.times(2)).createPayment(?)(?, ?)
      Mockito.verify(kassaMock).cancel(?)(?, ?)
      Mockito.verify(kassaMock).capture(?, ?)(?, ?)

      val pr1 = support.getPaymentRequest(source1.account, request1.id).futureValue
      val pr2 = support.getPaymentRequest(source2.account, request2.id).futureValue

      pr1.state.map(_.stateStatus) shouldBe Some(StateStatuses.Valid)
      pr2.state.map(_.stateStatus) shouldBe Some(StateStatuses.Cancelled)
    }

    "concurrent notification wait_for_capture" in {
      val externalId = uuid
      val sources = (0 until requestsCount).map(id => sourceGen(Some(externalId), token = Some(id.toString)).next)

      mockSuccessPayment(ApiModel.Payment.PaymentStatus.pending)

      val requests = sources.map(s => support.request(s.account, method(s), s).futureValue)
      requests.map(_.id).toSet.size shouldBe requestsCount

      val notifications = requests.map { request =>
        val p = payment(request.id).get
        paymentNotification(ApiModel.Payment.PaymentStatus.waiting_for_capture, p)
      }

      async.inSeries(notifications)(n => support.parse(AbstractNotificationSource(n))).futureValue

      val externalRequests = externalRequestDao.get(ForExternalId(externalId)).futureValue
      externalRequests.size shouldBe requestsCount
      externalRequests.count(_.status == RecordStatuses.Succeeded) shouldBe 1
      externalRequests.count(_.status == RecordStatuses.Cancelled) shouldBe requestsCount - 1

      Mockito.verify(kassaMock, Mockito.times(requestsCount - 1)).cancel(?)(?, ?)
      Mockito.verify(kassaMock).capture(?, ?)(?, ?)

      val prs = sources.zip(requests).map { case (s, r) =>
        support.getPaymentRequest(s.account, r.id).futureValue
      }
      prs.flatMap(_.state).size shouldBe requestsCount
      prs.flatMap(_.state).count(_.stateStatus == StateStatuses.Valid) shouldBe 1
      prs.flatMap(_.state).count(_.stateStatus == StateStatuses.Cancelled) shouldBe requestsCount - 1
    }

  }

  def checkPaymentWithUserApproveWithoutExternal(sources: Seq[Source]): Unit = {
    mockSuccessPayment(ApiModel.Payment.PaymentStatus.pending)
    sources.foreach { source =>
      val pr = support.request(source.account, method(source), source).futureValue
      externalRequestDao.getOpt(ForInternalId(pr.id)).futureValue.get.status shouldBe RecordStatuses.Succeeded
    }
    Mockito.verify(kassaMock, Mockito.times(sources.size)).createPayment(?)(?, ?)
    Mockito.verify(kassaMock, Mockito.times(sources.size)).capture(?, ?)(?, ?): Unit
  }

  def checkPaymentWithUserApproveWithSameSource(sources: Seq[Source]): Unit = {
    val externalId = {
      val externalIds = sources.map(_.options.id.get).toSet
      require(externalIds.size == 1, "Expected sources with same external id")
      externalIds.head
    }
    mockSuccessPayment(ApiModel.Payment.PaymentStatus.pending)

    sources.foreach { source =>
      val form = support.request(source.account, method(source), source).futureValue
      form.isInstanceOf[EmptyForm] shouldBe true
      externalRequestDao
        .getOpt(ForInternalId(form.id))
        .futureValue
        .map(_.status) shouldBe Some(RecordStatuses.Created)
    }
    Mockito.verify(kassaMock, Mockito.times(sources.size)).createPayment(?)(?, ?)
    Mockito.verify(kassaMock, Mockito.times(0)).capture(?, ?)(?, ?)
    externalRequestDao.get(ForExternalId(externalId)).futureValue.exactlyOne.status shouldBe RecordStatuses.Created

    Mockito.reset(kassaMock)
    checkPaymentWithoutUserApproveWithSameSource(sources)
  }

  def checkPaymentWithDifferentMethods(sources: Seq[Source]): Unit = {
    val externalId = {
      val externalIds = sources.map(_.options.id.get).toSet
      require(externalIds.size == 1, "Expected sources with same external id")
      externalIds.head
    }
    mockSuccessPayment()

    sources.foreach { source =>
      val form = support.request(source.account, method(source), source).futureValue
      form.isInstanceOf[EmptyForm] shouldBe true
      externalRequestDao
        .getOpt(ForInternalId(form.id))
        .futureValue
        .map(_.status) shouldBe Some(RecordStatuses.Succeeded)
    }
    Mockito.verify(kassaMock).createPayment(?)(?, ?)
    Mockito.verify(kassaMock).capture(?, ?)(?, ?)
    val requests = externalRequestDao.get(ForExternalId(externalId)).futureValue
    requests.size shouldBe 1
    requests.count(_.status == RecordStatuses.Succeeded) shouldBe 1: Unit
  }

  def checkPaymentWithoutUserApproveWithoutExternal(sources: Seq[Source]): Unit = {
    mockSuccessPayment()
    sources.foreach { source =>
      val pr = support.request(source.account, method(source), source).futureValue
      externalRequestDao.getOpt(ForInternalId(pr.id)).futureValue.get.status shouldBe RecordStatuses.Succeeded
    }
    Mockito.verify(kassaMock, Mockito.times(sources.size)).createPayment(?)(?, ?)
    Mockito.verify(kassaMock, Mockito.times(sources.size)).capture(?, ?)(?, ?): Unit
  }

  def checkPaymentWithoutUserApproveWithSameSource(sources: Seq[Source]): Unit = {
    val externalId = {
      val externalIds = sources.map(_.options.id.get).toSet
      require(externalIds.size == 1, "Expected sources with same external id")
      externalIds.head
    }
    mockSuccessPayment()
    val first = sources.head
    val form = support.request(first.account, method(first), first).futureValue
    form.isInstanceOf[EmptyForm] shouldBe true
    externalRequestDao
      .getOpt(ForInternalId(form.id))
      .futureValue
      .map(_.status) shouldBe Some(RecordStatuses.Succeeded)

    sources.foreach { source =>
      support.request(source.account, method(source), source).futureValue shouldBe form
    }
    Mockito.verify(kassaMock, Mockito.times(1)).createPayment(?)(?, ?)
    Mockito.verify(kassaMock, Mockito.times(1)).capture(?, ?)(?, ?)
    externalRequestDao
      .get(
        ForExternalId(externalId)
      )
      .futureValue
      .exactlyOne
      .status shouldBe RecordStatuses.Succeeded: Unit
  }
}
