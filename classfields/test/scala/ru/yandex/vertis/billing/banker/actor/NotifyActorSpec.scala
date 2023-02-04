package ru.yandex.vertis.billing.banker.actor

import akka.actor.Props
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.actor.EffectActorProtocol.{Done, Fail, Request}
import ru.yandex.vertis.billing.banker.actor.NotifyActor.Request.{Batch, One}
import ru.yandex.vertis.billing.banker.actor.NotifyActorSpec.{
  NonPushableNonIncomingOrRefundTransactionGen,
  NotPushableIncomingTransactionGen,
  NotPushableRefundTransactionGen,
  PushableIncomingTransactionNewFormatGen,
  PushableIncomingTransactionOldFormatGen,
  PushableNonIncomingOrRefundTransactionGen,
  PushableRefundTransactionFromNewApiGen,
  PushableRefundTransactionFromOldApiGen
}
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao.{ForIds, TransactionsFilter}
import ru.yandex.vertis.billing.banker.model.AccountTransaction.{Activities, PushStatus, PushStatuses}
import ru.yandex.vertis.billing.banker.model.PaymentRequest.Targets
import ru.yandex.vertis.billing.banker.model.gens.{
  accountTransactionGen,
  withNonEmptyPayload,
  BooleanGen,
  Producer,
  SimpleJsonGen
}
import ru.yandex.vertis.billing.banker.model.{
  AccountTransaction,
  AccountTransactions,
  Payload,
  PaymentRequest,
  PushNotification
}
import ru.yandex.vertis.billing.banker.push.AsyncNotificationClient
import ru.yandex.vertis.billing.banker.service.impl.GlobalAccountTransactionService
import ru.yandex.vertis.billing.banker.util.RequestContext

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
  * Spec on [[NotifyActor]]
  *
  * @author ruslansd
  */
class NotifyActorSpec extends ActorSpecBase("NotifyActorSpec") with AsyncSpecBase with MockFactory {

  private val clientMock = mock[AsyncNotificationClient]

  private def mockNotify(trs: Iterable[AccountTransaction]): Unit = {
    if (trs.nonEmpty) {
      (clientMock
        .notify(_: Iterable[PushNotification]))
        .expects(*)
        .onCall { actual: Iterable[PushNotification] =>
          val expected = trs.map(NotifyActor.toNotification)
          actual should contain theSameElementsAs expected
          Future.unit
        }: Unit
    }
  }

  private val transactionsServiceMock = mock[GlobalAccountTransactionService]

  private def mockUpdatePushStatus(expectedTr: AccountTransaction, expectedPushStatus: PushStatus): Unit = {
    (transactionsServiceMock
      .updatePushStatus(_: AccountTransaction, _: PushStatus)(_: RequestContext))
      .expects(expectedTr, *, *)
      .returns {
        Future.unit
      }: Unit
  }

  private def mockGetTransactions(trs: Iterable[AccountTransaction]): Unit = {
    (transactionsServiceMock
      .get(_: Seq[TransactionsFilter])(_: RequestContext))
      .expects(*, *)
      .onCall { (actual, _) =>
        val ids = trs.map(_.id)
        val expected = Seq(ForIds(ids))
        actual should contain theSameElementsAs expected
        Future.successful(trs)
      }: Unit
  }

  private def send(request: Request): Future[Any] = {
    val actor = TestActorRef(Props(new NotifyActorImpl(clientMock, transactionsServiceMock)))
    implicit val timeout = Timeout(10.seconds)
    val send = actor ? request
    send
  }

  private def sendExpectSuccess(request: Request): Unit =
    send(request).futureValue match {
      case Done =>
      case Fail(e) => throw e
    }

  sealed trait TransactionSource {
    def actual: AccountTransaction
    def forNotify: Seq[AccountTransaction]
  }

  case class TransactionSourceWithoutFetch(actual: AccountTransaction, forNotify: Seq[AccountTransaction])
    extends TransactionSource

  case class TransactionSourceWithFetch(
      actual: AccountTransaction,
      forFetch: AccountTransaction,
      forNotify: Seq[AccountTransaction])
    extends TransactionSource

