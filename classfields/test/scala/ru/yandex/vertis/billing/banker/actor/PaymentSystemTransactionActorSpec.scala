package ru.yandex.vertis.billing.banker.actor

import akka.testkit.TestActorRef
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{atLeastOnce, reset, times, verify, verifyNoInteractions}
import ru.yandex.vertis.billing.banker.actor.PaymentSystemTransactionActor.{BatchItem, Request}
import ru.yandex.vertis.billing.banker.model.AccountTransactions.Incoming
import ru.yandex.vertis.billing.banker.model.gens.{accountTransactionGen, AccountTransactionGen, Producer}
import ru.yandex.vertis.billing.banker.model.{AccountTransactionRequest, AccountTransactionResponse}
import ru.yandex.vertis.billing.banker.service.AccountTransactionService
import ru.yandex.vertis.billing.banker.util.RequestContext
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

/**
  * Specs on [[PaymentSystemTransactionActor]]
  *
  * @author alex-kovalenko
  */
class PaymentSystemTransactionActorSpec extends ActorSpecBase("IncomeActor") with MockitoSupport {

  val globalTransactions = mock[AccountTransactionService]
  val psTransactions = mock[AccountTransactionService]
  val actor = TestActorRef(PaymentSystemTransactionActor.props(globalTransactions))

  override protected def beforeEach(): Unit = {
    reset(globalTransactions, psTransactions)
    stub(globalTransactions.execute(_: AccountTransactionRequest)(_: RequestContext)) { case (r, _) =>
      Future.successful(mock[AccountTransactionResponse])
    }
    when(psTransactions.processed(?)(?)).thenReturn(Future.successful(()))
    super.beforeEach()
  }

  "IncomeActor" should {
    "process incoming transactions" in {
      val captor: ArgumentCaptor[AccountTransactionRequest] =
        ArgumentCaptor.forClass(classOf[AccountTransactionRequest])
      forAll(accountTransactionGen(Incoming)) { incoming =>
        actor ! Request.One(incoming, psTransactions, expectResponse = false)
        verify(globalTransactions, atLeastOnce()).execute(captor.capture())(?)
        val rq = captor.getValue
        rq.id shouldBe incoming.id
        rq.timestamp shouldBe incoming.timestamp
        rq.account shouldBe incoming.account
        rq.activity shouldBe incoming.activity
        rq.payload shouldBe incoming.payload
        rq.amount shouldBe incoming.income
      }
    }

    "process batch of incoming transactions" in {
      val count = 50
      val incomings = accountTransactionGen(Incoming).next(count)

      actor ! Request.Batch(Iterable(BatchItem(incomings, psTransactions)), expectResponse = false)

      verify(globalTransactions, times(count)).execute(?)(?)
      verify(psTransactions, times(count)).processed(?)(?)
    }

    "mark other transactions as processed with no-op" in {
      val count = 10
      val notIncomes = AccountTransactionGen
        .suchThat(_.id.`type` != Incoming)
        .next(count)

      notIncomes.foreach(actor ! Request.One(_, psTransactions, expectResponse = false))
      verify(psTransactions, times(count)).processed(?)(?)

      actor ! Request.Batch(Iterable(BatchItem(notIncomes, psTransactions)), expectResponse = false)
      verify(psTransactions, times(count * 2)).processed(?)(?)
      verifyNoInteractions(globalTransactions)
    }

    "skip non-income transactions in batch" in {
      val count = 50
      val mixed = AccountTransactionGen.next(count)
      val incomeCnt = mixed.count(_.id.`type` == Incoming)

      actor ! Request.Batch(Iterable(BatchItem(mixed, psTransactions)), expectResponse = false)
      verify(globalTransactions, times(incomeCnt)).execute(?)(?)
      verify(psTransactions, times(count)).processed(?)(?)
    }

    "not mark as processed on fail" in {
      when(globalTransactions.execute(?)(?)).thenReturn(Future.failed(new RuntimeException("artificial")))
      val tr = accountTransactionGen(Incoming).next

      actor ! Request.One(tr, psTransactions, expectResponse = false)
      verify(globalTransactions).execute(?)(?)
      verifyNoInteractions(psTransactions)
    }
  }

}
