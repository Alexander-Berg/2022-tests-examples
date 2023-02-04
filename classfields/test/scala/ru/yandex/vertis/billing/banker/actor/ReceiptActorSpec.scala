package ru.yandex.vertis.billing.banker.actor

import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalamock.scalatest.MockFactory
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.actor.EffectActorProtocol.{Done, Fail}
import ru.yandex.vertis.billing.banker.actor.ReceiptActorSpec.{
  fingerPrintOf,
  getReceipt,
  notRefunded,
  FullOrPartlyRefundedPaymentFormatGen,
  RefundRequestAndPayment,
  RefundRequestAndPaymentGen,
  RefundSecurityDepositPRGen,
  RefundedAccountTransactionGen,
  RefundedPaymentFormatGen,
  SecurityDepositAccountTransactionGen,
  SecurityDepositPRGen,
  ValidAccountTransactionGen,
  ValidPaymentGen
}
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.RequestFilter
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.RequestFilter.RefundsFor
import ru.yandex.vertis.billing.banker.dao.ReceiptDao
import ru.yandex.vertis.billing.banker.model.AccountTransaction.Activities
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{Context, Target, Targets}
import ru.yandex.vertis.billing.banker.model.State.{Incoming, StateStatuses}
import ru.yandex.vertis.billing.banker.model.gens.{
  accountTransactionGen,
  hashAccountTransactionIdGen,
  paymentForRefundGen,
  paymentRequestGen,
  refundRequestGen,
  requestReceiptGen,
  PaymentRequestParams,
  PaymentRequestSourceParams,
  Producer,
  RefundRequestParams,
  RefundRequestSourceParams,
  StateParams
}
import ru.yandex.vertis.billing.banker.model.{
  AccountTransaction,
  AccountTransactions,
  PaymentRequest,
  Receipt,
  ReceiptId,
  ReceiptSentStatuses,
  RefundPaymentRequest,
  State
}
import ru.yandex.vertis.billing.banker.service.ReceiptDeliveryService.SentReceiptResponse
import ru.yandex.vertis.billing.banker.service.impl.ReceiptApiImpl
import ru.yandex.vertis.billing.banker.service.{PaymentSystemService, ReceiptApi, ReceiptService}
import ru.yandex.vertis.billing.banker.util.{AutomatedContext, RequestContext}

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
  * @author ruslansd
  */
