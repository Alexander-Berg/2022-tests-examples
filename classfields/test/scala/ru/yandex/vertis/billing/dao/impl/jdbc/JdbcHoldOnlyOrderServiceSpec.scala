package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.OrderDao
import ru.yandex.vertis.billing.model_core.{CustomerId, Funds, Order, OrderBalance2, OrderId, OrderProperties}
import ru.yandex.vertis.billing.service.impl.HoldOnlyOrderServiceImpl
import ru.yandex.vertis.billing.service.{HoldOnlyOrderServiceSpec, OrderService}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.Success

/**
  * [[HoldOnlyOrderServiceSpec]] with jdbc hold service
  *
  * @author zvez
  */
class JdbcHoldOnlyOrderServiceSpec extends HoldOnlyOrderServiceSpec with JdbcSpecTemplate with MockitoSupport {

  val orderWithoutMoney = 53L
  val nonExistentOrder = 33L

  val holdService = new JdbcHoldDao(holdDatabase)

  val orderDao = {
    val m = mock[OrderDao]

    when(m.get(?, ?))
      .thenReturn(Success(Seq(order(123, 100000))))

    when(m.get(OrderService.GetFilter.ForOrderIds(nonExistentOrder), readFromMaster = false))
      .thenReturn(Success(Seq()))

    when(m.get(OrderService.GetFilter.ForOrderIds(orderWithoutMoney), readFromMaster = false))
      .thenReturn(Success(Seq(order(orderWithoutMoney, 0))))

    m
  }

  val service = new HoldOnlyOrderServiceImpl(orderDao, holdService)

  def order(id: OrderId, balance: Funds) =
    Order(id, CustomerId(id, None), OrderProperties("test", None), OrderBalance2(balance, 0))
}
