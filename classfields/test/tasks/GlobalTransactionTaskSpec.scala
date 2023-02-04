package ru.yandex.vertis.billing.banker.tasks

import akka.testkit.TestProbe
import ru.yandex.vertis.billing.banker.actor.{EffectActorProtocol, GlobalTransactionActor}
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao.TransactionsFilter
import ru.yandex.vertis.billing.banker.model.gens.{AccountTransactionGen, Producer}
import ru.yandex.vertis.billing.banker.service.AccountTransactionService

import scala.concurrent.{Await, Future}

/**
  * Runnable spec on [[GlobalTransactionsTask]]
  *
  * @author alex-kovalenko
  */
class GlobalTransactionTaskSpec extends EffectAsyncTaskSpecBase("GlobalTransactionTaskSpec") {

  val globalTransactions = mock[AccountTransactionService]

  val probe = TestProbe()
  val task = new GlobalTransactionsTask(globalTransactions, probe.ref)

  "GlobalTransactionTaskSpec" should {
    "create correct request when has transactions" in {
      val transactions = AccountTransactionGen.next(50).toList
      when(globalTransactions.get(?)(?))
        .thenReturn(Future.successful(transactions))

      val future = task.execute()
      probe.expectMsgPF() { case GlobalTransactionActor.Request.Batch(trs, true) =>
        trs should contain theSameElementsAs transactions
      }
      probe.reply(EffectActorProtocol.Done)
      Await.result(future, timeout.duration)
    }

    "create request even if has not transactions" in {
      when(globalTransactions.get(?)(?))
        .thenReturn(Future.successful(Iterable.empty))

      val future = task.execute()
      probe.expectMsgPF() {
        case GlobalTransactionActor.Request.Batch(trs, true) if trs.isEmpty =>
      }
      probe.reply(EffectActorProtocol.Done)
      Await.result(future, timeout.duration)
    }
  }
}
