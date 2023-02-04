package ru.yandex.vertis.billing.dao

import ru.yandex.vertis.billing.model_core.TotalIncomesFromBeginningsRequest
import ru.yandex.vertis.billing.service.OrderService.{IncomingFilter, NonNegative}
import ru.yandex.vertis.billing.util.DateTimeInterval

import scala.util.Success

/**
  * @author ruslansd
  */
trait OrderDao5Spec extends OrderDaoSpecBase {

  "OrderDao" should {
    "correctly handle nonNegative filter" in {
      val o = createOrder(Some(5000))
      val id = incomingIds.incrementAndGet().toString
      orderDao.totalIncome(TotalIncomesFromBeginningsRequest(1, id, o.id, 0)) match {
        case Success((in, or)) =>
          in.amount shouldBe -5000
          or.balance2.current shouldBe 0
        case other =>
          fail(s"Unexpected $other")
      }
      val range = ru.yandex.vertis.billing.util.Range(0, 10)

      orderDao.listTransactions(o.owner, o.id, DateTimeInterval.currentDay, IncomingFilter, range).map(_.toList) match {
        case Success(last :: first :: Nil) =>
          last.amount shouldBe -5000
          first.amount shouldBe 5000
        case other =>
          fail(s"Unexpected $other")
      }

      orderDao
        .listTransactions(o.owner, o.id, DateTimeInterval.currentDay, NonNegative(IncomingFilter), range)
        .map(_.toList) match {
        case Success(last :: Nil) =>
          last.amount shouldBe 5000
        case other =>
          fail(s"Unexpected $other")
      }

    }

  }
}
