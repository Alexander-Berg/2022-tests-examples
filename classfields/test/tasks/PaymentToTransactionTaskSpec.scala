package ru.yandex.vertis.billing.banker.tasks

import akka.testkit.TestProbe
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.billing.banker.actor.{EffectActorProtocol, PaymentActor}
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.{RequestFilter, StateFilter}
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{Context, Targets}
import ru.yandex.vertis.billing.banker.model.State.{EnrichedPayment, Incoming}
import ru.yandex.vertis.billing.banker.model._
import ru.yandex.vertis.billing.banker.model.gens.{
  paymentRequestGen,
  totalIncomingPaymentGen,
  PaymentRequestParams,
  Producer
}
import ru.yandex.vertis.billing.banker.payment.payload.DefaultPayGatePayloadExtractor
import ru.yandex.vertis.billing.banker.service.PaymentSystemService
import ru.yandex.vertis.billing.banker.service.impl.PaymentSystemTransactionService
import ru.yandex.vertis.billing.banker.tasks.PaymentToTransactionTask.Setup
import ru.yandex.vertis.billing.banker.tasks.PaymentToTransactionTaskSpec.{
  genIncomings,
  genRequests,
  genTotalIncomings,
  toPayloadPayment
}
import ru.yandex.vertis.billing.banker.util.DateTimeUtils

import scala.concurrent.{Await, Future}

/**
  * Runnable spec on [[PaymentToTransactionTask]]
  *
  * @author alex-kovalenko
  */
