package ru.yandex.vertis.billing.banker.tasks

import java.util.regex.Pattern
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory
import org.mockito.Mockito.{times, verify}
import org.scalacheck.Gen
import ru.yandex.vertis.billing.banker.actor.{EffectActorProtocol, ReceiptActor}
import ru.yandex.vertis.billing.banker.config.{InvariantChecker, ReceiptDeliverySettings}
import ru.yandex.vertis.billing.banker.model.PaymentRequest.ReceiptData
import ru.yandex.vertis.billing.banker.model.gens.{
  accountTransactionGen,
  paymentForRefundGen,
  paymentRequestGen,
  refundRequestGen,
  requestReceiptGen,
  NonEmptyPaymentPayloadGen,
  PaymentRequestParams,
  Producer,
  RefundRequestParams,
  RefundRequestSourceParams
}
import ru.yandex.vertis.billing.banker.model.{
  Account,
  AccountTransaction,
  AccountTransactions,
  Epoch,
  PaymentMethod,
  PaymentRequest,
  PaymentSystemIds,
  RefundPaymentRequest
}
import ru.yandex.vertis.billing.banker.service.{AccountTransactionService, EpochService, PaymentSystemService}
import ru.yandex.vertis.billing.banker.tasks.ReceiptTask.Setup
import ru.yandex.vertis.billing.banker.tasks.ReceiptTaskSpec.{epochServiceMock, MessagePattern, ValidationException}
import ru.yandex.vertis.billing.banker.util.DateTimeUtils
import pureconfig.generic.auto._
import ru.yandex.vertis.billing.banker.model.State.Incoming

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import ReceiptTaskSpec.{
  requestsAndTransactions,
  NoEmailAndPhone,
  OnlyEmail,
  OnlyPhone,
  Params,
  TestContext,
  WithEmailAndPhone
}
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import pureconfig.ConfigSource
import ru.yandex.vertis.banker.util.LogFetchingProvider
import ru.yandex.vertis.billing.banker.actor.ReceiptActorSpec.RefundRequestAndPayment

import scala.util.Failure

/**
  * @author ruslansd
  */
class ReceiptTaskSpec extends EffectAsyncTaskSpecBase("ReceiptTaskSpec") with LogFetchingProvider {

  val method = PaymentMethod(PaymentSystemIds.YandexKassa, "AC")

  def prepareRequestsAndTransaction(
      paymentReqCount: Option[Int],
      trCount: Option[Int],
      refundPaymentReqCount: Option[Int],
      params: Params): TestContext = {
    val source = requestsAndTransactions(
      paymentReqCount.getOrElse(0),
      trCount.getOrElse(0),
      refundPaymentReqCount.getOrElse(0),
      params
    )

    val service = mock[PaymentSystemService]
    paymentReqCount match {
      case Some(_) =>
        when(service.getPaymentRequestsFor(?)(?)).thenReturn(Future.successful(source.paymentRequests))
      case None =>
        when(service.getPaymentRequestsFor(?)(?)).thenReturn(Future.failed(ValidationException))
    }
    refundPaymentReqCount match {
      case Some(_) =>
        when(service.getRefundPaymentRequestsFor(?)(?))
          .thenReturn(Future.successful(source.refundPaymentRequests.map(_.refundRequest)))
        when(service.getPayments(?)(?)).thenReturn(Future.successful(source.refundPaymentRequests.map(_.payment)))
      case None =>
        when(service.getRefundPaymentRequestsFor(?)(?)).thenReturn(Future.failed(ValidationException))
        when(service.getPayments(?)(?)).thenReturn(Future.failed(ValidationException))
    }
    val setup = Iterable(Setup(PaymentSystemIds.YandexKassa, service))

    val accountTransactionService = mock[AccountTransactionService]
    trCount match {
      case Some(_) =>
        when(accountTransactionService.get(?)(?)).thenReturn(Future.successful(source.transactions))
      case None =>
        when(accountTransactionService.get(?)(?)).thenReturn(Future.failed(ValidationException))
    }

    val epochService = epochServiceMock()

    val checker = new InvariantChecker(
      ConfigSource.fromConfig(ConfigFactory.empty()).loadOrThrow[ReceiptDeliverySettings]
    )

    val probe = TestProbe()

    val task = new ReceiptTask(setup, accountTransactionService, epochService, checker, probe.ref)

    TestContext(
      task,
      epochService,
      probe,
      service,
      source.paymentRequests,
      source.transactions,
      source.refundPaymentRequests
    )
  }

  def isRequestValuable(request: PaymentRequest): Boolean =
    request.source.optReceiptData.exists(_.isReceiptEmailOrPhoneDefined)

  def isTransactionValuable(tr: AccountTransaction): Boolean =
    tr.withdraw != 0 && tr.receiptData.exists(_.isReceiptEmailOrPhoneDefined)

  def isRequestValuable(request: RefundRequestAndPayment): Boolean =
    request.refundRequest.source.optReceiptData.exists(_.isReceiptEmailOrPhoneDefined)