class ReceiptActorSpec
  extends ActorSpecBase("ReceiptActorSpec")
  with AsyncSpecBase
  with MockFactory
  with ShrinkLowPriority {

  "ReceiptActorSpec" should {
    "process valid payment request" when {
      "receipt not stored" in new TestPaymentRequest {
        val paymentRequest = ValidPaymentGen.next
        mockNotStored(paymentRequest)
        checkSendSuccess(paymentRequest)
      }
      "receipt stored" in new TestPaymentRequest {
        val paymentRequest = ValidPaymentGen.next
        mockStored(paymentRequest)
        checkSendSuccess(paymentRequest)
      }
      "receipt stored and already sent" in new TestPaymentRequest {
        val paymentRequest = ValidPaymentGen.next
        mockGetReceipt(paymentRequest, ReceiptSentStatuses.OK)
        checkSendSuccess(paymentRequest)
      }
    }
    "fail processing of valid payment request" when {
      "one of steps fail" in new TestPaymentRequest {
        checkStepFail(
          ValidPaymentGen,
          paymentRequestSteps
        )
      }
    }
    "process refunded old format payment request" when {
      "receipt not stored" in new TestPaymentRequest {
        val paymentRequest = RefundedPaymentFormatGen.next
        mockGetRefundPaymentRequests(paymentRequest)
        mockNotStored(paymentRequest)
        mockNotStored(notRefunded(paymentRequest))
        checkSendSuccess(paymentRequest)
      }
      "receipt stored" in new TestPaymentRequest {
        val paymentRequest = RefundedPaymentFormatGen.next
        mockGetRefundPaymentRequests(paymentRequest)
        mockStored(paymentRequest)
        mockStored(notRefunded(paymentRequest))
        checkSendSuccess(paymentRequest)
      }
      "receipt stored and already sent" in new TestPaymentRequest {
        val paymentRequest = RefundedPaymentFormatGen.next
        mockGetRefundPaymentRequests(paymentRequest)
        mockGetReceipt(paymentRequest, ReceiptSentStatuses.OK)
        mockGetReceipt(notRefunded(paymentRequest), ReceiptSentStatuses.OK)
        checkSendSuccess(paymentRequest)
      }
    }
    "fail processing of refunded old format request" when {
      "one of steps fail" in new TestPaymentRequest {
        checkStepFail(
          RefundedPaymentFormatGen,
          refundedOldFormatPaymentRequestSteps
        )
      }
    }
    "process refunded new format payment request" in new TestPaymentRequest {
      forAll(FullOrPartlyRefundedPaymentFormatGen) { paymentRequest =>
        mockGetRefundPaymentRequestsWithRefunds(paymentRequest)
        checkSendSuccess(paymentRequest)
      }
    }
    "fail processing of refunded new format request" when {
      "get refund payment requests fail" in new TestPaymentRequest {
        val paymentRequest = RefundedPaymentFormatGen.next
        mockGetRefundPaymentRequestsFail(paymentRequest)
        checkSendFail(paymentRequest)
      }
    }
    "process valid transaction request" when {
      "receipt not stored" in new TestTransaction {
        val transaction = ValidAccountTransactionGen.next
        mockNotStored(transaction)
        checkSendSuccess(transaction)
      }
      "receipt stored" in new TestTransaction {
        val transaction = ValidAccountTransactionGen.next
        mockStored(transaction)
        checkSendSuccess(transaction)
      }
      "receipt stored and already sent" in new TestTransaction {
        val transaction = ValidAccountTransactionGen.next
        mockGetReceipt(transaction, ReceiptSentStatuses.OK)
        checkSendSuccess(transaction)
      }
    }
    "fail processing of valid transaction" when {
      "one of steps fail" in new TestTransaction {
        checkStepFail(
          ValidAccountTransactionGen,
          transactionSteps
        )
      }
    }
    "process refunded transaction request" when {
      "receipt not stored" in new TestTransaction {
        val transaction = RefundedAccountTransactionGen.next
        mockReceiptNotCalled(transaction)
        mockReceiptNotCalled(notRefunded(transaction))
      }
      "receipt stored" in new TestTransaction {
        val transaction = RefundedAccountTransactionGen.next
        mockReceiptNotCalled(transaction)
        mockReceiptNotCalled(notRefunded(transaction))
      }
    }
    "process refund request" when {
      "receipt not stored" in new TestRefundRequest {
        forAll(RefundRequestAndPaymentGen) { refundRequest =>
          mockNotStored(refundRequest)
          checkSendSuccess(refundRequest)
        }
      }
      "receipt stored" in new TestRefundRequest {
        forAll(RefundRequestAndPaymentGen) { refundRequest =>
          mockStored(refundRequest)
          checkSendSuccess(refundRequest)
        }
      }
      "receipt stored and already sent" in new TestRefundRequest {
        forAll(RefundRequestAndPaymentGen) { refundRequest =>
          mockGetReceipt(refundRequest, ReceiptSentStatuses.OK)
          checkSendSuccess(refundRequest)
        }
      }
    }
    "fail processing of refund request" when {
      "one of steps fail" in new TestRefundRequest {
        checkStepFail(
          RefundRequestAndPaymentGen,
          refundRequestSteps
        )
      }
    }

    "do nothing on SecurityDeposit" when {
      "payment" in new TestPaymentRequest {
        forAll(SecurityDepositPRGen) { request =>
          mockReceiptNotCalled(request)
        }
      }
      "refund" in new TestPaymentRequest {
        forAll(RefundSecurityDepositPRGen) { refund =>
          mockReceiptNotCalled(refund)
        }
      }

      "transaction" in new TestTransaction {
        forAll(SecurityDepositAccountTransactionGen) { transaction =>
          mockReceiptNotCalled(transaction)
        }
      }
    }

    trait TestBase {

      protected val ValidationException = new IllegalArgumentException("VALIDATION")

      protected val ReceiptApiMock = mock[ReceiptApi]

      protected val PaymentSystemServiceMock = mock[PaymentSystemService]

      protected val ReceiptServiceMock = mock[ReceiptService]

      protected val actor = TestActorRef(ReceiptActor.props(ReceiptApiMock, ReceiptServiceMock))

      implicit val timeout = Timeout(2.seconds)
      implicit val rc = AutomatedContext("receipt-actor-spec")

      case class Step[T](success: T => Unit, fail: T => Unit)

      private def genOfSteps[T](steps: Seq[Step[T]]): Gen[Seq[T => Unit]] = {
        for {
          i <- Gen.chooseNum(1, steps.size - 1)
          success = steps.take(i).map(_.success)
          fail = steps.drop(i).head.fail
        } yield success :+ fail
      }

      trait Checker[T] {
        def checkSuccess(u: T): Unit
        def checkFail(u: T): Unit
      }

      object Checker {

        def apply[A](implicit c: Checker[A]): Checker[A] = c

        def checkSuccess[A: Checker](a: A): Unit = Checker[A].checkSuccess(a)

        def checkFail[A: Checker](a: A): Unit = Checker[A].checkFail(a)

        trait SendChecker[T] extends Checker[T] {

          protected def send(u: T): Future[Any]

          override def checkSuccess(u: T): Unit = {
            send(u).futureValue match {
              case Done =>
                ()
              case other =>
                fail(s"Unexpected $other")
            }
          }

          override def checkFail(u: T): Unit = {
            send(u).futureValue match {
              case Fail(`ValidationException`) =>
                ()
              case other =>
                fail(s"Unexpected $other")
            }
          }

        }

      }

      protected def checkSendSuccess[T: Checker](u: T): Unit = {
        Checker.checkSuccess(u)
      }

      protected def checkSendFail[T: Checker](u: T): Unit = {
        Checker.checkFail(u)
      }

      protected def checkStepFail[T: Checker](paymentRequestGen: Gen[T], steps: Seq[Step[T]]): Unit = {
        forAll(paymentRequestGen, genOfSteps(steps)) { (paymentRequest, steps) =>
          steps.foreach { step =>
            step(paymentRequest)
          }
          Checker.checkFail(paymentRequest)
        }
      }

    }

    trait TestPaymentRequest extends TestBase {

      protected def mockReceiptMethodCall(request: PaymentRequest): Unit = {
        val receipt = getReceipt(request)
        (ReceiptApiMock
          .receipt(_: PaymentRequest))
          .expects(request)
          .returns(Future.successful(receipt)): Unit
      }

      protected def mockReceiptNotCalled(request: PaymentRequest): Unit = {
        (ReceiptApiMock
          .receipt(_: PaymentRequest))
          .stubs(request)
          .never(): Unit
      }

      protected def mockReceiptNotCalled(request: RefundPaymentRequest, payment: State.Payment): Unit = {
        (ReceiptApiMock
          .receipt(_: RefundPaymentRequest, _: State.Payment))
          .stubs(request, payment)
          .never(): Unit
      }

      protected def mockReceiptMethodCallFail(paymentRequest: PaymentRequest): Unit = {
        (ReceiptApiMock
          .receipt(_: PaymentRequest))
          .expects(paymentRequest)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockSendMethodCall(request: PaymentRequest): Unit = {
        val receipt = getReceipt(request)
        (ReceiptApiMock.send _)
          .expects(receipt)
          .returns(Future.successful(SentReceiptResponse(ReceiptSentStatuses.OK))): Unit
      }

      protected def mockSendMethodCallFail(request: PaymentRequest): Unit = {
        val receipt = getReceipt(request)
        (ReceiptApiMock.send _)
          .expects(receipt)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockCommitMethodCall(request: PaymentRequest): Unit = {
        val receipt = getReceipt(request)
        (ReceiptApiMock.commit _).expects(receipt).returns(Future.successful(())): Unit
      }

      protected def mockCommitMethodCallFail(request: PaymentRequest): Unit = {
        val receipt = getReceipt(request)
        (ReceiptApiMock.commit _).expects(receipt).returns(Future.failed(ValidationException)): Unit
      }

      private def mockGetRefundPaymentRequests(request: PaymentRequest, refunds: Seq[RefundPaymentRequest]): Unit = {
        request.state.map(_.stateStatus) match {
          case Some(StateStatuses.Refunded) =>
            (PaymentSystemServiceMock
              .getRefundPaymentRequests(_: Seq[RequestFilter])(_: RequestContext))
              .expects(Seq(RefundsFor(request.id)), *)
              .returns(Future.successful(refunds)): Unit
          case _ =>
            ()
        }
      }

      protected def mockGetRefundPaymentRequests(request: PaymentRequest): Unit = {
        mockGetRefundPaymentRequests(request, Seq.empty)
      }

      protected def mockGetRefundPaymentRequestsWithRefunds(request: PaymentRequest): Unit = {
        val refunds = Gen.nonEmptyListOf(refundRequestGen()).next
        mockGetRefundPaymentRequests(request, refunds)
      }

      protected def mockGetRefundPaymentRequestsFail(request: PaymentRequest): Unit = {
        (PaymentSystemServiceMock
          .getRefundPaymentRequests(_: Seq[RequestFilter])(_: RequestContext))
          .expects(Seq(RefundsFor(request.id)), *)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockGetReceipt(request: PaymentRequest, sentStatuses: ReceiptSentStatuses.Value): Unit = {
        val id = fingerPrintOf(request)
        val receipt = getReceipt(request).copy(sentStatus = sentStatuses)
        (ReceiptServiceMock
          .get(_: ReceiptDao.Filter)(_: RequestContext))
          .expects(ReceiptDao.ForId(id), *)
          .returns(Future.successful(Seq(receipt))): Unit
      }

      protected def mockGetReceiptMiss(request: PaymentRequest): Unit = {
        val id = fingerPrintOf(request)
        (ReceiptServiceMock
          .get(_: ReceiptDao.Filter)(_: RequestContext))
          .expects(ReceiptDao.ForId(id), *)
          .returns(Future.successful(Seq.empty)): Unit
      }

      protected def mockGetReceiptFail(paymentRequest: PaymentRequest): Unit = {
        val id = fingerPrintOf(paymentRequest)
        (ReceiptServiceMock
          .get(_: ReceiptDao.Filter)(_: RequestContext))
          .expects(ReceiptDao.ForId(id), *)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockInsertReceipt(paymentRequest: PaymentRequest): Unit = {
        val receipt = getReceipt(paymentRequest)
        (ReceiptServiceMock
          .insert(_: Receipt)(_: RequestContext))
          .expects(receipt, *)
          .returns(Future.unit): Unit
      }

      protected def mockInsertReceiptFail(paymentRequest: PaymentRequest): Unit = {
        val receipt = getReceipt(paymentRequest)
        (ReceiptServiceMock
          .insert(_: Receipt)(_: RequestContext))
          .expects(receipt, *)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockSentMethodCall(paymentRequest: PaymentRequest): Unit = {
        val receipt = getReceipt(paymentRequest)
        (ReceiptServiceMock
          .sent(_: ReceiptId, _: SentReceiptResponse)(_: RequestContext))
          .expects(receipt.id, *, *)
          .returns(Future.unit): Unit
      }

      protected def mockSentMethodCallFail(paymentRequest: PaymentRequest): Unit = {
        val receipt = getReceipt(paymentRequest)
        (ReceiptServiceMock
          .sent(_: ReceiptId, _: SentReceiptResponse)(_: RequestContext))
          .expects(receipt.id, *, *)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockNotStored(paymentRequest: PaymentRequest): Unit = {
        mockGetReceiptMiss(paymentRequest)
        mockReceiptMethodCall(paymentRequest)
        mockInsertReceipt(paymentRequest)
        mockSendMethodCall(paymentRequest)
        mockSentMethodCall(paymentRequest)
      }

      protected def mockStored(paymentRequest: PaymentRequest): Unit = {
        mockGetReceipt(paymentRequest, ReceiptSentStatuses.Ready)
        mockSendMethodCall(paymentRequest)
        mockSentMethodCall(paymentRequest)
      }

      protected val paymentRequestSteps: Seq[Step[PaymentRequest]] = Seq(
        Step(mockGetReceiptMiss(_: PaymentRequest), mockGetReceiptFail(_: PaymentRequest)),
        Step(mockReceiptMethodCall(_: PaymentRequest), mockReceiptMethodCallFail(_: PaymentRequest)),
        Step(mockInsertReceipt(_: PaymentRequest), mockInsertReceiptFail(_: PaymentRequest)),
        Step(mockSendMethodCall(_: PaymentRequest), mockSendMethodCallFail(_: PaymentRequest)),
        Step(mockSentMethodCall(_: PaymentRequest), mockSentMethodCallFail(_: PaymentRequest))
      )

      protected val refundedOldFormatPaymentRequestSteps: Seq[Step[PaymentRequest]] = {
        val init = Step(
          mockGetRefundPaymentRequests(_: PaymentRequest),
          mockGetRefundPaymentRequestsFail(_: PaymentRequest)
        )

        val notRefundedSteps = paymentRequestSteps.map { case Step(s, f) =>
          Step(
            { r: PaymentRequest =>
              s(notRefunded(r))
            },
            { r: PaymentRequest =>
              f(notRefunded(r))
            }
          )
        }
        (init +: notRefundedSteps) ++ paymentRequestSteps
      }

      implicit val paymentRequestChecker: Checker[PaymentRequest] = new Checker.SendChecker[PaymentRequest] {

        override protected def send(u: PaymentRequest): Future[Any] = {
          val request = ReceiptActor.Request.ProcessPaymentRequest(
            u,
            PaymentSystemServiceMock,
            expectResponse = true
          )
          actor ? request
        }

      }

    }

    trait TestTransaction extends TestBase {

      protected def mockReceiptMethodCall(transaction: AccountTransaction): Unit = {
        val receipt = getReceipt(transaction)
        (ReceiptApiMock
          .receipt(_: AccountTransaction))
          .expects(transaction)
          .returns(Future.successful(receipt)): Unit
      }

      protected def mockReceiptNotCalled(transaction: AccountTransaction): Unit = {
        (ReceiptApiMock
          .receipt(_: AccountTransaction))
          .stubs(transaction)
          .never(): Unit
      }

      protected def mockReceiptMethodCallFail(transaction: AccountTransaction): Unit = {
        (ReceiptApiMock
          .receipt(_: AccountTransaction))
          .expects(transaction)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockSendMethodCall(transaction: AccountTransaction): Unit = {
        val receipt = getReceipt(transaction)
        (ReceiptApiMock.send _)
          .expects(receipt)
          .returns(Future.successful(SentReceiptResponse(ReceiptSentStatuses.OK))): Unit
      }

      protected def mockSendMethodCallFail(transaction: AccountTransaction): Unit = {
        val receipt = getReceipt(transaction)
        (ReceiptApiMock.send _)
          .expects(receipt)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockGetReceipt(transaction: AccountTransaction, sentStatuses: ReceiptSentStatuses.Value): Unit = {
        val id = fingerPrintOf(transaction)
        val receipt = getReceipt(transaction).copy(sentStatus = sentStatuses)
        (ReceiptServiceMock
          .get(_: ReceiptDao.Filter)(_: RequestContext))
          .expects(ReceiptDao.ForId(id), *)
          .returns(Future.successful(Seq(receipt))): Unit
      }

      protected def mockGetReceiptMiss(transaction: AccountTransaction): Unit = {
        val id = fingerPrintOf(transaction)
        (ReceiptServiceMock
          .get(_: ReceiptDao.Filter)(_: RequestContext))
          .expects(ReceiptDao.ForId(id), *)
          .returns(Future.successful(Seq.empty)): Unit
      }

      protected def mockGetReceiptFail(transaction: AccountTransaction): Unit = {
        val id = fingerPrintOf(transaction)
        (ReceiptServiceMock
          .get(_: ReceiptDao.Filter)(_: RequestContext))
          .expects(ReceiptDao.ForId(id), *)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockInsertReceipt(transaction: AccountTransaction): Unit = {
        val receipt = getReceipt(transaction)
        (ReceiptServiceMock
          .insert(_: Receipt)(_: RequestContext))
          .expects(receipt, *)
          .returns(Future.unit): Unit
      }

      protected def mockInsertReceiptFail(transaction: AccountTransaction): Unit = {
        val receipt = getReceipt(transaction)
        (ReceiptServiceMock
          .insert(_: Receipt)(_: RequestContext))
          .expects(receipt, *)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockSentMethodCall(transaction: AccountTransaction): Unit = {
        val receipt = getReceipt(transaction)
        (ReceiptServiceMock
          .sent(_: ReceiptId, _: SentReceiptResponse)(_: RequestContext))
          .expects(receipt.id, *, *)
          .returns(Future.unit): Unit
      }

      protected def mockSentMethodCallFail(transaction: AccountTransaction): Unit = {
        val receipt = getReceipt(transaction)
        (ReceiptServiceMock
          .sent(_: ReceiptId, _: SentReceiptResponse)(_: RequestContext))
          .expects(receipt.id, *, *)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockNotStored(transaction: AccountTransaction): Unit = {
        mockGetReceiptMiss(transaction)
        mockReceiptMethodCall(transaction)
        mockInsertReceipt(transaction)
        mockSendMethodCall(transaction)
        mockSentMethodCall(transaction)
      }

      protected def mockStored(transaction: AccountTransaction): Unit = {
        mockGetReceipt(transaction, ReceiptSentStatuses.Ready)
        mockSendMethodCall(transaction)
        mockSentMethodCall(transaction)
      }

      protected val transactionSteps: Seq[Step[AccountTransaction]] = Seq(
        Step(mockGetReceiptMiss(_: AccountTransaction), mockGetReceiptFail(_: AccountTransaction)),
        Step(mockReceiptMethodCall(_: AccountTransaction), mockReceiptMethodCallFail(_: AccountTransaction)),
        Step(mockInsertReceipt(_: AccountTransaction), mockInsertReceiptFail(_: AccountTransaction)),
        Step(mockSendMethodCall(_: AccountTransaction), mockSendMethodCallFail(_: AccountTransaction)),
        Step(mockSentMethodCall(_: AccountTransaction), mockSentMethodCallFail(_: AccountTransaction))
      )

      protected val refundedTransactionSteps: Seq[Step[AccountTransaction]] = {
        val notRefundedSteps = transactionSteps.map { case Step(s, f) =>
          Step(
            { r: AccountTransaction =>
              s(notRefunded(r))
            },
            { r: AccountTransaction =>
              f(notRefunded(r))
            }
          )
        }

        notRefundedSteps ++ transactionSteps
      }

      implicit val transactionChecker: Checker[AccountTransaction] = new Checker.SendChecker[AccountTransaction] {

        override protected def send(u: AccountTransaction): Future[Any] = {
          actor ? ReceiptActor.Request.ProcessTransaction(u, expectResponse = true)
        }

      }

    }

    trait TestRefundRequest extends TestBase {

      protected def mockReceiptMethodCall(refundAndPayment: RefundRequestAndPayment): Unit = {
        val receipt = getReceipt(refundAndPayment.refundRequest)
        (ReceiptApiMock
          .receipt(_: RefundPaymentRequest, _: State.Payment))
          .expects(refundAndPayment.refundRequest, refundAndPayment.payment)
          .returns(Future.successful(receipt)): Unit
      }

      protected def mockReceiptMethodCallFail(refundAndPayment: RefundRequestAndPayment): Unit = {
        (ReceiptApiMock
          .receipt(_: RefundPaymentRequest, _: State.Payment))
          .expects(refundAndPayment.refundRequest, refundAndPayment.payment)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockSendMethodCall(request: RefundRequestAndPayment): Unit = {
        val receipt = getReceipt(request.refundRequest)
        (ReceiptApiMock.send _)
          .expects(receipt)
          .returns(Future.successful(SentReceiptResponse(ReceiptSentStatuses.OK))): Unit
      }

      protected def mockSendMethodCallFail(request: RefundRequestAndPayment): Unit = {
        val receipt = getReceipt(request.refundRequest)
        (ReceiptApiMock.send _)
          .expects(receipt)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockGetReceipt(
          refundAndPayment: RefundRequestAndPayment,
          sentStatuses: ReceiptSentStatuses.Value): Unit = {
        val id = fingerPrintOf(refundAndPayment.refundRequest)
        val receipt = getReceipt(refundAndPayment.refundRequest).copy(sentStatus = sentStatuses)
        (ReceiptServiceMock
          .get(_: ReceiptDao.Filter)(_: RequestContext))
          .expects(ReceiptDao.ForId(id), *)
          .returns(Future.successful(Seq(receipt))): Unit
      }

      protected def mockGetReceiptMiss(request: RefundRequestAndPayment): Unit = {
        val id = fingerPrintOf(request.refundRequest)
        (ReceiptServiceMock
          .get(_: ReceiptDao.Filter)(_: RequestContext))
          .expects(ReceiptDao.ForId(id), *)
          .returns(Future.successful(Seq.empty)): Unit
      }

      protected def mockGetReceiptFail(request: RefundRequestAndPayment): Unit = {
        val id = fingerPrintOf(request.refundRequest)
        (ReceiptServiceMock
          .get(_: ReceiptDao.Filter)(_: RequestContext))
          .expects(ReceiptDao.ForId(id), *)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockInsertReceipt(request: RefundRequestAndPayment): Unit = {
        val receipt = getReceipt(request.refundRequest)
        (ReceiptServiceMock
          .insert(_: Receipt)(_: RequestContext))
          .expects(receipt, *)
          .returns(Future.unit): Unit
      }

      protected def mockInsertReceiptFail(request: RefundRequestAndPayment): Unit = {
        val receipt = getReceipt(request.refundRequest)
        (ReceiptServiceMock
          .insert(_: Receipt)(_: RequestContext))
          .expects(receipt, *)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockSentMethodCall(request: RefundRequestAndPayment): Unit = {
        val receipt = getReceipt(request.refundRequest)
        (ReceiptServiceMock
          .sent(_: ReceiptId, _: SentReceiptResponse)(_: RequestContext))
          .expects(receipt.id, *, *)
          .returns(Future.unit): Unit
      }

      protected def mockSentMethodCallFail(request: RefundRequestAndPayment): Unit = {
        val receipt = getReceipt(request.refundRequest)
        (ReceiptServiceMock
          .sent(_: ReceiptId, _: SentReceiptResponse)(_: RequestContext))
          .expects(receipt.id, *, *)
          .returns(Future.failed(ValidationException)): Unit
      }

      protected def mockNotStored(request: RefundRequestAndPayment): Unit = {
        mockGetReceiptMiss(request)
        mockReceiptMethodCall(request)
        mockInsertReceipt(request)
        mockSendMethodCall(request)
        mockSentMethodCall(request)
      }

      protected def mockStored(request: RefundRequestAndPayment): Unit = {
        mockGetReceipt(request, ReceiptSentStatuses.Ready)
        mockSendMethodCall(request)
        mockSentMethodCall(request)
      }

      protected val refundRequestSteps: Seq[Step[RefundRequestAndPayment]] = Seq(
        Step(mockGetReceiptMiss(_: RefundRequestAndPayment), mockGetReceiptFail(_: RefundRequestAndPayment)),
        Step(
          mockReceiptMethodCall(_: RefundRequestAndPayment),
          mockReceiptMethodCallFail(_: RefundRequestAndPayment)
        ),
        Step(mockInsertReceipt(_: RefundRequestAndPayment), mockInsertReceiptFail(_: RefundRequestAndPayment)),
        Step(mockSendMethodCall(_: RefundRequestAndPayment), mockSendMethodCallFail(_: RefundRequestAndPayment)),
        Step(mockSentMethodCall(_: RefundRequestAndPayment), mockSentMethodCallFail(_: RefundRequestAndPayment))
      )

      implicit val refundRequestAndPaymentChecker: Checker[RefundRequestAndPayment] =
        new Checker.SendChecker[RefundRequestAndPayment] {

          override protected def send(u: RefundRequestAndPayment): Future[Any] = {
            actor ? ReceiptActor.Request.ProcessRefundPaymentRequest(
              u.refundRequest,
              u.payment,
              expectResponse = true
            )
          }

        }

    }
  }
}

object ReceiptActorSpec {
  private val TargetsToReceipt = (Targets.values - Targets.SecurityDeposit).toSeq

  case class RefundRequestAndPayment(refundRequest: RefundPaymentRequest, payment: State.Payment)

  private def paymentRequestWithStatusesGen(
      stateStatuses: Set[StateStatuses.Value],
      targets: Seq[Target]): Gen[PaymentRequest] =
    for {
      target <- Gen.oneOf(targets)
      request <- paymentRequestGen(
        PaymentRequestParams(
          state = Some(
            StateParams(
              stateStatus = stateStatuses
            )
          ),
          source = PaymentRequestSourceParams(context = Some(Context(target)))
        )
      )
    } yield request

  private val ValidPaymentGen: Gen[PaymentRequest] = {
    paymentRequestWithStatusesGen(Set(StateStatuses.Valid), TargetsToReceipt)
  }

  private val SecurityDepositPRGen: Gen[PaymentRequest] = {
    paymentRequestWithStatusesGen(Set(StateStatuses.Valid), Seq(Targets.SecurityDeposit))
  }

  private val RefundSecurityDepositPRGen: Gen[PaymentRequest] = {
    paymentRequestWithStatusesGen(
      Set(StateStatuses.Refunded, StateStatuses.PartlyRefunded),
      Seq(Targets.SecurityDeposit)
    )
  }

  private val RefundedPaymentFormatGen: Gen[PaymentRequest] = {
    paymentRequestWithStatusesGen(Set(StateStatuses.Refunded), TargetsToReceipt)
  }

  private val FullOrPartlyRefundedPaymentFormatGen: Gen[PaymentRequest] = {
    paymentRequestWithStatusesGen(
      Set(StateStatuses.Refunded, StateStatuses.PartlyRefunded),
      TargetsToReceipt
    )
  }

  private val ValidAccountTransactionGen: Gen[AccountTransaction] =
    for {
      t <- accountTransactionGen(AccountTransactions.Withdraw)
      id <- hashAccountTransactionIdGen(AccountTransactions.Withdraw)
      target <- Gen.oneOf(TargetsToReceipt)
      receipt <- t.receiptData match {
        case Some(data) =>
          Gen.const(data)
        case None =>
          requestReceiptGen(t.withdraw)
      }
    } yield t.copy(
      id = id,
      receiptData = Some(receipt),
      activity = Activities.Active,
      target = Some(target)
    )

  private val SecurityDepositAccountTransactionGen: Gen[AccountTransaction] =
    ValidAccountTransactionGen.map { tr =>
      tr.copy(target = Some(Targets.SecurityDeposit), receiptData = None)
    }

  private val RefundedAccountTransactionGen: Gen[AccountTransaction] = {
    ValidAccountTransactionGen.map { tr =>
      tr.copy(activity = Activities.Inactive)
    }
  }

  private val RefundRequestGen: Gen[RefundPaymentRequest] = for {
    target <- Gen.oneOf(TargetsToReceipt)
    stateParams = StateParams(
      stateStatus = Set(StateStatuses.Valid)
    )
    request <- refundRequestGen(
      RefundRequestParams(
        state = Some(stateParams),
        source = RefundRequestSourceParams(context = Some(Context(target)))
      )
    )
  } yield request

  private val RefundRequestAndPaymentGen: Gen[RefundRequestAndPayment] = for {
    request <- RefundRequestGen
    payment <- paymentForRefundGen(request)
  } yield RefundRequestAndPayment(request, payment)

  private def fingerPrintOf(p: PaymentRequest): String =
    Receipt.fingerPrintOf(
      p.id,
      p.method.ps,
      p.source.account,
      ReceiptApiImpl.statusOf(p.state.get.stateStatus).get
    )

  private def fingerPrintOf(tr: AccountTransaction): String =
    Receipt.fingerPrintOf(
      tr.id,
      tr.account,
      ReceiptApiImpl.statusOf(tr.activity)
    )

  private def fingerPrintOf(r: RefundPaymentRequest): String =
    Receipt.fingerPrintOf(
      r.id,
      r.method.ps,
      r.account,
      ReceiptApiImpl.statusOf(r.state.get.stateStatus).get
    )

  private val paymentRequestReceiptMap = mutable.Map.empty[PaymentRequest, Receipt]

  private def getReceipt(paymentRequest: PaymentRequest): Receipt = {
    paymentRequestReceiptMap.getOrElseUpdate(paymentRequest, receiptOf(paymentRequest))
  }

  private val transactionsReceiptMap = mutable.Map.empty[AccountTransaction, Receipt]

  private def getReceipt(transaction: AccountTransaction): Receipt = {
    transactionsReceiptMap.getOrElseUpdate(transaction, receiptOf(transaction))
  }

  private val refundRequestReceiptMap = mutable.Map.empty[RefundPaymentRequest, Receipt]

  private def getReceipt(refundPaymentRequest: RefundPaymentRequest): Receipt = {
    refundRequestReceiptMap.getOrElseUpdate(refundPaymentRequest, receiptOf(refundPaymentRequest))
  }

  private def receiptOf(paymentRequest: PaymentRequest): Receipt = {
    Receipt(
      paymentRequest.id,
      paymentRequest.method.ps,
      paymentRequest.source.account,
      new String("test").getBytes,
      Some("test@mail"),
      None,
      ReceiptApiImpl.statusOf(paymentRequest.state.get.stateStatus).get,
      sent = None,
      sentStatus = ReceiptSentStatuses.Ready,
      failDescription = None
    )
  }

  private def receiptOf(transaction: AccountTransaction): Receipt = {
    Receipt(
      transaction.id,
      transaction.account,
      new String("test").getBytes,
      Some("test@mail"),
      None,
      ReceiptApiImpl.statusOf(transaction.activity),
      sent = None,
      sentStatus = ReceiptSentStatuses.Ready,
      failDescription = None
    )
  }

  private def receiptOf(refundPaymentRequest: RefundPaymentRequest): Receipt = {
    Receipt(
      refundPaymentRequest.id,
      refundPaymentRequest.method.ps,
      refundPaymentRequest.account,
      new String("test").getBytes,
      Some("test@mail"),
      None,
      ReceiptApiImpl.statusOf(refundPaymentRequest.state.get.stateStatus).get,
      sent = None,
      sentStatus = ReceiptSentStatuses.Ready,
      failDescription = None
    )
  }

  private def notRefunded(r: PaymentRequest): PaymentRequest = {
    val state = r.state match {
      case Some(p: Incoming) if p.stateStatus == StateStatuses.Refunded =>
        p.copy(stateStatus = StateStatuses.Valid)
      case p =>
        throw new IllegalArgumentException(s"Unexpected payment $p")
    }
    r.copy(state = Some(state))
  }

  private def notRefunded(tr: AccountTransaction): AccountTransaction =
    tr.copy(activity = Activities.Active)

}
