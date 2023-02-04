package ru.yandex.vertis.billing.banker.tasks

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.google.protobuf.util.Timestamps
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.model.CommonModel.OpaquePayload
import ru.yandex.vertis.banker.model.LogModel.BankerOperationEvent
import ru.yandex.vertis.banker.model.{ApiModel, LogModel}
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao.TransactionsFilter
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.RequestFilter.RefundsFor
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.{RequestFilter, StateFilter}
import ru.yandex.vertis.billing.banker.dao.TrustExternalPurchaseDao
import ru.yandex.vertis.billing.banker.model.Raw.RawPaymentFields
import ru.yandex.vertis.billing.banker.model.State.{Incoming, StateStatuses}
import ru.yandex.vertis.billing.banker.model._
import ru.yandex.vertis.billing.banker.model.gens.Producer
import ru.yandex.vertis.billing.banker.proto.BankerProtoFormats
import ru.yandex.vertis.billing.banker.service.log.{BatchAsyncProtoLoggerMock, BrokerClientMock, BrokerLogger}
import ru.yandex.vertis.billing.banker.service.{
  AccountService,
  AccountTransactionService,
  EpochService,
  PaymentSystemService
}
import ru.yandex.vertis.billing.banker.tasks.TransactionsLogTaskSpec.{
  asBankerOperationEvent,
  inValidState,
  isLoggable,
  isProcessable,
  preparePaymentRequests,
  prepareTransactions,
  InitTime
}
import ru.yandex.vertis.billing.banker.tasks.utils.PaymentRequestsGens.{
  NonProcessedPaymentRequestGen,
  PaymentRequestWithRefundRequests,
  ProcessedPaymentRequestGen,
  ProcessedPaymentRequestsWithRefundRequestGen
}
import ru.yandex.vertis.billing.banker.tasks.utils.TransactionGens.{
  HashProcessedTransactionGen,
  HashUnprocessedTransactionGen,
  PaymentSystemHashUnprocessedTransactionGen,
  PaymentSystemProcessedTransactionGen
}
import ru.yandex.vertis.billing.banker.util.CollectionUtils.RichTraversableLike
import ru.yandex.vertis.billing.banker.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.billing.banker.{Domain, Domains}
import ru.yandex.vertis.broker.client.marshallers.GoogleProtoMarshaller.googleProtoMarshaller
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.protobuf.asProto
import ru.yandex.vertis.util.time.DateTimeUtil
import spray.json._

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class TransactionsLogTaskSpec
  extends TestKit(ActorSystem("TransactionsLogTaskSpec"))
  with MockFactory
  with Matchers
  with AnyWordSpecLike
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority
  with AsyncSpecBase {

  implicit private val rc: RequestContext = AutomatedContext("TransactionsTskvLogTaskSpec")

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 50)

  private val AccountId = "master"

  private val User = "master"

  private val accountTransactions = mock[AccountTransactionService]

  private val trustPurchaseDao = mock[TrustExternalPurchaseDao]

  private def mockTransactionsGet(transactions: Iterable[AccountTransaction]): Unit =
    (accountTransactions
      .get(_: Seq[TransactionsFilter])(_: RequestContext))
      .expects(*, *)
      .returns(Future.successful(transactions)): Unit

  private val accountService = mock[AccountService]

  private def mockAccountGet(
      transactions: Iterable[AccountTransaction],
      paymentRequests: Iterable[PaymentRequest]): Unit = {
    val willCallForTrs = transactions.toSeq.sortBy(_.epoch).takeWhile(isProcessable).find(isLoggable)
    val willCallForPaymentReqs = paymentRequests.toSeq.sortBy(_.state.get.epoch).find(isProcessable)
    val expectedTimes = Seq(willCallForTrs, willCallForPaymentReqs).flatten.size
    (accountService
      .get(_: Iterable[AccountId])(_: RequestContext))
      .expects(*, *)
      .onCall { (ids, _) =>
        Future(ids.map(_ => Account(AccountId, User)))
      }
      .repeat(expectedTimes): Unit
  }

  private val pss = mock[PaymentSystemService]

  private def mockGetPsId(): Unit =
    (() => pss.psId).expects().returns(PaymentSystemIds.YandexKassaV3).anyNumberOfTimes(): Unit

  private val psServices = Seq(pss)

  private def mockPaymentRequestsGet(paymentRequests: Iterable[PaymentRequest]): Unit =
    (pss
      .getPaymentRequestsFor(_: StateFilter)(_: RequestContext))
      .expects(*, *)
      .returns(Future.successful(paymentRequests)): Unit

  private def mockRefundPaymentRequestsGet(
      paymentRequestsWithRefund: Iterable[PaymentRequestWithRefundRequests]): Unit = {
    val refundRequestsMap = paymentRequestsWithRefund
      .filter { s =>
        isProcessable(s.paymentRequest) && s.refundRequests.nonEmpty
      }
      .map { r =>
        r.paymentRequest.id -> r.refundRequests
      }
      .toMap

    if (refundRequestsMap.nonEmpty) {
      (pss
        .getRefundPaymentRequests(_: Seq[RequestFilter])(_: RequestContext))
        .expects(*, *)
        .onCall { (filters, _) =>
          filters.exactlyOne match {
            case RefundsFor(paymentRequestId) =>
              Future(refundRequestsMap(paymentRequestId))
            case _ =>
              fail("Unexpected call")
          }
        }
        .repeat(refundRequestsMap.keys.size): Unit
    }
  }

  private val epochService = mock[EpochService]

  private def mockEpochGetOptional(): Unit =
    (epochService.getOptional(_: String)).expects(*).returning(Future.successful(Some(InitTime))).repeat(2): Unit

  private def mockEpochSet(expectedOpt: Option[Epoch]): Unit = {
    expectedOpt.foreach { expected =>
      (epochService.set(_: String, _: Epoch)).expects(*, expected).returns(Future.unit)
    }
  }

  private def runTaskWithLoggers(
      brokerClient: BrokerClient): Unit = {

    val tskvLogTask = new TransactionsLogTask(
      accountTransactions,
      accountService,
      psServices,
      new BrokerLogger[BankerOperationEvent](brokerClient),
      epochService,
      trustPurchaseDao
    )(Domains.AutoRu, ec)

    tskvLogTask.execute().futureValue
  }

  private def expectedTransactionResult(
      transactions: Iterable[
        AccountTransaction
      ]): (Seq[BankerOperationEvent], Option[Epoch]) = {
    val processableTransactions = transactions.toSeq
      .sortBy(_.epoch)
      .takeWhile(isProcessable)

    val trEpoch = processableTransactions.lastOption.flatMap(_.epoch)

    val valuableTransactions = processableTransactions.filter(isLoggable)

    val operationEvents = valuableTransactions.map(asBankerOperationEvent(_, User, Domains.AutoRu))

    (operationEvents, trEpoch)
  }

  private def expectedPaymentRequestResult(
      paymentRequestsWithRefund: Iterable[
        PaymentRequestWithRefundRequests
      ]): (Seq[BankerOperationEvent], Option[Epoch]) = {
    val processablePaymentRequests = paymentRequestsWithRefund.filter { r =>
      isProcessable(r.paymentRequest)
    }
    val paymentRequestsEpoch = processablePaymentRequests
      .flatMap(_.paymentRequest.state)
      .flatMap(_.epoch)
      .reduceOption(_ max _)

    val valuablePaymentRequests = processablePaymentRequests.filter(inValidState)

    val operationEvents = valuablePaymentRequests.map(asBankerOperationEvent(_, User, Domains.AutoRu))

    (operationEvents.toSeq, paymentRequestsEpoch)
  }

  private def mockAll(
      transactions: Iterable[AccountTransaction],
      paymentRequestsWithRefund: Iterable[PaymentRequestWithRefundRequests],
      expectedTrEpochOpt: Option[Epoch],
      expectedReqEpochOpt: Option[Epoch]): Unit = {
    mockGetPsId()
    mockEpochGetOptional()
    mockTransactionsGet(transactions)
    val paymentRequests = paymentRequestsWithRefund.map(_.paymentRequest)
    mockPaymentRequestsGet(paymentRequests)
    mockRefundPaymentRequestsGet(paymentRequestsWithRefund)
    mockAccountGet(transactions, paymentRequests)
    mockEpochSet(expectedTrEpochOpt)
    mockEpochSet(expectedReqEpochOpt)
  }

  private def checkTask(trGen: Gen[AccountTransaction], paymentGen: Gen[PaymentRequestWithRefundRequests]): Unit = {
    forAll(Gen.listOfN(5, trGen), Gen.listOfN(5, paymentGen)) { (rawTransactions, rawPayments) =>
      val transactions = prepareTransactions(rawTransactions, AccountId)
      val (expectedTrLines, expectTrEpoch) = expectedTransactionResult(transactions)

      val payments = preparePaymentRequests(rawPayments, AccountId)
      val (expectedPRLines, expectPaymentsEpoch) = expectedPaymentRequestResult(payments)

      mockAll(transactions, payments, expectTrEpoch, expectPaymentsEpoch)
      val expected = expectedTrLines ++ expectedPRLines
      val brokerClientMock = BrokerClientMock[BankerOperationEvent]()
      runTaskWithLoggers(brokerClientMock)
      checkBrokerEvents(brokerClientMock.getMessages, expected)
    }
  }

  private def checkBrokerEvents(actual: Seq[BankerOperationEvent], expected: Seq[BankerOperationEvent]): Unit = {
    actual.map(setTimestamp(_, 0)) should contain theSameElementsAs expected.map(setTimestamp(_, 0))
    val now = System.currentTimeMillis
    val threshold = now - 5 * 60 * 1000 // просто проверяем, что таймстемп не слишком старый
    actual.foreach { operation =>
      Timestamps.toMillis(operation.getTimestamp) > threshold shouldBe true
    }
  }

  private def setTimestamp(operation: BankerOperationEvent, timestamp: Long) =
    operation.toBuilder.setTimestamp(Timestamps.fromMillis(timestamp)).build()

  "TransactionsTskvLogTask" should {
    "process all" in {
      checkTask(
        Gen.oneOf(
          PaymentSystemProcessedTransactionGen,
          PaymentSystemHashUnprocessedTransactionGen,
          HashProcessedTransactionGen
        ),
        Gen.oneOf(
          ProcessedPaymentRequestGen,
          ProcessedPaymentRequestsWithRefundRequestGen
        )
      )
    }
    "not process all" in {
      checkTask(HashUnprocessedTransactionGen, NonProcessedPaymentRequestGen)
    }
    "process correctly" in {
      checkTask(
        Gen.frequency(
          20 -> PaymentSystemProcessedTransactionGen,
          20 -> PaymentSystemHashUnprocessedTransactionGen,
          50 -> HashProcessedTransactionGen,
          10 -> HashUnprocessedTransactionGen
        ),
        Gen.frequency(
          50 -> ProcessedPaymentRequestGen,
          25 -> NonProcessedPaymentRequestGen,
          25 -> ProcessedPaymentRequestsWithRefundRequestGen
        )
      )
    }
    "serialize json operation's payload" in {
      val actual =
        TransactionsLogTask.OperationLogPayload
          .asJsonString(Payload.Json(JsObject("field" -> JsString("data"))))
          .parseJson
      actual shouldBe JsObject("struct" -> JsString("""{"field":"data"}"""))
    }
    "serialize text operation's payload" in {
      val actual = TransactionsLogTask.OperationLogPayload.asJsonString(Payload.Text("some text")).parseJson
      actual shouldBe JsObject("plain" -> JsString("some text"))
    }
    "serialize refund operation's payload" in {
      val payload = Payload.RefundPayload(
        "user1",
        "comment1",
        Some(JsObject("field" -> JsString("data"))),
        OpaquePayload.RefundPayload.Reason.OTHER
      )
      val actual = TransactionsLogTask.OperationLogPayload.asJsonString(payload).parseJson
      actual shouldBe JsObject(
        "refund" -> JsObject(
          "user" -> JsString("user1"),
          "comment" -> JsString("comment1"),
          "value" -> JsString("""{"field":"data"}"""),
          "reason" -> JsString("OTHER")
        )
      )
    }
  }
}