class PaymentToTransactionTaskSpec
  extends EffectAsyncTaskSpecBase("PaymentToTransactionTaskSpec")
  with BeforeAndAfterEach {

  val transactions = mock[PaymentSystemTransactionService]
  val first = mock[PaymentSystemService]
  val second = mock[PaymentSystemService]

  val setups = Iterable(
    Setup(PaymentSystemIds.FreeOfCharge, first, transactions, DefaultPayGatePayloadExtractor),
    Setup(PaymentSystemIds.Robokassa, second, transactions, DefaultPayGatePayloadExtractor)
  )

  val probe = TestProbe()

  val task = new PaymentToTransactionTask(setups, probe.ref)

  override protected def beforeEach(): Unit = {
    Mockito.reset(first, second)
  }

  "PaymentToTransactionTask" should {
    "generate correct request" in {
      val firstRequests = genRequests(3)
      val firstPayments = genIncomings(firstRequests)
      val secondRequests = genRequests(2)
      val secondPayments = genIncomings(secondRequests)
      when(first.getPayments(?[StateFilter])(?)).thenReturn(Future.successful(firstPayments))
      when(first.getPaymentRequests(?[RequestFilter])(?)).thenReturn(Future.successful(firstRequests))
      when(second.getPayments(?[StateFilter])(?)).thenReturn(Future.successful(secondPayments))
      when(second.getPaymentRequests(?[RequestFilter])(?)).thenReturn(Future.successful(secondRequests))

      val future = task.execute()
      probe.expectMsgPF() { case PaymentActor.Request.Batch(items, true) =>
        val asMap = items.map(i => i.psService -> i.ps).toMap
        asMap.size shouldBe 2

        asMap(first) should contain theSameElementsAs
          toPayloadPayment(firstPayments, firstRequests)
        asMap(second) should contain theSameElementsAs
          toPayloadPayment(secondPayments, secondRequests)
      }
      probe.reply(EffectActorProtocol.Done)
      Await.result(future, timeout.duration)
    }

    "use empty payload for TotalIncoming requests" in {
      val firstRequests = genRequests(3)
      val firstPayments = genIncomings(firstRequests)
      val secondPayments = genTotalIncomings(2)
      when(first.getPayments(?[StateFilter])(?)).thenReturn(Future.successful(firstPayments))
      when(first.getPaymentRequests(?[RequestFilter])(?)).thenReturn(Future.successful(firstRequests))
      when(second.getPayments(?[StateFilter])(?)).thenReturn(Future.successful(secondPayments))
      when(second.getPaymentRequests(?[RequestFilter])(?)).thenReturn(Future.successful(Iterable.empty))

      val future = task.execute()
      probe.expectMsgPF() { case PaymentActor.Request.Batch(items, true) =>
        val asMap = items.map(i => i.psService -> i.ps).toMap
        asMap.size shouldBe 2

        asMap(first) should contain theSameElementsAs
          toPayloadPayment(firstPayments, firstRequests)
        asMap(second) should contain theSameElementsAs
          secondPayments.map(p => EnrichedPayment(p, Payload.Empty, Context(Targets.Wallet), None))
      }
      probe.reply(EffectActorProtocol.Done)
      Await.result(future, timeout.duration)
    }

    "skip Incomings without requests" in {
      val firstRequests = genRequests(3)
      val firstPayments = genIncomings(firstRequests)
      val secondPayments = genIncomings(genRequests(2))
      when(first.getPayments(?[StateFilter])(?)).thenReturn(Future.successful(firstPayments))
      when(first.getPaymentRequests(?[RequestFilter])(?)).thenReturn(Future.successful(firstRequests))
      when(second.getPayments(?[StateFilter])(?)).thenReturn(Future.successful(secondPayments))
      when(second.getPaymentRequests(?[RequestFilter])(?)).thenReturn(Future.successful(Iterable.empty))

      val future = task.execute()
      probe.expectMsgPF() { case PaymentActor.Request.Batch(items, true) =>
        items.size shouldBe 1
        val firstItem = items.head
        firstItem.psService shouldBe first
        firstItem.ps should contain theSameElementsAs
          toPayloadPayment(firstPayments, firstRequests)
      }
      probe.reply(EffectActorProtocol.Done)
      Await.result(future, timeout.duration)
    }

    "filter empty payments" in {
      val firstRequests = genRequests(3)
      val firstPayments = genIncomings(firstRequests)
      when(first.getPayments(?[StateFilter])(?)).thenReturn(Future.successful(firstPayments))
      when(first.getPaymentRequests(?[RequestFilter])(?)).thenReturn(Future.successful(firstRequests))
      when(second.getPayments(?[StateFilter])(?)).thenReturn(Future.successful(Iterable.empty))
      when(second.getPaymentRequests(?[RequestFilter])(?)).thenReturn(Future.successful(Iterable.empty))

      val future = task.execute()
      probe.expectMsgPF() { case PaymentActor.Request.Batch(items, true) =>
        items.size shouldBe 1
        val firstItem = items.head
        firstItem.psService shouldBe first
        firstItem.ps should contain theSameElementsAs
          toPayloadPayment(firstPayments, firstRequests)
      }
      probe.reply(EffectActorProtocol.Done)
      Await.result(future, timeout.duration)
    }

    "skip if failed to process payment system" in {
      val secondRequests = genRequests(2)
      val secondPayments = genIncomings(secondRequests)
      when(first.getPayments(?[StateFilter])(?)).thenReturn(Future.successful(Iterable.empty))
      when(first.getPaymentRequests(?[RequestFilter])(?)).thenReturn(Future.successful(Iterable.empty))
      when(second.getPayments(?[StateFilter])(?)).thenReturn(Future.successful(secondPayments))
      when(second.getPaymentRequests(?[RequestFilter])(?)).thenReturn(Future.successful(secondRequests))

      val future = task.execute()
      probe.expectMsgPF() { case PaymentActor.Request.Batch(items, true) =>
        items.size shouldBe 1
        val secondItem = items.head
        secondItem.psService shouldBe second
        secondItem.ps should contain theSameElementsAs
          toPayloadPayment(secondPayments, secondRequests)
      }
      probe.reply(EffectActorProtocol.Done)
      Await.result(future, timeout.duration)
    }
  }
}

object PaymentToTransactionTaskSpec {

  def toPayloadPayment(ps: Iterable[State.Payment], rs: Iterable[PaymentRequest]): Iterable[EnrichedPayment] =
    (ps.zip(rs)).map { case (p, r) =>
      toPayloadPayment(p, r)
    }

  def toPayloadPayment(p: State.Payment, r: PaymentRequest): EnrichedPayment =
    State.EnrichedPayment(p, r.source.payload, r.source.context, Some(PayGateDetails(Payload.Empty, None, None)))

  private[this] val paymentRequestParams = PaymentRequestParams().withoutState

  def genRequests(n: Int): Iterable[PaymentRequest] =
    paymentRequestGen(paymentRequestParams).next(n).toList

  def genTotalIncomings(n: Int): Iterable[State.Payment] =
    totalIncomingPaymentGen().next(n).toList

  def genIncomings(requests: Iterable[PaymentRequest]): Iterable[State.Payment] =
    requests.map(r =>
      Incoming(
        r.id,
        r.source.account,
        r.source.amount,
        DateTimeUtils.now(),
        Raw.Empty,
        State.Statuses.Created,
        State.StateStatuses.Valid,
        None
      )
    )

}