  def checkContext(context: TestContext)(finishTaskExecution: Future[Unit] => Unit): Unit = {
    val execution = context.task.execute()
    val valuablePaymentRequests = context.paymentRequests.filter(isRequestValuable)
    val valuableTransactions = context.transactions.filter(isTransactionValuable)
    val valuableRefundPaymentRequests = context.refundPaymentRequests.filter(isRequestValuable)
    val expectedCounter = {
      valuablePaymentRequests.size + valuableTransactions.size + valuableRefundPaymentRequests.size
    }
    val actualPaymentRequests = ArrayBuffer.empty[PaymentRequest]
    val actualTransactions = ArrayBuffer.empty[AccountTransaction]
    val actualRefundPaymentRequests = ArrayBuffer.empty[RefundPaymentRequest]
    val service = context.paymentSystemService
    (1 to expectedCounter).foreach { _ =>
      context.probe.expectMsgPF() {
        case ReceiptActor.Request.ProcessPaymentRequest(request, `service`, true) =>
          context.probe.reply(EffectActorProtocol.Done)
          actualPaymentRequests += request
        case ReceiptActor.Request.ProcessTransaction(tr, true) =>
          context.probe.reply(EffectActorProtocol.Done)
          actualTransactions += tr
        case ReceiptActor.Request.ProcessRefundPaymentRequest(request, _, true) =>
          context.probe.reply(EffectActorProtocol.Done)
          actualRefundPaymentRequests += request
      }
    }
    finishTaskExecution(execution)
    actualPaymentRequests should contain theSameElementsAs valuablePaymentRequests
    actualTransactions should contain theSameElementsAs valuableTransactions
    actualRefundPaymentRequests should contain theSameElementsAs valuableRefundPaymentRequests.map(_.refundRequest)
    val actualCounter = {
      actualPaymentRequests.size + actualTransactions.size + actualRefundPaymentRequests.size
    }
    actualCounter shouldBe expectedCounter

    val expectedEpochSets = Seq(
      valuablePaymentRequests,
      valuableTransactions,
      valuableRefundPaymentRequests
    ).count(_.nonEmpty)

    verify(context.epochService, times(expectedEpochSets)).set(?, ?): Unit
  }

  def checkTaskSucceed(context: TestContext): Unit = {
    val events = withLogFetching[ReceiptTask, Unit] {
      checkContext(context) { action =>
        action.futureValue
      }
    }
    events.logEvents.isEmpty shouldBe true: Unit
  }

  def checkLoggingEvent(event: ILoggingEvent): Unit = {
    event.getLevel shouldBe Level.ERROR

    val wrapper = event.getThrowableProxy
    wrapper.getClassName shouldBe classOf[IllegalArgumentException].getName
    wrapper.getMessage shouldBe ValidationException.getMessage: Unit
  }

  def checkTaskFailed(context: TestContext): Unit = {
    val events = withLogFetching[ReceiptTask, Unit] {
      checkContext(context) { action =>
        action.toTry match {
          case Failure(`ValidationException`) => ()
          case other =>
            fail(s"Unexpected result $other")
        }
      }
    }

    events.logEvents.size shouldBe 1
    checkLoggingEvent(events.logEvents.head)
  }

  def testRequestsAndTransaction(count: Int, params: Params): Unit = {
    checkTaskSucceed(prepareRequestsAndTransaction(Some(count), Some(count), Some(count), params))
  }

  def testPaymentRequestsGetFail(count: Int, params: Params): Unit = {
    checkTaskFailed(prepareRequestsAndTransaction(None, Some(count), Some(count), params))
  }

  def testTransactionsGetFail(count: Int, params: Params): Unit = {
    checkTaskFailed(prepareRequestsAndTransaction(Some(count), None, Some(count), params))
  }

  def testRefundRequestsGetFail(count: Int, params: Params): Unit = {
    checkTaskFailed(prepareRequestsAndTransaction(Some(count), Some(count), None, params))
  }

  "ReceiptTask" should {
    "process without fail" when {
      "work on empty set" in {
        testRequestsAndTransaction(0, Params(NoEmailAndPhone))
      }
      "work correctly when no email and phone" in {
        testRequestsAndTransaction(10, Params(NoEmailAndPhone))
      }
      "work correctly with only email" in {
        testRequestsAndTransaction(10, Params(OnlyEmail))
      }
      "work correctly with only phone" in {
        testRequestsAndTransaction(10, Params(OnlyPhone))
      }
      "work correctly with email and phone" in {
        testRequestsAndTransaction(10, Params(WithEmailAndPhone))
      }
      "do not receipt transactions with zero amount" in {
        testRequestsAndTransaction(10, Params(WithEmailAndPhone, zeroTransactions = true))
      }
    }
    "process with fail" when {
      "payment requests get fail (fail of one type not affect other)" in {
        testPaymentRequestsGetFail(15, Params(WithEmailAndPhone))
      }
      "transaction get fail (fail of one type not affect other)" in {
        testTransactionsGetFail(15, Params(WithEmailAndPhone))
      }
      "refund requests get fail (fail of one type not affect other)" in {
        testRefundRequestsGetFail(15, Params(WithEmailAndPhone))
      }
    }
  }

}