object TransactionsLogTaskSpec extends BankerProtoFormats {

  private val InitTime: Long = DateTimeUtil.now().getMillis

  private def prepareTransactions(
      transactions: Seq[AccountTransaction],
      account: AccountId): Seq[AccountTransaction] = {
    val counter = Gen.choose(InitTime + 1, InitTime + transactions.length * 1000)
    transactions.map { transaction =>
      transaction.copy(
        account = account,
        epoch = Some(counter.next)
      )
    }
  }

  private def preparePaymentRequests(
      sources: Seq[PaymentRequestWithRefundRequests],
      account: AccountId): Seq[PaymentRequestWithRefundRequests] = {
    val paymentRequestEpochGen = Gen.choose(InitTime + 1, InitTime + sources.length * 1000)
    sources.map { source =>
      val paymentRequest = source.paymentRequest
      val changedEpoch = paymentRequestEpochGen.next
      val changedState = paymentRequest.state.get match {
        case i: Incoming =>
          i.copy(
            account = account,
            epoch = Some(changedEpoch)
          )
        case other =>
          throw new IllegalArgumentException(s"Unexpected $other")
      }
      val changedPaymentRequest = paymentRequest.copy(state = Some(changedState))
      val refundRequestEpochGen = Gen.choose(InitTime + 1, changedEpoch + 100)
      val changedRefundRequests = source.refundRequests.map { refundRequest =>
        val changedState = refundRequest.state.get.copy(
          epoch = Some(refundRequestEpochGen.next)
        )

        refundRequest.copy(state = Some(changedState))
      }
      PaymentRequestWithRefundRequests(changedPaymentRequest, changedRefundRequests)
    }
  }

