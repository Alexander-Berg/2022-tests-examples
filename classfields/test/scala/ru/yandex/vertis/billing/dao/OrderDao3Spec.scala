package ru.yandex.vertis.billing.dao

import org.joda.time.Interval
import ru.yandex.vertis.billing.model_core.gens.{CampaignSnapshotGen, Producer}
import ru.yandex.vertis.billing.model_core.{CurrentLimit, Funds, LimitSetting, WithdrawRequest2, WithdrawResponse2}
import ru.yandex.vertis.billing.util.DateTimeUtils.{endOfToday, now, startOfToday, wholeDay, wholeWeek}

import scala.util.Try

/**
  * Specs on limited withdraws of [[OrderDao]].
  */
trait OrderDao3Spec extends OrderDaoSpecBase {

  "OrderDao (limited withdraw part)" should {
    "not withdraw from empty order" in {
      val order = createOrder()
      val snapshot = CampaignSnapshotGen.next.copy(orderId = order.id)
      val limit = currentLimit(Some(100))

      expect1(withdraw = Some(0), overdraft = Some(1), balance = Some(0)) {
        orderDao.withdraw2("0.0", WithdrawRequest2(snapshot, 1, limit))
      }
    }

    "preserve limit on sequential withdraws" in {
      val order = createOrder(Some(100))

      val snapshot = CampaignSnapshotGen.next.copy(orderId = order.id)
      val limit = currentLimit(Some(100))

      expect1(withdraw = Some(1), balance = Some(99)) {
        orderDao.withdraw2("0.1", WithdrawRequest2(snapshot, 1, limit))
      }
      expect1(withdraw = Some(50), balance = Some(49)) {
        orderDao.withdraw2("0.2", WithdrawRequest2(snapshot, 50, limit))
      }
      expect1(withdraw = Some(49), overdraft = Some(1), balance = Some(0)) {
        orderDao.withdraw2("0.3", WithdrawRequest2(snapshot, 50, limit))
      }
      expect1(withdraw = Some(0), overdraft = Some(50), balance = Some(0)) {
        orderDao.withdraw2("0.4", WithdrawRequest2(snapshot, 50, limit))
      }
    }

    "preserve daily limit on gradually increased single withdraw" in {
      val order = createOrder(Some(100))

      val snapshot = CampaignSnapshotGen.next.copy(orderId = order.id)
      val limit = currentLimit(Some(100))
      val id = "1.1"

      expect1(withdraw = Some(1), balance = Some(99)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 1, limit))
      }
      expect1(withdraw = Some(50), balance = Some(50)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 50, limit))
      }
      expect1(withdraw = Some(100), balance = Some(0)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 100, limit))
      }
      expect1(withdraw = Some(100), overdraft = Some(1), balance = Some(0)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 101, limit))
      }
      expect1(withdraw = Some(100), overdraft = Some(100), balance = Some(0)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 200, limit))
      }
    }

    "preserve limit on oscillate single withdraw" in {
      val order = createOrder(Some(100))
      val snapshot = CampaignSnapshotGen.next.copy(orderId = order.id)
      val limit = currentLimit(Some(100))
      val id = "2.1"

      expect1(withdraw = Some(50), balance = Some(50)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 50, limit))
      }
      expect1(withdraw = Some(1), balance = Some(99)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 1, limit))
      }
      expect1(withdraw = Some(100), overdraft = Some(50), balance = Some(0)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 150, limit))
      }
      expect1(withdraw = Some(50), balance = Some(50)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 50, limit))
      }
      expect1(withdraw = Some(100), overdraft = Some(100), balance = Some(0)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 200, limit))
      }
    }

    "preserve limit on already overdrafted order" in {
      val order = createOrder(Some(100))
      val snapshot = CampaignSnapshotGen.next.copy(orderId = order.id)
      val limit = currentLimit(Some(100))

      orderDao.withdraw2("3.1", WithdrawRequest2(snapshot, 100, CurrentLimit.Empty))

      expect1(withdraw = Some(0), overdraft = Some(1), balance = Some(0)) {
        orderDao.withdraw2("3.2", WithdrawRequest2(snapshot, 1, limit))
      }
    }

    "not lead to negative order amount on gradually increased single withdraw" in {
      val order = createOrder(Some(50))

      val snapshot = CampaignSnapshotGen.next.copy(orderId = order.id)
      val limit = CurrentLimit.Empty
      val id = "4.1"

      expect1(withdraw = Some(1), balance = Some(49)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 1, limit))
      }
      expect1(withdraw = Some(50), balance = Some(0)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 50, limit))
      }
      expect1(withdraw = Some(50), overdraft = Some(50), balance = Some(0)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 100, limit))
      }
    }

    "get all spent" in {
      var _id = 0
      def id() = {
        _id += 1
        "5." + _id
      }

      val order = createOrder(Some(100))
      val snapshot = CampaignSnapshotGen
        .suchThat(o => new Interval(startOfToday(), endOfToday()).contains(o.time))
        .next
        .copy(orderId = order.id)
      val limit = currentLimit(Some(100))
      val snapshot2 = CampaignSnapshotGen
        .suchThat(o => new Interval(startOfToday(), endOfToday()).contains(o.time))
        .next
        .copy(orderId = order.id)

      orderDao.withdraw2(id(), WithdrawRequest2(snapshot, 1, limit))
      orderDao.withdraw2(id(), WithdrawRequest2(snapshot, 1, limit))
      orderDao.withdraw2(id(), WithdrawRequest2(snapshot, 1, limit))
      orderDao.withdraw2(id(), WithdrawRequest2(snapshot2, 2, limit))
      orderDao.withdraw2(id(), WithdrawRequest2(snapshot2, 2, limit))
      orderDao.withdraw2(id(), WithdrawRequest2(snapshot2, 2, limit))

      val spent = orderDao
        .getAllSpent(Iterable(snapshot.campaignId, snapshot2.campaignId), wholeDay(now()))
        .get
      spent(snapshot.campaignId) should be(3)
      spent(snapshot2.campaignId) should be(6)

    }

    "support for zero withdraws" in {
      val order = createOrder(Some(50))

      val snapshot = CampaignSnapshotGen.next.copy(orderId = order.id)
      val limit = CurrentLimit.Empty
      val id = "6.1"

      expect1(withdraw = Some(0), balance = Some(50)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 0, limit))
      }
      expect1(withdraw = Some(1), balance = Some(49)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 1, limit))
      }
      expect1(withdraw = Some(0), balance = Some(50)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 0, limit))
      }
    }

    "support for zero withdraws with limits" in {
      val order = createOrder(Some(50))

      val snapshot = CampaignSnapshotGen.next.copy(orderId = order.id)
      val limit = currentLimit(Some(30))
      val id = "6.2"

      expect1(withdraw = Some(0), balance = Some(50)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 0, limit))
      }
      expect1(withdraw = Some(1), balance = Some(49)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 1, limit))
      }
      expect1(withdraw = Some(0), balance = Some(50)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 0, limit))
      }
      expect1(withdraw = Some(30), overdraft = Some(10), balance = Some(20)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 40, limit))
      }
      expect1(withdraw = Some(0), balance = Some(50)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 0, limit))
      }
      expect1(withdraw = Some(30), overdraft = Some(10), balance = Some(20)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 40, limit))
      }
      expect1(withdraw = Some(20), balance = Some(30)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 20, limit))
      }
      expect1(withdraw = Some(0), balance = Some(50)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 0, limit))
      }
    }

    "correctly support weekly limit" in {
      val order = createOrder(Some(120))
      val week = wholeWeek(now())
      val snapshot = CampaignSnapshotGen.next.copy(orderId = order.id, time = week.from)
      val nextDaySnapshot = snapshot.copy(time = snapshot.time.plusDays(1))
      val limit = currentLimit(None, Some(100))
      val id = "7.1"
      val id2 = "7.2"

      expect1(withdraw = Some(0), balance = Some(120)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 0, limit))
      }
      expect1(withdraw = Some(10), balance = Some(110)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 10, limit))
      }
      expect1(withdraw = Some(100), balance = Some(20)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 100, limit))
      }
      expect1(withdraw = Some(70), balance = Some(50)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 70, limit))
      }
      expect1(withdraw = Some(60), balance = Some(60)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 60, limit))
      }

      // Second withdraw
      expect1(withdraw = Some(0), balance = Some(60)) {
        orderDao.withdraw2(id2, WithdrawRequest2(nextDaySnapshot, 0, limit))
      }
      expect1(withdraw = Some(5), balance = Some(55)) {
        orderDao.withdraw2(id2, WithdrawRequest2(nextDaySnapshot, 5, limit))
      }
      expect1(withdraw = Some(40), balance = Some(20)) {
        orderDao.withdraw2(id2, WithdrawRequest2(nextDaySnapshot, 40, limit))
      }
      expect1(withdraw = Some(40), overdraft = Some(10), balance = Some(20)) {
        orderDao.withdraw2(id2, WithdrawRequest2(nextDaySnapshot, 50, limit))
      }
    }

    "weekly limit exceeded on second day" in {
      val order = createOrder(Some(110))
      val week = wholeWeek(now())
      val snapshot = CampaignSnapshotGen.next.copy(orderId = order.id, time = week.from)

      val nextDaySnapshot = snapshot.copy(time = snapshot.time.plusDays(1))
      val limit = currentLimit(Some(60), Some(100))
      val id = "8.1"
      val id2 = "8.2"

      expect1(withdraw = Some(0), balance = Some(110)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 0, limit))
      }
      expect1(withdraw = Some(10), balance = Some(100)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 10, limit))
      }
      expect1(withdraw = Some(60), overdraft = Some(40), balance = Some(50)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 100, limit))
      }
      expect1(withdraw = Some(20), balance = Some(90)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 20, limit))
      }
      expect1(withdraw = Some(60), balance = Some(50)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 60, limit))
      }

      // Second day
      expect1(withdraw = Some(0), balance = Some(50)) {
        orderDao.withdraw2(id2, WithdrawRequest2(nextDaySnapshot, 0, limit))
      }
      expect1(withdraw = Some(40), balance = Some(10)) {
        orderDao.withdraw2(id2, WithdrawRequest2(nextDaySnapshot, 40, limit))
      }
      expect1(withdraw = Some(40), overdraft = Some(10), balance = Some(10)) {
        orderDao.withdraw2(id2, WithdrawRequest2(nextDaySnapshot, 50, limit))
      }
    }

    "weekly limit exceeded on last day" in {
      val order = createOrder(Some(110))
      val week = wholeWeek(now())
      val snapshot = CampaignSnapshotGen.next.copy(orderId = order.id, time = week.from)

      val lastDaySnapshot = snapshot.copy(time = week.to)
      val limit = currentLimit(Some(60), Some(100))
      val id = "9.1"
      val id2 = "9.2"

      expect1(withdraw = Some(60), balance = Some(50)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 60, limit))
      }

      // Last day
      expect1(withdraw = Some(0), balance = Some(50)) {
        orderDao.withdraw2(id2, WithdrawRequest2(lastDaySnapshot, 0, limit))
      }
      expect1(withdraw = Some(40), balance = Some(10)) {
        orderDao.withdraw2(id2, WithdrawRequest2(lastDaySnapshot, 40, limit))
      }
      expect1(withdraw = Some(40), overdraft = Some(10), balance = Some(10)) {
        orderDao.withdraw2(id2, WithdrawRequest2(lastDaySnapshot, 50, limit))
      }
    }

    "next week withdraws" in {
      val order = createOrder(Some(110))
      val week = wholeWeek(now())
      val snapshot = CampaignSnapshotGen.next.copy(orderId = order.id, time = week.from)

      val nextWeekSnapshot = snapshot.copy(time = week.to.plusDays(1))
      val limit = currentLimit(Some(60), Some(100))
      val id = "10.1"
      val id2 = "10.2"

      expect1(withdraw = Some(60), balance = Some(50)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 60, limit))
      }

      // Next week
      expect1(withdraw = Some(0), balance = Some(50)) {
        orderDao.withdraw2(id2, WithdrawRequest2(nextWeekSnapshot, 0, limit))
      }
      expect1(withdraw = Some(40), balance = Some(10)) {
        orderDao.withdraw2(id2, WithdrawRequest2(nextWeekSnapshot, 40, limit))
      }
      expect1(withdraw = Some(50), balance = Some(0)) {
        orderDao.withdraw2(id2, WithdrawRequest2(nextWeekSnapshot, 50, limit))
      }
    }

    "if balance less than limit" in {
      val order = createOrder(Some(50))
      val week = wholeWeek(now())
      val snapshot = CampaignSnapshotGen.next.copy(orderId = order.id, time = week.from)

      val limit = currentLimit(Some(60), Some(100))
      val id = "11"

      expect1(withdraw = Some(50), overdraft = Some(10), balance = Some(0)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 60, limit))
      }
    }

    "if totalSpent zero with existing withdraws" in {
      val order = createOrder(Some(100))
      val snapshot = CampaignSnapshotGen.next.copy(orderId = order.id)
      val correctionId = "12"
      val id = "13.1"

      expect1(withdraw = Some(100), balance = Some(0)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 100))
      }

      orderDao.correct(correctionId, order.id, now(), 100, "Test").get

      expect1(withdraw = Some(100), balance = Some(100)) {
        orderDao.withdraw2(id, WithdrawRequest2(snapshot, 100))
      }

    }

  }

  private def currentLimit(daily: Option[Funds], weekly: Option[Funds] = None) = {
    val dL = daily.map(LimitSetting.Daily(_, now()))
    val wL = weekly.map(LimitSetting.Weekly(_, now()))
    CurrentLimit(dL, wL)
  }

  private def expect1(
      withdraw: Option[Funds] = None,
      overdraft: Option[Funds] = None,
      balance: Option[Funds] = None
    )(tryResponse: Try[WithdrawResponse2]) = {
    val response = tryResponse.get
    response.withdraw.amount shouldBe withdraw.get
    response.overdraft match {
      case Some(transaction) if overdraft.isDefined =>
        transaction.amount should be(overdraft.get)
      case None if overdraft.isEmpty =>
      case other =>
        fail(s"Expected overdraft [$overdraft], but got [${response.overdraft}]")
    }
    balance match {
      case Some(current) =>
        response.order.balance2.current should be(current)
      case None =>
    }
  }
}