object ReceiptTaskSpec {
  import ru.yandex.vertis.mockito.MockitoSupport.{mock, stub}

  private def epochServiceMock(): EpochService = {
    val m = mock[EpochService]

    val map = mutable.Map.empty[String, Epoch]

    stub(m.get _) { case marker =>
      Future.successful(map.getOrElse(marker, 0))
    }
    stub(m.set(_: String, _: Epoch)) { case (marker, epoch) =>
      Future.successful(map += (marker -> epoch))
    }
    m
  }

  sealed trait SourcePart
  case object NoEmailAndPhone extends SourcePart
  case object OnlyEmail extends SourcePart
  case object OnlyPhone extends SourcePart
  case object WithEmailAndPhone extends SourcePart

  case class Params(part: SourcePart, zeroTransactions: Boolean = false)

  case class TestContext(
      task: ReceiptTask,
      epochService: EpochService,
      probe: TestProbe,
      paymentSystemService: PaymentSystemService,
      paymentRequests: Seq[PaymentRequest],
      transactions: Seq[AccountTransaction],
      refundPaymentRequests: Seq[RefundRequestAndPayment])

  private val acc = Account("1", "1")

  private def fixReceipt(receipt: ReceiptData, part: SourcePart): ReceiptData = {
    part match {
      case NoEmailAndPhone =>
        receipt.copy(email = None, phone = None)
      case OnlyEmail =>
        receipt.copy(email = Some("test@test.test"), phone = None)
      case OnlyPhone =>
        receipt.copy(email = None, phone = Some("88005553535"))
      case WithEmailAndPhone =>
        receipt.copy(email = Some("test@test.test"), phone = Some("88005553535"))
    }
  }

  private def paymentRequests(count: Int, params: Params): Seq[PaymentRequest] = {
    paymentRequestGen(
      PaymentRequestParams(
        account = Some(acc.id),
        payload = Some(NonEmptyPaymentPayloadGen.next)
      ).withState.withReceipt
    ).next(count)
      .map { req =>
        val s = req.source
        val updatedReceipt = s.optReceiptData.map { receipt =>
          fixReceipt(receipt, params.part)
        }
        val updatedState = req.state.map {
          case i: Incoming =>
            i.copy(epoch = Some(DateTimeUtils.now().getMillis))
          case other =>
            throw new IllegalArgumentException(s"Unexpeceted $other")
        }
        req.copy(
          source = s.copy(optReceiptData = updatedReceipt),
          state = updatedState
        )
      }
      .toSeq
  }

  private def transactions(count: Int, params: Params): Seq[AccountTransaction] = {
    val (zeroCount, nonZeroCount) = if (params.zeroTransactions) (count / 2, count / 2) else (0, count)
    val transactionGen = accountTransactionGen(AccountTransactions.Withdraw).map { tr =>
      tr.copy(epoch = Some(DateTimeUtils.now().getMillis))
    }
    val rawTransactions = transactionGen.next(count)
    val zeroTransactions = rawTransactions.take(zeroCount).map { tr =>
      tr.copy(withdraw = 0L)
    }
    val nonZeroTransactions = rawTransactions.slice(zeroCount, zeroCount + nonZeroCount).map { tr =>
      tr.copy(withdraw = Gen.posNum[Long].next)
    }
    val transactions = zeroTransactions ++ nonZeroTransactions
    transactions.map { tr =>
      val receipt = tr.receiptData.getOrElse {
        requestReceiptGen(tr.withdraw).next
      }
      val updatedReceipt = fixReceipt(receipt, params.part)
      tr.copy(receiptData = Some(updatedReceipt))
    }.toSeq
  }

  private def refundPaymentRequests(count: Int, params: Params): Seq[RefundRequestAndPayment] = {
    val sourceParams = RefundRequestSourceParams(
      withReceipt = Some(true)
    )

    refundRequestGen(
      RefundRequestParams(
        account = Some(acc.id),
        source = sourceParams
      ).withState
    ).next(count)
      .map { req =>
        val s = req.source
        val updatedReceipt = s.optReceiptData.map(r => fixReceipt(r, params.part))
        val updatedState = req.state.map { r =>
          r.copy(epoch = Some(DateTimeUtils.now().getMillis))
        }
        val refundRequest = req.copy(
          source = s.copy(optReceiptData = updatedReceipt),
          state = updatedState
        )
        val payment = paymentForRefundGen(req).next
        RefundRequestAndPayment(refundRequest, payment)
      }
      .toSeq
  }

  case class TestSource(
      paymentRequests: Seq[PaymentRequest],
      transactions: Seq[AccountTransaction],
      refundPaymentRequests: Seq[RefundRequestAndPayment])

  def requestsAndTransactions(
      paymentReqCount: Int,
      trCount: Int,
      paymentRefundReqCount: Int,
      params: Params): TestSource = {
    TestSource(
      paymentRequests(paymentReqCount, params),
      transactions(trCount, params),
      refundPaymentRequests(paymentRefundReqCount, params)
    )
  }

  private val ValidationException = new IllegalArgumentException("Error")

  private val MessagePattern = Pattern.compile(
    "Processing of .* failed with"
  )

}
