package ru.yandex.vertis.billing.banker.tasks

import akka.testkit.TestProbe
import ru.yandex.vertis.billing.banker.actor.{EffectActorProtocol, PaymentSystemTransactionActor}
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao.TransactionsFilter
import ru.yandex.vertis.billing.banker.model.PaymentSystemIds
import ru.yandex.vertis.billing.banker.model.gens.{AccountTransactionGen, Producer}
import ru.yandex.vertis.billing.banker.service.AccountTransactionService
import ru.yandex.vertis.billing.banker.tasks.PaymentSystemTransactionTask.Setup

import scala.concurrent.{Await, Future}

/**
  * Runnable spec on [[PaymentSystemTransactionTask]]
  *
  * @author alex-kovalenko
  */
class PaymentSystemTransactionTaskSpec extends EffectAsyncTaskSpecBase("PaymentSystemTransactionTaskSpec") {

  val first = mock[AccountTransactionService]
  val second = mock[AccountTransactionService]

  val setups = Iterable(Setup(PaymentSystemIds.FreeOfCharge, first), Setup(PaymentSystemIds.Robokassa, second))

  val probe = TestProbe()

  val task = new PaymentSystemTransactionTask(setups, probe.ref)

  "PaymentSystemTransactionTask" should {
    "generate correct request" in {
      val firstTrs = AccountTransactionGen.next(3).toList
      val secondTrs = AccountTransactionGen.next(2).toList
      when(first.get(?)(?))
        .thenReturn(Future.successful(firstTrs))
      when(second.get(?)(?))
        .thenReturn(Future.successful(secondTrs))

      val future = task.execute()
      probe.expectMsgPF() { case PaymentSystemTransactionActor.Request.Batch(items, true) =>
        val asMap = items.map(i => i.psTransactions -> i.trs).toMap
        asMap.size shouldBe 2
        asMap(first) should contain theSameElementsAs firstTrs
        asMap(second) should contain theSameElementsAs secondTrs
      }
      probe.reply(EffectActorProtocol.Done)
      Await.result(future, timeout.duration)
    }

    "filter empty transactions" in {
      val firstTrs = AccountTransactionGen.next(3).toList
      when(first.get(?)(?))
        .thenReturn(Future.successful(firstTrs))
      when(second.get(?)(?))
        .thenReturn(Future.successful(Iterable.empty))

      val future = task.execute()
      probe.expectMsgPF() { case PaymentSystemTransactionActor.Request.Batch(items, true) =>
        items.size shouldBe 1
        val firstItem = items.head
        firstItem.psTransactions shouldBe first
        firstItem.trs shouldBe firstTrs
      }
      probe.reply(EffectActorProtocol.Done)
      Await.result(future, timeout.duration)
    }

    "skip if failed to get transactions" in {
      val secondTrs = AccountTransactionGen.next(3).toList
      when(first.get(?)(?))
        .thenReturn(Future.failed(new RuntimeException("artificial")))
      when(second.get(?)(?))
        .thenReturn(Future.successful(secondTrs))

      val future = task.execute()
      probe.expectMsgPF() { case PaymentSystemTransactionActor.Request.Batch(items, true) =>
        items.size shouldBe 1
        val secondItem = items.head
        secondItem.psTransactions shouldBe second
        secondItem.trs shouldBe secondTrs
      }
      probe.reply(EffectActorProtocol.Done)
      Await.result(future, timeout.duration)
    }
  }
}