  private def withExpected(tr: AccountTransaction): TransactionSource = {
    tr.id.`type` match {
      case AccountTransactions.Refund =>
        val paymentTr = accountTransactionGen(AccountTransactions.Incoming).map { t =>
          withNonEmptyPayload(t).copy(
            refund = 0L,
            target = tr.target
          )
        }.next
        val activation = paymentTr.copy(activity = Activities.Active)
        val forNotify =
          if (tr.payload.asRefund.value.isEmpty) {
            Seq(activation, paymentTr.copy(activity = Activities.Inactive))
          } else {
            Seq(activation, tr.copy(refundFor = Some(activation.id)))
          }
        TransactionSourceWithFetch(tr.copy(refundFor = Some(activation.id)), activation, forNotify)
      case _ if tr.activity == Activities.Inactive =>
        TransactionSourceWithoutFetch(tr, Seq(tr.copy(activity = Activities.Active), tr))
      case _ =>
        TransactionSourceWithoutFetch(tr, Seq(tr))
    }
  }

  private def mockAll(zipped: Iterable[TransactionSource]): Unit = {
    val (zippedToPush, zippedToSkip) = zipped.partition { t =>
      NotifyActor.isAbleToPush(t.actual)
    }

    val toPush = zippedToPush.map(_.actual)
    val toSkip = zippedToSkip.map(_.actual)

    toPush.foreach(mockUpdatePushStatus(_, PushStatuses.Ok))
    toSkip.foreach(mockUpdatePushStatus(_, PushStatuses.Skipped))

    val forNotify = zippedToPush.flatMap(_.forNotify).filter(NotifyActor.isAbleToPush)
    mockNotify(forNotify)

    val sourcesWithFetch = zippedToPush.collect { case r: TransactionSourceWithFetch =>
      r
    }

    val forFetch = sourcesWithFetch.map(_.forFetch)
    if (forFetch.nonEmpty) {
      mockGetTransactions(forFetch)
    }
  }

  private def checkSuccessSendOne(gen: Gen[AccountTransaction]): Unit = {
    forAll(gen) { tr =>
      val zipped = withExpected(tr)
      mockAll(Seq(zipped))
      sendExpectSuccess(One(zipped.actual, expectResponse = true))
    }
  }

  private def checkSuccessSendBatch(gen: Gen[AccountTransaction]): Unit = {
    forAll(Gen.nonEmptyListOf(gen)) { trs =>
      val zipped = trs.map(withExpected)
      mockAll(zipped)
      val actual = zipped.map(_.actual)
      sendExpectSuccess(Batch(actual, expectResponse = true))
    }
  }

  private def checkSuccess(checker: Gen[AccountTransaction] => Unit): Unit = {
    "refund transaction from old api" in {
      checker(PushableRefundTransactionFromOldApiGen)
    }
    "refund transaction from new api" in {
      checker(PushableRefundTransactionFromNewApiGen)
    }
    "not pushable refund transaction" in {
      checker(NotPushableRefundTransactionGen)
    }
    "incoming transaction in old format" in {
      checker(PushableIncomingTransactionOldFormatGen)
    }
    "incoming transaction in new format" in {
      checker(PushableIncomingTransactionNewFormatGen)
    }
    "not pushable incoming transaction" in {
      checker(NotPushableIncomingTransactionGen)
    }
    "pushable withdraw transaction" in {
      checker(PushableNonIncomingOrRefundTransactionGen)
    }
    "non pushable withdraw transaction" in {
      checker(NonPushableNonIncomingOrRefundTransactionGen)
    }
  }

  "NotifyActor" should {
    "process One request correctly" when {
      checkSuccess(checkSuccessSendOne)
    }
    "process Batch request correctly" when {
      checkSuccess(checkSuccessSendBatch)
    }
  }
}

object NotifyActorSpec {

  private val PushableRefundTransactionFromOldApiGen: Gen[AccountTransaction] = {
    for {
      tr <- accountTransactionGen(AccountTransactions.Refund)
      payload = tr.payload.asRefund
      changedPayload = payload.copy(value = None)
    } yield tr.copy(
      payload = changedPayload,
      target = Some(Targets.Purchase)
    )
  }

