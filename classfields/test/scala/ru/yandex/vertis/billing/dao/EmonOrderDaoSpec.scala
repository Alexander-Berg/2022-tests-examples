package ru.yandex.vertis.billing.dao

import org.scalatest.Inside.inside
import ru.yandex.vertis.billing.dao.OrderDao2Spec.FundsHolder
import ru.yandex.vertis.billing.dao.impl.jdbc.order.JdbcOrderDaoHelper.checkBalancesConformity
import ru.yandex.vertis.billing.model_core.EventStat.EmonDetails
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.Producer
import ru.yandex.vertis.billing.service.OrderService.GetFilter
import ru.yandex.vertis.billing.util.DateTimeUtils.now

import scala.util.Success

/**
  * More specs on [[OrderDao]] for emon.
  */
trait EmonOrderDaoSpec extends OrderDaoSpecBase {
  private val order: Order = createOrder()

  private val holder = new FundsHolder

  def checkOrderState(order: Order): Unit = {
    order.balance2 should be(holder.asBalance2)
    checkBalancesConformity(holder.asBalance2, order)
  }

  def checkNotUpdated(): Unit = {
    val notUpdated = orderDao.get(GetFilter.ForCustomer(order.owner, order.id)).get.head
    checkOrderState(notUpdated)
  }

  "OrderDao (EMon transactions)" should {

    "handle income" in {
      val updated =
        orderDao.totalIncome(TotalIncomesFromBeginningsRequest(1, "1", order.id, holder.income(200))).get
      checkOrderState(updated._2)
    }

    "handle emon withdraw" in {
      val amount = holder.withdraw("g1", 20)
      val events = customEventStateGen(
        order.id,
        amount / 2,
        fixedProductEventGen.next
      ).next(2).toSeq
      val updated = orderDao.withdrawEvent(EmonTransactionRequest(events, "g1")).get
      checkOrderState(updated.order)
    }

    "handle increased emon withdraw" in {
      val amount = holder.withdraw("g1", 30)
      val events = customEventStateGen(
        order.id,
        amount / 3,
        fixedProductEventGen.next
      ).next(3).toSeq

      val updated = orderDao.withdrawEvent(EmonTransactionRequest(events, "g1")).get
      checkOrderState(updated.order)
    }

    "handle decreased emon withdraw" in {
      val amount = holder.withdraw("g1", 10)
      val events = customEventStateGen(
        order.id,
        amount / 5,
        fixedProductEventGen.next
      ).next(5).toSeq
      val updated = orderDao.withdrawEvent(EmonTransactionRequest(events, "g1")).get
      checkOrderState(updated.order)
    }

    "handle correction" in {
      val updated = orderDao.correct("c1", order.id, now(), holder.correct(10), "Artificial").get
      checkOrderState(updated._2)
    }

    "add details to emon transaction" in {
      val transactionId = "g1"
      val amount = holder.withdraw(transactionId, 30)

      val eventIds = (1 to 3).map { i =>
        val b = fixedProductEventGen.next.toBuilder
        b.getEventIdBuilder.setId(i.toString)
        b.setSnapshotId(i)
        b.build()
      }
      val events = eventIds.map(e => customEventStateGen(order.id, amount / 3, e).next)

      orderDao.withdrawEvent(EmonTransactionRequest(events, transactionId)).get

      inside(orderDao.getTransaction(transactionId, order.id, OrderTransactions.Withdraw, withDetails = true)) {
        case Success(Some(t)) =>
          inside(t) { case Withdraw(_, _, _, _, _, Some(details), _) =>
            inside(details) { case EmonDetails(detailEvents) =>
              if (detailEvents.map(_.getEventStateId).toSet != eventIds.toSet) {
                fail(s"Unexpected details ${detailEvents.mkString(", ")}")
              }
              info("Done")
            }
          }
      }

    }
  }
}