  private def isProcessable(transaction: AccountTransaction): Boolean = {
    transaction.id match {
      case _: HashAccountTransactionId =>
        transaction.status == AccountTransaction.Statuses.Processed
      case _ =>
        true
    }
  }

  private def isProcessable(paymentRequest: PaymentRequest): Boolean =
    paymentRequest.state.exists(_.status == State.Statuses.Processed)

  private def inValidState(pr: PaymentRequestWithRefundRequests): Boolean =
    pr.paymentRequest.state match {
      case Some(p) if p.stateStatus == State.StateStatuses.PartlyRefunded =>
        val processedRefunds = pr.refundRequests.flatMap(_.state).filter { r =>
          r.status == State.Statuses.Processed && r.stateStatus == State.StateStatuses.Valid
        }
        val refunded = processedRefunds.map(_.amount).sum
        p.amount > refunded
      case _ =>
        true
    }

  private def isLoggable(transaction: AccountTransaction) = {
    transaction.id match {
      case _: HashAccountTransactionId =>
        true
      case _ =>
        false
    }
  }

  private def asBankerOperationEvent(
      transaction: AccountTransaction,
      user: User,
      domain: Domains.Value): LogModel.BankerOperationEvent = {
    val `type` = ApiModel.TransactionType.forNumber(transaction.id.`type`.id)
    val status = transaction.activity match {
      case AccountTransaction.Activities.Active =>
        LogModel.BankerOperationEvent.BankerOperationStatus.VALID
      case AccountTransaction.Activities.Inactive =>
        LogModel.BankerOperationEvent.BankerOperationStatus.REFUNDED
    }
    val target = transaction.target.getOrElse(PaymentRequest.getTarget(transaction.payload))
    LogModel.BankerOperationEvent
      .newBuilder()
      .setId(transaction.id.id)
      .setTransactionType(`type`)
      .setGate("ACCOUNT")
      .setMethod("wallet")
      .setAmount(transaction.withdraw)
      .setStatus(status)
      .setPayload(TransactionsLogTask.OperationLogPayload.asJsonString(transaction.payload))
      .setTarget(ApiModel.Target.forNumber(target.id))
      .setOperationTimestamp(transaction.timestamp)
      .setDomain(domain.toString)
      .setAccountId(transaction.account)
      .setUser(user)
      .build()
  }

