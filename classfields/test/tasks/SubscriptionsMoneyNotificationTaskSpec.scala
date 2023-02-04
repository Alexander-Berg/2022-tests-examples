package ru.yandex.vertis.billing.tasks

import org.mockito.Answers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.balance.model.Balance
import ru.yandex.vertis.billing.dao.OrderDao
import ru.yandex.vertis.billing.model_core.{
  CustomerId,
  EpochValue,
  Funds,
  MoneyRunningLowNotification,
  NotificationEvent,
  Order,
  OrderBalance2,
  OrderId,
  OrderProperties,
  OrderTransactionsOutcome
}
import ru.yandex.vertis.billing.service.EpochService
import ru.yandex.vertis.billing.service.OrderService.TransactionsOutcomeQuery
import ru.yandex.vertis.billing.service.impl.OrderServiceImpl
import ru.yandex.vertis.billing.service.subscriptions.NotificationEventSink
import ru.yandex.vertis.billing.settings.BalanceSettings
import ru.yandex.vertis.billing.util.{DateTimeInterval, DateTimeUtils}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.annotation.nowarn
import scala.concurrent.duration.FiniteDuration
import scala.util.{Success, Try}

/**
  * Tests for [[SubscriptionsMoneyNotificationTask]]
  *
  * @author zvez
  */
@nowarn("msg=discarded non-Unit value")
class SubscriptionsMoneyNotificationTaskSpec extends AnyWordSpec with Matchers with MockitoSupport {

  private val orderDao = mock[OrderDao]

  private val balance = mock[Balance]

  private val orderService =
    new OrderServiceImpl(orderDao, balance, mock[BalanceSettings]) with HoldMemorizedOrderService

  private val epochService = {
    val m = mock[EpochService](Answers.RETURNS_DEFAULTS)
    when(m.getTry(SubscriptionsMoneyNotificationTask.EpochMarker)).thenReturn(Success(1L))
    when(m.setTry(?, ?)).thenReturn(Success(()))
    m
  }

  var _events: Seq[NotificationEvent] = Nil

  def events = {
    val res = _events.collect { case e: MoneyRunningLowNotification => e.daysLeft }
    _events = Nil
    res
  }

  val sink = new NotificationEventSink {

    def submit(event: NotificationEvent) = Try { _events = _events :+ event }

    override def push(event: NotificationEvent, timeout: FiniteDuration): Try[Unit] =
      submit(event)

  }

  def task(daysLeft: Option[Int] = None) =
    new SubscriptionsMoneyNotificationTask(orderService, epochService, sink, "test", daysLeft)
  val CurrentDay = DateTimeUtils.now().withTimeAtStartOfDay()
  val OrderId: OrderId = 1

  "SubscriptionsMoneyNotificationTask" should {

    "do nothing if no order was changed" in {
      when(orderDao.getOrders(?, ?)).thenReturn(Success(Nil))
      task().run().get
      events should be(Nil)
    }

    "send any notifications if there is enough money for 4 days only" in {
      setState(800, Seq(10, 200, 150, 100))
      task(Some(5)).run().get
      events should be(List(4))
    }

    "not send any notifications if there is enough money for 7 days" in {
      setState(1600, Seq(10, 200, 150, 100))
      task(Some(7)).run().get
      events should be(Nil)
    }

    "not send any notifications if there is enough money for 4 days" in {
      setState(800, Seq(10, 200, 150, 100))
      task().run().get
      events should be(Nil)
    }

    "not send any notifications if current balance is zero" in {
      setState(0, Seq(10, 200, 150, 100))
      task().run() should be(Success(()))
      events should be(Nil)
    }

    "send notification if there is enough money for 3 days" in {
      setState(700, Seq(10, 200, 150, 100))
      task().run().get
      events should be(Seq(3))
    }

    "send notification if there is not enough money for 1 day" in {
      setState(700, Seq(800, 200, 150, 100))
      task().run().get
      events should be(Seq(0))
    }

    "take into account last 3 active days only" in {
      setState(1000, Seq(30, 200, 150, 100, 500))
      task().run().get
      events should be(Nil)
    }

    "don't take into account days without any spendings" in {
      setState(800, Seq(30, 0, 150, 200, 500))
      task().run().get
      events should be(Seq(1))
    }

    "work if there were no spendings" in {
      setState(800, Seq.fill(10)(0))
      task().run().get
      events should be(Nil)
    }

    "not send any notification if there were no spendings today" in {
      setState(800, Seq(0, 20, 150, 200, 500))
      task().run().get
      events should be(Nil)
    }

  }

  def setState(currentBalance: Funds, spendingsPerDay: Seq[Funds]): Unit = {
    val order =
      Order(OrderId, CustomerId(OrderId, None), OrderProperties("some", None), OrderBalance2(currentBalance, 0, 0))

    when(orderDao.getOrders(?, ?)).thenReturn(Success(Seq(EpochValue(order))))

    stub(orderDao.getTransactionsOutcome _)(
      spendingsPerDay.zipWithIndex.map { case (spent, idx) =>
        val period = DateTimeInterval.dayIntervalFrom(CurrentDay.minusDays(idx))
        val result = OrderTransactionsOutcome(0, spent, 0, 0, 0)
        TransactionsOutcomeQuery(OrderId, Some(period)) -> Success(result)
      }.toMap
    )
  }
}
