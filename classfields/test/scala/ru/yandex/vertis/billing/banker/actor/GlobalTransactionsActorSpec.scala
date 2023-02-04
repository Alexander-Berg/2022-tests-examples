package ru.yandex.vertis.billing.banker.actor

import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{atLeastOnce, reset, times, verify, verifyNoInteractions}
import ru.yandex.vertis.billing.banker.actor.GlobalTransactionActor.Request
import ru.yandex.vertis.billing.banker.model.AccountTransactions.{Incoming, Refund, Withdraw}
import ru.yandex.vertis.billing.banker.model.gens.{accountTransactionGen, AccountTransactionGen, Producer}
import ru.yandex.vertis.billing.banker.model.{
  AccountTransactionType,
  AccountTransactions,
  ConsumeAccountTransactionRequest,
  ConsumeTransactionDistributor
}
import ru.yandex.vertis.billing.banker.service.AccountTransactionService
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
  * Spec on [[GlobalTransactionActor]]
  *
  * @author alex-kovalenko
  */
class GlobalTransactionsActorSpec extends ActorSpecBase("ConsumeActorSpec") with MockitoSupport {

  implicit val timeout = Timeout(2.seconds)
  val globalTransactions = mock[AccountTransactionService]
  val distributor = mock[ConsumeTransactionDistributor]
  val actor = TestActorRef(GlobalTransactionActor.props(globalTransactions, distributor))

  override protected def beforeEach(): Unit = {
    reset[Any](globalTransactions, distributor)
    when(distributor.consume(?)(?)).thenReturn(Future.successful(()))
    when(globalTransactions.processed(?)(?)).thenReturn(Future.successful(()))
    super.beforeEach()
  }

  val consumeTypes = AccountTransactions.values.filterNot { t =>
    t == Incoming || t == Refund
  }

  "ConsumeActor" should {
    def processConsumeTransaction(`type`: AccountTransactionType): Unit = {
      s"process transactions with type ${`type`}" in {
        val captor: ArgumentCaptor[ConsumeAccountTransactionRequest] =
          ArgumentCaptor.forClass(classOf[ConsumeAccountTransactionRequest])
        forAll(accountTransactionGen(`type`)) { transaction =>
          actor ! Request.One(transaction, expectResponse = false)
          verify(distributor, atLeastOnce()).consume(captor.capture())(?)

          val rq = captor.getValue
          rq.id shouldBe transaction.id
          rq.timestamp shouldBe transaction.timestamp
          rq.account shouldBe transaction.account
          rq.activity shouldBe transaction.activity
          rq.payload shouldBe transaction.payload
          rq.amount shouldBe math.abs(transaction.withdraw)
        }
      }
    }

    processConsumeTransaction(Withdraw)

    "process batch of consume transactions" in {
      val count = 50
      val transactions = AccountTransactionGen
        .suchThat(t => consumeTypes(t.id.`type`))
        .next(count)

      actor ! Request.Batch(transactions, expectResponse = false)

      verify(distributor, times(count)).consume(?)(?)
      verify(globalTransactions, times(count)).processed(?)(?)
    }

    "mark other transactions as processed with no-op" in {
      val count = 10
      val notConsumes = accountTransactionGen(Incoming).next(count)

      notConsumes.foreach(actor ? Request.One(_, expectResponse = true))
      verify(globalTransactions, times(count)).processed(?)(?)

      actor ? Request.Batch(notConsumes, expectResponse = true)
      verify(globalTransactions, times(count * 2)).processed(?)(?)
      verifyNoInteractions(distributor)
    }

    "skip non-consume transactions in batch" in {
      val count = 50
      val mixed = AccountTransactionGen.next(count)
      val consumeCnt = mixed.count(t => consumeTypes(t.id.`type`))

      actor ? Request.Batch(mixed, expectResponse = true)
      verify(distributor, times(consumeCnt)).consume(?)(?)
      verify(globalTransactions, times(count)).processed(?)(?)
    }

    "not mark as processed on fail" in {
      when(distributor.consume(?)(?)).thenReturn(Future.failed(new RuntimeException("artificial")))
      val tr = AccountTransactionGen.suchThat(t => consumeTypes(t.id.`type`)).next

      actor ! Request.One(tr, expectResponse = false)
      verify(distributor).consume(?)(?)
      verifyNoInteractions(globalTransactions)
    }
  }
}
