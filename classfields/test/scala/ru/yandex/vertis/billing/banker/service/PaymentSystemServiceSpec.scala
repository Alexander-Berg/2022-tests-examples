package ru.yandex.vertis.billing.banker.service

import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.banker.model.CommonModel.OpaquePayload
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.{RequestFilter, StateFilter}
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.{
  GlobalJdbcAccountTransactionDao,
  JdbcAccountDao,
  JdbcPaymentSystemDao,
  PaymentSystemJdbcAccountTransactionDao
}
import ru.yandex.vertis.billing.banker.dao.util.{CleanableJdbcAccountTransactionDao, CleanableJdbcPaymentSystemDao}
import ru.yandex.vertis.billing.banker.exceptions.Exceptions.{
  DuplicatePaymentIdException,
  PaymentAlreadyRefundedException,
  RefundAlreadyProcessedException,
  UnprocessedRefundAlreadyExistsException
}
import ru.yandex.vertis.billing.banker.model.State._
import ru.yandex.vertis.billing.banker.model._
import ru.yandex.vertis.billing.banker.model.gens.{
  idNoMoreLength,
  incomingPaymentGen,
  paymentGen,
  paymentRequestSourceGen,
  requestReceiptGen,
  PaymentRequestParams,
  PaymentRequestSourceParams,
  Producer,
  RefundReasonGen,
  SimpleJsonGen,
  StateParams
}
import ru.yandex.vertis.billing.banker.payment.impl.{emptyAction, emptyForm}
import ru.yandex.vertis.billing.banker.payment.payload.DefaultPayGatePayloadExtractor
import ru.yandex.vertis.billing.banker.service.PaymentSystemService.MethodsFilter
import ru.yandex.vertis.billing.banker.service.impl.PaymentSystemServiceImpl
import ru.yandex.vertis.billing.banker.util.{AutomatedContext, DateTimeUtils, RequestContext}
import slick.dbio.DBIO

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Failure