  private def refundAmount(paymentRequestWithRefund: PaymentRequestWithRefundRequests): Funds = {
    val refunds = paymentRequestWithRefund.refundRequests.flatMap(_.state)
    val valuableRefunds = refunds
      .filter { r =>
        r.status == State.Statuses.Processed && r.stateStatus == State.StateStatuses.Valid
      }
      .map(_.amount)
    valuableRefunds.sum
  }

  private def refundPayloads(
      paymentRequestWithRefund: PaymentRequestWithRefundRequests): Seq[BankerOperationEvent.RefundPayload] =
    for {
      refundRequest <- paymentRequestWithRefund.refundRequests
      state <- refundRequest.state
      if state.status == State.Statuses.Processed && state.stateStatus == State.StateStatuses.Valid
    } yield {
      val builder = LogModel.BankerOperationEvent.RefundPayload
        .newBuilder()
        .setId(state.id)
        .setAmount(state.amount)
        .setComment(refundRequest.source.payload.comment)
        .setUser(refundRequest.source.payload.user)
        .setReason(refundRequest.source.payload.reason)
        .setTimestamp(Timestamps.fromMillis(state.timestamp.getMillis))
      val payloadJsonString = refundRequest.source.payload.value.map(_.compactPrint)
      payloadJsonString.foreach(builder.setValue)
      builder.build()
    }