  private val PushableRefundTransactionFromNewApiGen: Gen[AccountTransaction] = {
    for {
      tr <- accountTransactionGen(AccountTransactions.Refund)
      payload = tr.payload.asRefund
      jsonPayload <- SimpleJsonGen
      changedPayload = payload.copy(value = Some(jsonPayload))
    } yield tr.copy(
      payload = changedPayload,
      target = Some(Targets.Purchase)
    )
  }

  private val NotPushableTargets = {
    Seq(Targets.Binding, Targets.Wallet, Targets.ExternalTransfer)
  }

  private val NotPushableTargetsGen: Gen[PaymentRequest.Targets.Value] = {
    Gen.oneOf(NotPushableTargets)
  }

  private val NotPushableRefundTransactionGen: Gen[AccountTransaction] = {
    for {
      tr <- accountTransactionGen(AccountTransactions.Refund)
      changedTarget <- NotPushableTargetsGen
    } yield tr.copy(
      target = Some(changedTarget)
    )
  }

  private val PushableIncomingTransactionOldFormatGen: Gen[AccountTransaction] = {
    for {
      tr <- accountTransactionGen(AccountTransactions.Incoming)
      trWithNonEmptyPayload = withNonEmptyPayload(tr)
    } yield trWithNonEmptyPayload.copy(
      refund = 0L,
      target = Some(Targets.Purchase)
    )
  }

  private val PushableIncomingTransactionNewFormatGen: Gen[AccountTransaction] = {
    for {
      tr <- accountTransactionGen(AccountTransactions.Incoming)
      trWithNonEmptyPayload = withNonEmptyPayload(tr)
    } yield trWithNonEmptyPayload.copy(
      activity = Activities.Active,
      target = Some(Targets.Purchase)
    )
  }

  private val IncomingTransactionWithEmptyPayload: Gen[AccountTransaction] = {
    accountTransactionGen(AccountTransactions.Incoming).map { tr =>
      tr.copy(
        refund = 0L,
        payload = Payload.Empty,
        activity = Activities.Active,
        target = Some(Targets.Purchase)
      )
    }
  }

  private val IncomingTransactionWithNotPushablePayload: Gen[AccountTransaction] = {
    for {
      tr <- accountTransactionGen(AccountTransactions.Incoming)
      changedTarget <- NotPushableTargetsGen
    } yield tr.copy(
      refund = 0L,
      activity = Activities.Active,
      target = Some(changedTarget)
    )
  }

  private val IncomingTransactionWithNonZeroRefund: Gen[AccountTransaction] = {
    for {
      tr <- accountTransactionGen(AccountTransactions.Incoming)
      changedIncome <- Gen.chooseNum(100L, 1000L)
      changedWithdraw <- Gen.oneOf(0L, changedIncome)
      changedRefund <- Gen.chooseNum(1L, changedIncome)
    } yield tr.copy(
      income = changedIncome,
      withdraw = changedWithdraw,
      refund = changedRefund,
      activity = Activities.Active,
      target = Some(Targets.Purchase)
    )
  }

  private val NotPushableIncomingTransactionGen: Gen[AccountTransaction] = {
    Gen.oneOf(
      IncomingTransactionWithEmptyPayload,
      IncomingTransactionWithNotPushablePayload,
      IncomingTransactionWithNonZeroRefund
    )
  }

  private val NonIncomingOrRefundTransactionGen: Gen[AccountTransaction] =
    accountTransactionGen(AccountTransactions.Withdraw)

  private val PushableNonIncomingOrRefundTransactionGen: Gen[AccountTransaction] = {
    NonIncomingOrRefundTransactionGen.map { tr =>
      withNonEmptyPayload(tr).copy(target = Some(Targets.Purchase))
    }
  }

  private val NonPushableNonIncomingOrRefundTransactionGen: Gen[AccountTransaction] = {
    for {
      tr <- NonIncomingOrRefundTransactionGen
      choose <- BooleanGen
      spoiled <-
        if (choose) {
          Gen.const(tr.copy(payload = Payload.Empty))
        } else {
          NotPushableTargetsGen.map { target =>
            tr.copy(target = Some(target))
          }
        }
    } yield spoiled
  }

}
