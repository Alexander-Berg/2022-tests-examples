package ru.yandex.vertis.billing.dao

import ru.yandex.vertis.billing.dao.OrderDao2Spec.FundsHolder
import ru.yandex.vertis.billing.dao.impl.jdbc.order.JdbcOrderDaoHelper.checkBalancesConformity
import ru.yandex.vertis.billing.exceptions.IncomeIgnoredException
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{EventStatRevenueDetailsGen, Producer}
import ru.yandex.vertis.billing.service.OrderService.{GetFilter, WithdrawFilter}
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.billing.util.DateTimeUtils.{now, today}
import ru.yandex.vertis.billing.util.TestingHelpers.RichOrderTransaction

import scala.util.Success

/**
  * More specs on [[OrderDao]].
  */
trait OrderDao2Spec extends OrderDaoSpecBase {

  "OrderDao (transactions part)" should {
    val order = createOrder()

    "not handle income with not long ID" in {
      intercept[IllegalArgumentException] {
        orderDao.totalIncome(TotalIncomesFromBeginningsRequest(1, "foo", order.id, 100)).get
      }
    }

    val holder = new FundsHolder

    def checkOrderState(order: Order): Unit = {
      order.balance2 should be(holder.asBalance2)
      checkBalancesConformity(holder.asBalance2, order)
    }

    def checkOrderState2(transactionAndOrder: (OrderTransaction, Order)): Unit = {
      val order = transactionAndOrder._2
      order.balance2 should be(holder.asBalance2)
      checkBalancesConformity(holder.asBalance2, order)
    }

    def checkOrderState3(response: WithdrawResponse2): Unit = {
      val order = response.order
      order.balance2 should be(holder.asBalance2)
      checkBalancesConformity(holder.asBalance2, order)
    }

    def checkNotUpdated(): Unit = {
      val notUpdated = orderDao.get(GetFilter.ForCustomer(order.owner, order.id)).get.head
      checkOrderState(notUpdated)
    }

    "handle income" in {
      val updated =
        orderDao.totalIncome(TotalIncomesFromBeginningsRequest(1, "1", order.id, holder.income(100))).get
      checkOrderState2(updated)
    }

    "handle other" in {
      val updated =
        orderDao.totalIncome(TotalIncomesFromBeginningsRequest(1, "3", order.id, holder.income(100))).get
      checkOrderState2(updated)
    }

    "not handle obsolete income" in {
      intercept[IncomeIgnoredException] {
        orderDao.totalIncome(TotalIncomesFromBeginningsRequest(1, "2", order.id, 50)).get
      }
      checkNotUpdated()
    }

    "not create transaction on zero income notification" in {
      val order = createOrder()
      val earlierIncomeRequest =
        TotalIncomesFromBeginningsRequest(serviceId = 1, transactionId = "1", order.id, amount = 100)
      val laterZeroIncomeRequest =
        TotalIncomesFromBeginningsRequest(serviceId = 1, transactionId = "2", order.id, amount = 100)
      orderDao.totalIncome(earlierIncomeRequest).get
      orderDao.totalIncome(laterZeroIncomeRequest).get
      val result = orderDao.getTransaction(id = "2", order.id, OrderTransactions.Incoming).get
      result shouldBe None
    }

    "handle initial withdraw" in {
      val updated = orderDao.withdraw2("w1", withdrawRequest(order.id, holder.withdraw("w1", 20))).get
      checkOrderState3(updated)
    }

    "handle increased withdraw" in {
      val updated = orderDao.withdraw2("w1", withdrawRequest(order.id, holder.withdraw("w1", 30))).get
      checkOrderState3(updated)
    }

    "handle decreased withdraw" in {
      val updated = orderDao.withdraw2("w1", withdrawRequest(order.id, holder.withdraw("w1", 10))).get
      checkOrderState3(updated)
    }

    "handle correction" in {
      val updated = orderDao.correct("c1", order.id, now(), holder.correct(10), "Artificial").get
      checkOrderState2(updated)
    }

    "not handle more correction" in {
      intercept[IllegalArgumentException] {
        orderDao.correct("c2", order.id, now(), 100, "Artificial").get
      }
      checkNotUpdated()
    }

    "handle withdraw with duplicate" in {
      // last hour, must be last transaction in list
      val last = DateTimeInterval.currentDay.to
      val request = withdrawRequest(last, order.id, holder.withdraw("w2", 44))

      val updated = orderDao.withdraw2("w2", request).get
      checkOrderState3(updated)
      val transaction = orderDao.getTransaction("w2", order.id, OrderTransactions.Withdraw).get.get

      val duplicate = orderDao.withdraw2("w2", request.copy()).get
      checkOrderState3(duplicate)

      orderDao.getTransaction("w2", order.id, OrderTransactions.Withdraw) match {
        case Success(Some(t)) =>
          t.withoutEpoch shouldBe transaction.withoutEpoch
          t.extractEpoch.get should be > transaction.extractEpoch.get
        case other =>
          fail(s"Unpredicted transaction $other")
      }

      val nonDuplicate = orderDao.withdraw2("w2", withdrawRequest(last, order.id, holder.withdraw("w2", 33))).get
      checkOrderState3(nonDuplicate)

      orderDao.getTransaction("w2", order.id, OrderTransactions.Withdraw) match {
        case Success(Some(t)) =>
          assert(t.id === transaction.id)
          assert(t.amount < transaction.amount)
          assert(t.epoch.get > transaction.epoch.get)
        case other =>
          fail(s"Unpredicted transaction $other")
      }
    }

    "support withdraw2 transaction with details" in {

      orderDao.listTransactions(today(), WithdrawFilter) match {
        case Success(ts) if ts.nonEmpty =>
          ts.collect { case w @ Withdraw2(_, _, _, Some(_), _) =>
            w
          } should be(empty)
        case other => fail(s"Unexpected $other")
      }

      val last = DateTimeInterval.currentDay.to
      val request =
        withdrawRequest(last, order.id, holder.withdraw("w2", 44)).copy(details = Some(EventStatRevenueDetailsGen.next))
      orderDao.withdraw2("w2", request).get

      orderDao.getTransaction("w2", order.id, OrderTransactions.Withdraw) match {
        case Success(Some(t)) =>
          t match {
            case Withdraw2(_, _, _, None, _) => info("Done")
            case other => fail(s"Unexpected $other")
          }
        case other =>
          fail(s"Unpredicted transaction $other")
      }
      orderDao.getTransaction("w2", order.id, OrderTransactions.Withdraw, withDetails = true) match {
        case Success(Some(t)) =>
          t match {
            case Withdraw2(_, _, _, Some(_), _) =>
              info("Done")
            case other => fail(s"Unexpected $other")
          }
        case other =>
          fail(s"Unpredicted transaction $other")
      }

    }
  }
}

object OrderDao2Spec {

  class FundsHolder {
    private var withdraws: Map[TransactionId, Funds] = Map.empty
    private var totalIncome: Funds = 0
    private var totalSpent: Funds = 0

    def income(amount: Funds): Funds = {
      totalIncome += amount
      totalIncome
    }

    def withdraw(id: TransactionId, amount: Funds): Funds = {
      withdraws.get(id) match {
        case Some(alreadySpent) =>
          totalSpent += (amount - alreadySpent)
        case None =>
          totalSpent += amount
      }
      withdraws = withdraws.updated(id, amount)
      amount
    }

    def correct(amount: Funds): Funds = {
      totalSpent -= amount
      amount
    }

    def asBalance2 = OrderBalance2(totalIncome, totalSpent)
  }

}