  private def extractMethod(paymentRequest: PaymentRequest, payment: State): String =
    paymentRequest.method.ps match {
      case PaymentSystemIds.Robokassa =>
        val rawMethod = payment.rawData match {
          case RawPaymentFields(fields) =>
            fields.find(_._1 == "IncCurrLabel").map(_._2)
          case _ => None
        }
        rawMethod.getOrElse(paymentRequest.method.id)
      case _ =>
        paymentRequest.method.id
    }

  private def asBankerOperationEvent(
      paymentRequestWithRefund: PaymentRequestWithRefundRequests,
      user: User,
      domain: Domain): LogModel.BankerOperationEvent = {
    val payment = paymentRequestWithRefund.paymentRequest.state.get

    val request = paymentRequestWithRefund.paymentRequest
    val payload: Payload =
      request.source.payload

    LogModel.BankerOperationEvent
      .newBuilder()
      .setId(request.id)
      .setTransactionType(ApiModel.TransactionType.INCOMING)
      .setGate(ApiModel.PaymentSystemId.forNumber(request.method.ps.id).toString)
      .setMethod(extractMethod(request, payment))
      .setAmount(payment.amount)
      .setTarget(ApiModel.Target.forNumber(request.source.context.target.id))
      .setStatus(asBankerOperationStatus(payment.stateStatus))
      .setPayload(TransactionsLogTask.OperationLogPayload.asJsonString(payload))
      .setOperationTimestamp(payment.timestamp)
      .setDomain(domain.toString)
      .setAccountId(payment.account)
      .setUser(user)
      .setRefund(refundAmount(paymentRequestWithRefund))
      .addAllRefundPayloads(refundPayloads(paymentRequestWithRefund).asJava)
      .build()
  }

  private def asBankerOperationStatus(status: State.StateStatus) =
    status match {
      case StateStatuses.Valid =>
        LogModel.BankerOperationEvent.BankerOperationStatus.VALID
      case StateStatuses.Refunded =>
        LogModel.BankerOperationEvent.BankerOperationStatus.REFUNDED
      case StateStatuses.Cancelled =>
        LogModel.BankerOperationEvent.BankerOperationStatus.CANCELLED
      case StateStatuses.PartlyRefunded =>
        LogModel.BankerOperationEvent.BankerOperationStatus.PARTLY_REFUNDED
    }

}
