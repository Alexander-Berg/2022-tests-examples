package ru.yandex.vertis.billing.service.impl

import org.joda.time.DateTime
import org.mockito.Mockito
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.balance.model.Balance
import ru.yandex.vertis.billing.dao.OrderDao
import ru.yandex.vertis.billing.dao.OrderDao.ModifiedSince
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.settings.BalanceSettings
import ru.yandex.vertis.billing.util.{AutomatedContext, DateTimeUtils}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.Success

/**
  * Spec on [[OrderServiceImpl]]
  *
  * @author alesavin
  */
class OrderServiceImplSpec extends AnyWordSpec with Matchers with MockitoSupport {

  trait Setup {
    implicit val rc = AutomatedContext("OrderServiceImplSpec")

    val dao = mock[OrderDao]
    val service = new OrderServiceImpl(dao, mock[Balance], mock[BalanceSettings])
  }

  "OrderServiceImpl" should {
    "get orders current state if no adjust time given" in new Setup {
      when(dao.getOrders(?, ?)).thenReturn(Success(Iterable.empty))
      service.getOrders(0L) should matchPattern { case Success(_) =>
      }
      Mockito.verify(dao).getOrders(ModifiedSince(0L), false)
      Mockito.verifyNoMoreInteractions(dao)
    }

    "get adjusted orders state" in new Setup {
      val time = DateTimeUtils.now()
      when(dao.getOrders(?, ?)).thenReturn(Success(Iterable.empty))
      when(dao.getTotalSpent(?, ?)).thenReturn(Success(Map.empty[OrderId, Funds]))

      service.getOrders(1L, Some(time)) should matchPattern { case Success(_) =>
      }
      Mockito.verify(dao).getOrders(ModifiedSince(1L), false)
      Mockito.verify(dao).getTotalSpent(Iterable.empty, time)
    }
  }

  "OrderServiceImpl companion" should {
    "return hourly id" in {
      val product = Product(Placement(CostPerCall(575000L)))

      info(
        OrderServiceImpl.generateHourlyId(
          new DateTime("2015-10-31T12:00:00.000+03:00"),
          1175,
          product,
          "a3a1d825-aca8-42d8-9029-684056aca910",
          FingerprintImpl(Fingerprint.ofProduct(product))
        )
      )
    }
  }
}
