package ru.yandex.vertis.billing.banker.actor

import akka.testkit.{ImplicitSender, TestActorRef}
import org.mockito.Mockito.{atLeastOnce, times, verify}
import org.mockito.{ArgumentCaptor, Mockito}
import ru.yandex.vertis.billing.banker.actor.PaymentActor.{BatchItem, Request}
import ru.yandex.vertis.billing.banker.model.AccountTransaction.Activities
import ru.yandex.vertis.billing.banker.model.State.StateStatuses
import ru.yandex.vertis.billing.banker.model.gens.{
  paymentGen,
  paymentRequestGen,
  AccountTransactionGen,
  PaymentRequestParams,
  Producer,
  StateParams
}
import ru.yandex.vertis.billing.banker.model.{
  AccountTransactionRequest,
  AccountTransactionResponse,
  AccountTransactions,
  PaymentRequest,
  PaymentSystemAccountTransactionId,
  PaymentSystemIds,
  State
}
import ru.yandex.vertis.billing.banker.service.PaymentSystemService
import ru.yandex.vertis.billing.banker.service.impl.PaymentSystemTransactionService
import ru.yandex.vertis.billing.banker.util.RequestContext
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.implicitConversions

/**
  * Specs on [[PaymentActor]]
  *
  * @author alex-kovalenko
  */
class PaymentActorSpec extends ActorSpecBase("PaymentActorSpec") with ImplicitSender with MockitoSupport {

  val actor = TestActorRef(PaymentActor.props())
  private val psTransactions = mock[PaymentSystemTransactionService]
  private val psService = mock[PaymentSystemService]
  private val mockTransaction = AccountTransactionGen.next

  override protected def beforeEach(): Unit = {
    Mockito.reset(psTransactions)
    when(psService.processed(?)(?)).thenReturn(Future.successful(()))
    when(psService.psId).thenReturn(PaymentSystemIds.FreeOfCharge)
    when(psTransactions.execute(?)(?))
      .thenReturn(Future.successful(mock[AccountTransactionResponse]))
    stub(psTransactions.execute(_: AccountTransactionRequest)(_: RequestContext)) { case (rq, _) =>
      Future.successful(AccountTransactionResponse(rq, mockTransaction, None))
    }
    super.beforeEach()
  }

  implicit def asPaymentWithPayload(pr: PaymentRequest): State.EnrichedPayment =
    State.EnrichedPayment(pr.state.get, pr.source.payload, pr.source.context, None)

  implicit def asPaymentWithPayloads(prs: Iterable[PaymentRequest]): Iterable[State.EnrichedPayment] =
    prs.map(asPaymentWithPayload)

  "PaymentActor" should {
    "process payment requests with states" in {
      val captor: ArgumentCaptor[AccountTransactionRequest] =
        ArgumentCaptor.forClass(classOf[AccountTransactionRequest])
      val stateParams = StateParams().withoutStateStatus(StateStatuses.Cancelled)
      val reqGen = paymentRequestGen(
        PaymentRequestParams().withState(stateParams)
      )
      forAll(reqGen) { request =>
        val payment = request.state.get
        actor ! Request.One(request, psTransactions, psService, expectResponse = false)

        verify(psTransactions, atLeastOnce()).execute(captor.capture())(?)
        val rq = captor.getValue
        rq.id should matchPattern {
          case id: PaymentSystemAccountTransactionId
              if id.id == payment.id
                && id.`type` == AccountTransactions.Incoming =>
        }
        rq.account shouldBe payment.account
        rq.timestamp shouldBe payment.timestamp
        val activity = payment.stateStatus match {
          case State.StateStatuses.Valid => Activities.Active
          case State.StateStatuses.PartlyRefunded => Activities.Active
          case State.StateStatuses.Refunded => Activities.Inactive
        }
        rq.activity shouldBe activity
        rq.payload shouldBe request.source.payload
        rq.amount shouldBe payment.amount

      }
    }

    "process batch of valid requests" in {
      val count = 50
      val stateParams = StateParams().withoutStateStatus(StateStatuses.Cancelled)
      val requests = paymentRequestGen(
        PaymentRequestParams().withState(stateParams)
      ).next(count)

      actor ! Request.Batch(Iterable(BatchItem(requests, psTransactions, psService)), expectResponse = false)

      verify(psTransactions, times(count)).execute(?)(?)
    }

    "not send to global transactions on fail" in {
      when(psTransactions.execute(?)(?))
        .thenReturn(Future.failed(new RuntimeException("artificial")))
      val stateParams = StateParams().withoutStateStatus(StateStatuses.Cancelled)
      val request = paymentRequestGen(
        PaymentRequestParams().withState(stateParams)
      ).next

      actor ! Request.One(request, psTransactions, psService, expectResponse = false)
      expectNoMessage(1.millis)

      verify(psTransactions).execute(?)(?)
    }
  }

}