trait PaymentSystemServiceSpec
  extends AnyWordSpec
  with Matchers
  with AsyncSpecBase
  with JdbcSpecTemplate
  with BeforeAndAfterEach {

  def psId: PaymentSystemId

  implicit val rc: RequestContext = AutomatedContext("PaymentSystemServiceSpec")

  val accountDao = new JdbcAccountDao(database)
  val account = accountDao.upsert(Account("acc1", "u1")).futureValue.id

  val Operator: User = "operator"

  val sourceParams =
    PaymentRequestSourceParams(account = Some(account), withPayGateContext = Some(false), withYandexUid = Some(false))

  private def paymentParams(id: PaymentId, amount: Funds) = StateParams(
    id = Some(id),
    account = Some(account),
    amount = Some(amount),
    status = Set(State.Statuses.Created),
    stateStatus = Set(State.StateStatuses.Valid)
  )

  val accountTransactionsDao =
    new GlobalJdbcAccountTransactionDao(database) with CleanableJdbcAccountTransactionDao

  val psTransactionsDao =
    new PaymentSystemJdbcAccountTransactionDao(database, psId) with CleanableJdbcAccountTransactionDao

  val dao = new JdbcPaymentSystemDao(database, psId) with CleanableJdbcPaymentSystemDao

  def refundHelperService: RefundHelperService

  val service = new PaymentSystemServiceImpl(database, dao, refundHelperService, DefaultPayGatePayloadExtractor)

  val method = service.getMethods(MethodsFilter.Available).futureValue.head

  def params: PaymentRequestParams =
    PaymentRequestParams(
      method = Some(method),
      source = sourceParams
    ).withState

  override def beforeEach(): Unit = {
    accountTransactionsDao.clean()
    psTransactionsDao.clean()
    dao.cleanPayments().futureValue
    dao.cleanRequests().futureValue
    super.beforeEach()
  }

  def createRequest(amount: Option[Funds] = None): Future[PaymentRequest] = {
    val source = paymentRequestSourceGen(
      sourceParams.copy(amount = amount)
    ).next
    for {
      request <- service.createPaymentRequest(method, source, emptyForm, emptyAction)
      PaymentWithRequest(updatedRequest, _) <- service.upsertPayment(
        incomingPaymentGen(paymentParams(request.id, request.source.amount)).next
      )
    } yield updatedRequest
  }

  def createProcessedRequest(amount: Option[Funds] = None): Future[PaymentRequest] = {
    createRequest(amount).flatMap { paymentRequest =>
      service.processed(paymentRequest.state.get).map { _ =>
        paymentRequest
      }
    }
  }

  def getRefundSourceData(amount: Funds): RefundPaymentRequest.SourceData = {
    RefundPaymentRequest.SourceData(
      idNoMoreLength(128).next,
      Some(RefundReasonGen.next.name()),
      Gen.option(SimpleJsonGen).next,
      Some(requestReceiptGen(amount).next)
    )
  }

  def getOrCreateRefundPaymentRequest(
      paymentRequestId: PaymentRequestId,
      refundAmount: Funds,
      sourceData: RefundPaymentRequest.SourceData): Future[RefundPaymentRequest] =
    service.getOrCreateRefundPaymentRequest(
      paymentRequestId,
      refundAmount,
      Operator,
      sourceData
    )

  def checkRefundFailWithBadPayment[T <: AnyRef: ClassTag](
      paymentId: PaymentId,
      sourceData: RefundPaymentRequest.SourceData,
      request: PaymentRequest
    )(update: Incoming => Incoming): Unit = {
    val payment = service.getPayments(StateFilter.ForId(paymentId)).futureValue.head match {
      case i: Incoming =>
        i
      case other =>
        fail(s"Unexpected $other")
    }

    val paymentUpdate = update(payment)
    service.upsertPayment(paymentUpdate).futureValue

    intercept[T] {
      service
        .getOrCreateRefundPaymentRequest(
          payment.id,
          payment.amount,
          Operator,
          sourceData
        )
        .await
    }
    ()
  }

  def createRefundRequest(
      paymentRequestId: PaymentRequestId,
      expectedAmount: Funds,
      actualAmount: Funds,
      status: Statuses.Value = Statuses.Processed): Future[State.Refund] = {
    val refundSourceData = getRefundSourceData(actualAmount)

    getOrCreateRefundPaymentRequest(
      paymentRequestId,
      expectedAmount,
      refundSourceData
    ).flatMap { refundRequest =>
      val refund = State.Refund(
        refundRequest.id,
        refundRequest.account,
        refundRequest.source.amount,
        DateTimeUtils.now(),
        Raw.Empty,
        status,
        StateStatuses.Valid
      )

      service.upsertRefund(refund)
    }
  }

  def createRefundRequestWithFullAmount(
      paymentRequestId: PaymentRequestId,
      amount: Funds,
      status: Statuses.Value = Statuses.Processed): Future[State.Refund] = {
    createRefundRequest(paymentRequestId, amount, amount, status)
  }

  def paymentSystemRefundRequestSpecialCases(): Unit = ()

  val MinRefundAmount = 101L
  val MinPaymentRestAmount = 101L

  def split(paymentAmount: Funds, parts: Seq[Funds] = Seq.empty): Seq[Funds] =
    if (paymentAmount == 0) {
      parts
    } else {
      val refundAmount = Gen.chooseNum(MinRefundAmount, paymentAmount).next
      val actualRefundAmount =
        if (paymentAmount - refundAmount < MinPaymentRestAmount) {
          paymentAmount
        } else {
          refundAmount
        }
      split(paymentAmount - actualRefundAmount, parts :+ actualRefundAmount)
    }

  val RefundPartsGen: Gen[Seq[Funds]] = for {
    amount <- Gen.chooseNum(100000L, 100000000L)
  } yield split(amount)

  "PaymentSystemService" should {
    "get methods from dao" in {
      service.getMethods(MethodsFilter.Available).futureValue should not be empty
    }

    "insert request, payment and get them" in {
      val id = "rq1"
      val source = paymentRequestSourceGen(sourceParams.copy(id = Some(id))).next
      val state = paymentGen(paymentParams(id, source.amount)).next

      service.getPaymentRequests(RequestFilter.ForIds(id)).futureValue shouldBe empty
      service.getPayments(StateFilter.ForId(id)).futureValue shouldBe empty

      val request = service.createPaymentRequest(method, source, emptyForm, emptyAction).futureValue

      service.getPaymentRequest(id).futureValue shouldBe request.copy(state = None)
      service.getPayments(StateFilter.ForId(id)).futureValue shouldBe empty

      val PaymentWithRequest(updatedRequest, State.EnrichedPayment(payment, _, _, _)) =
        service.upsertPayment(state).futureValue
      service.getPaymentRequest(id).futureValue shouldBe updatedRequest

      service.getPayments(StateFilter.ForId(id)).futureValue should (have size 1 and contain(payment))
    }

    "get requests" in {
      val withState = {
        val sources = paymentRequestSourceGen(sourceParams).next(5).toList
        sources.map { s =>
          val request = service.createPaymentRequest(method, s, emptyForm, emptyAction).futureValue
          val PaymentWithRequest(updatedRequest, _) = service
            .upsertPayment(
              paymentGen(paymentParams(request.id, request.source.amount)).next
            )
            .futureValue
          updatedRequest
        }
      }
      val withoutState = {
        val sources = paymentRequestSourceGen(sourceParams).next(5)
        Future.traverse(sources)(service.createPaymentRequest(method, _, emptyForm, emptyAction)).futureValue
      }

      val first = withState.take(2) ++ withoutState.take(2)

      service.getPaymentRequests(RequestFilter.ForIds(first.map(_.id))).futureValue should
        contain theSameElementsAs first

      service.getPaymentRequests(RequestFilter.WithoutState).futureValue should
        contain theSameElementsAs withoutState
    }

    "do not create payment request with duplicate id" in {
      val source = paymentRequestSourceGen(sourceParams).next
      val request = service.createPaymentRequest(method, source, emptyForm, emptyAction).futureValue

      val withProvidedId = source.copy(options = source.options.copy(id = Some(request.id)))
      intercept[DuplicatePaymentIdException] {
        service.createPaymentRequest(method, withProvidedId, emptyForm, emptyAction).await
      }
    }

    "do not create payment request when form saver action fails" in {
      val source = paymentRequestSourceGen(sourceParams).next
      val failedAction = () => DBIO.failed(new IllegalStateException())

      val request = service.createPaymentRequest(method, source, emptyForm, failedAction)
      intercept[IllegalStateException](request.await)
    }

    "correctly mark payment as processed" in {
      val request = createRequest().futureValue

      val payment = request.state.get
      payment.status shouldBe Statuses.Created

      val (bad, old) = payment match {
        case i: Incoming =>
          (i.copy(epoch = None), i.copy(epoch = i.epoch.map(_ - 1000)))
        case ti: TotalIncoming =>
          (ti.copy(epoch = None), ti.copy(epoch = ti.epoch.map(_ - 1000)))
      }

      service.processed(bad).toTry should matchPattern { case Failure(_: IllegalArgumentException) =>
      }
      service.processed(old).futureValue

      service.getPayments(StateFilter.ForId(payment.id)).futureValue.head.status shouldBe Statuses.Created

      service.processed(payment).futureValue
      service.getPayments(StateFilter.ForId(payment.id)).futureValue.head.status shouldBe Statuses.Processed
    }

    "create refund payment request" in {
      val paymentRequest = createProcessedRequest().futureValue
      val desiredAmount = paymentRequest.source.amount
      val refundSourceData = getRefundSourceData(desiredAmount)

      val createRefundRequest = service
        .getOrCreateRefundPaymentRequest(
          paymentRequest.id,
          desiredAmount,
          Operator,
          refundSourceData
        )
        .futureValue

      createRefundRequest.method shouldBe method
      createRefundRequest.account shouldBe paymentRequest.source.account
      createRefundRequest.source.refundFor shouldBe paymentRequest.id
      createRefundRequest.source.amount shouldBe desiredAmount
      createRefundRequest.source.payload.user shouldBe Operator
      createRefundRequest.source.payload.comment shouldBe refundSourceData.comment
      createRefundRequest.source.payload.reason shouldBe refundSourceData.reason
        .map(OpaquePayload.RefundPayload.Reason.valueOf)
        .getOrElse(OpaquePayload.RefundPayload.Reason.UNKNOWN)
      createRefundRequest.source.payload.value shouldBe refundSourceData.jsonPayload
      createRefundRequest.source.optReceiptData shouldBe refundSourceData.receipt
      createRefundRequest.state.isDefined shouldBe false

      val refundId = createRefundRequest.id

      service.getRefundPaymentRequest(refundId).futureValue shouldBe createRefundRequest
      service.getRefunds(StateFilter.ForId(refundId)).futureValue.isEmpty shouldBe true

      val refund = State.Refund(
        createRefundRequest.id,
        createRefundRequest.account,
        createRefundRequest.source.amount,
        DateTimeUtils.now(),
        Raw.Empty
      )

      val refundCreate = service.upsertRefund(refund).futureValue
      val expectedCreateRefundRequest = createRefundRequest.copy(state = Some(refundCreate))
      val actualRefundRequest = service.getRefundPaymentRequest(refundId).futureValue
      actualRefundRequest shouldBe expectedCreateRefundRequest

      val actualRefund = service.getRefunds(StateFilter.ForId(refundId)).futureValue
      actualRefund should (have size 1 and contain(refundCreate))

      val getRefundRequest = service
        .getOrCreateRefundPaymentRequest(
          paymentRequest.id,
          desiredAmount,
          Operator,
          refundSourceData
        )
        .futureValue

      getRefundRequest shouldBe expectedCreateRefundRequest
    }

    "create and get refund payment request when refund request without state" in {
      val paymentRequest = createProcessedRequest().futureValue
      val desiredAmount = paymentRequest.source.amount
      val refundSourceData = getRefundSourceData(desiredAmount)

      val createRefundRequest = service
        .getOrCreateRefundPaymentRequest(
          paymentRequest.id,
          desiredAmount,
          Operator,
          refundSourceData
        )
        .futureValue

      val getRefundRequest = service
        .getOrCreateRefundPaymentRequest(
          paymentRequest.id,
          desiredAmount,
          Operator,
          refundSourceData
        )
        .futureValue

      getRefundRequest shouldBe createRefundRequest
    }

    "create and get refund payment request when refund request with unprocessed state" in {
      val paymentRequest = createProcessedRequest().futureValue
      val desiredAmount = paymentRequest.source.amount
      val refundSourceData = getRefundSourceData(desiredAmount)

      val createRefundRequest = service
        .getOrCreateRefundPaymentRequest(
          paymentRequest.id,
          desiredAmount,
          Operator,
          refundSourceData
        )
        .futureValue

      val refund = State.Refund(
        createRefundRequest.id,
        createRefundRequest.account,
        createRefundRequest.source.amount,
        DateTimeUtils.now(),
        Raw.Empty
      )

      val refundCreate = service.upsertRefund(refund).futureValue
      val expectedCreateRefundRequest = createRefundRequest.copy(state = Some(refundCreate))

      val getRefundRequest = service
        .getOrCreateRefundPaymentRequest(
          paymentRequest.id,
          desiredAmount,
          Operator,
          refundSourceData
        )
        .futureValue

      getRefundRequest shouldBe expectedCreateRefundRequest
    }

    "fail create refund payment request when amount already refunded" in {
      val paymentRequest = createProcessedRequest().futureValue
      val desiredAmount = paymentRequest.source.amount
      val refundSourceData = getRefundSourceData(desiredAmount)

      val createRefundRequest = service
        .getOrCreateRefundPaymentRequest(
          paymentRequest.id,
          desiredAmount,
          Operator,
          refundSourceData
        )
        .futureValue

      val refund = State.Refund(
        createRefundRequest.id,
        createRefundRequest.account,
        createRefundRequest.source.amount,
        DateTimeUtils.now(),
        Raw.Empty,
        Statuses.Processed,
        StateStatuses.Valid
      )

      service.upsertRefund(refund).futureValue

      intercept[RefundAlreadyProcessedException] {
        service
          .getOrCreateRefundPaymentRequest(
            paymentRequest.id,
            desiredAmount,
            Operator,
            refundSourceData
          )
          .await
      }
    }

    "create refund payment request when got only cancelled refund request" in {
      val paymentRequest = createProcessedRequest().futureValue
      val desiredAmount = paymentRequest.source.amount
      val refundSourceData = getRefundSourceData(desiredAmount)

      val firstRefundRequest = service
        .getOrCreateRefundPaymentRequest(
          paymentRequest.id,
          desiredAmount,
          Operator,
          refundSourceData
        )
        .futureValue

      val refund = State.Refund(
        firstRefundRequest.id,
        firstRefundRequest.account,
        firstRefundRequest.source.amount,
        DateTimeUtils.now(),
        Raw.Empty,
        Statuses.Processed,
        StateStatuses.Cancelled
      )

      service.upsertRefund(refund).futureValue

      val secondRefundRequest = service
        .getOrCreateRefundPaymentRequest(
          paymentRequest.id,
          desiredAmount,
          Operator,
          refundSourceData
        )
        .futureValue

      secondRefundRequest.id should not be equal(firstRefundRequest.id)
      secondRefundRequest.method shouldBe firstRefundRequest.method
      secondRefundRequest.account shouldBe firstRefundRequest.account
      secondRefundRequest.source shouldBe firstRefundRequest.source
      secondRefundRequest.state.isEmpty shouldBe true
    }

    "correctly update refund state status" in {
      val paymentRequest = createProcessedRequest().futureValue
      val desiredAmount = paymentRequest.source.amount

      val refund = createRefundRequestWithFullAmount(paymentRequest.id, desiredAmount).futureValue

      val refundCancel = refund.copy(stateStatus = StateStatuses.Cancelled)
      val cancelledRefund = service.upsertRefund(refundCancel).futureValue
      cancelledRefund.copy(epoch = None) shouldBe refundCancel.copy(epoch = None)

      intercept[IllegalArgumentException] {
        refund.copy(stateStatus = StateStatuses.Refunded)
      }
    }

    "fail creation of refund payment request with bad payment" in {
      val request = createProcessedRequest().futureValue

      val payment = request.state.get match {
        case i: Incoming =>
          i
        case other =>
          fail(s"Unexpected $other")
      }

      val validRefundSource = getRefundSourceData(request.source.amount)

      val stateChecker: (Incoming => Incoming) => Unit =
        checkRefundFailWithBadPayment[IllegalStateException](payment.id, validRefundSource, request)(_)

      stateChecker { payment =>
        payment.copy(status = Statuses.Created)
      }

      stateChecker { payment =>
        payment.copy(status = Statuses.Processed, stateStatus = StateStatuses.Cancelled)
      }

      checkRefundFailWithBadPayment[PaymentAlreadyRefundedException](
        payment.id,
        validRefundSource,
        request
      ) { payment =>
        payment.copy(status = Statuses.Processed, stateStatus = StateStatuses.Refunded)
      }
    }

    "fail creation of refund payment request with bad amount" in {
      val paymentRequest = createProcessedRequest().futureValue

      intercept[IllegalArgumentException] {
        createRefundRequestWithFullAmount(
          paymentRequest.id,
          paymentRequest.source.amount + 1
        ).await
      }
    }

    "fail creation of next refund payment request when previous is unprocessed" in {
      val fullAmount = 100000L
      val halfAmount = fullAmount / 2
      val paymentRequest = createProcessedRequest(Some(fullAmount)).futureValue

      createRefundRequestWithFullAmount(paymentRequest.id, halfAmount, Statuses.Created).futureValue

      intercept[UnprocessedRefundAlreadyExistsException] {
        createRefundRequest(paymentRequest.id, fullAmount, halfAmount).await
      }
    }

    "fail creation of refund payment request when refund amount greater than rest" in {
      val fullAmount = 100000L
      val halfAmount = fullAmount / 2
      val paymentRequest = createProcessedRequest(Some(fullAmount)).futureValue

      createRefundRequestWithFullAmount(paymentRequest.id, halfAmount).futureValue

      intercept[IllegalArgumentException] {
        createRefundRequestWithFullAmount(paymentRequest.id, fullAmount + halfAmount).await
      }
    }

    "create refund requests for full refund by parts" in {
      val fullAmount = 100000L
      val paymentRequest = createProcessedRequest(Some(fullAmount)).futureValue

      val refundAmounts = Seq(50000L, 25000L, 12500L, 6000L, 6500L)

      refundAmounts.foldLeft(0L) { (refunded, refundAmount) =>
        createRefundRequest(paymentRequest.id, refundAmount + refunded, refundAmount).futureValue
        refunded + refundAmount
      }

    }

    behave.like(paymentSystemRefundRequestSpecialCases())

  }
}
