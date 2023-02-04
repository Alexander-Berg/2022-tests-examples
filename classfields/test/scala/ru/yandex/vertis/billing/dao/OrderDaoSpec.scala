package ru.yandex.vertis.billing.dao

import org.scalatest.Inside.inside
import ru.yandex.vertis.billing.dao.OrderDao.{All, Filter, ModifiedSince}
import ru.yandex.vertis.billing.dao.impl.jdbc.order.JdbcOrderDaoHelper
import ru.yandex.vertis.billing.model_core.gens.{ClientIdGen, CustomerIdGen, OrderPropertiesGen, Producer}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.service.OrderService.GetTransactionFilter.WithdrawModifiedSinceFilter
import ru.yandex.vertis.billing.service.OrderService._
import ru.yandex.vertis.billing.util.CollectionUtils.RichSeq
import ru.yandex.vertis.billing.util.DateTimeUtils.{now, today}
import ru.yandex.vertis.billing.util.{DateTimeInterval, Page}

import java.sql.SQLException
import scala.util.{Failure, Success}

/**
  * Specs on [[OrderDao]]
  */
trait OrderDaoSpec extends OrderDaoSpecBase {

  "OrderDao" should {
    val order = createOrder()

    "get all for empty ids" in {
      val result = orderDao.getAllSpent(Iterable.empty, today())
      result should be(Success(Map.empty))
    }

    "get order for customer" in {
      orderDao.get(GetFilter.ForCustomer(customer.id, order.id)) should be(Success(Iterable(order)))
    }

    "get order for empty set of order ids" in {
      orderDao.get(GetFilter.ForOrderIds(Iterable.empty)) should be(Success(Iterable.empty))
    }

    "get order for set of order ids" in {
      val order1 = createOrder()
      val order2 = createOrder()
      orderDao.get(GetFilter.ForOrderIds(List(order1.id, order2.id))) should be(Success(List(order1, order2)))
    }

    "not get order for not owner" in {
      val notOwner = CustomerIdGen.suchThat(_ != customer.id).next
      assert(orderDao.get(GetFilter.ForCustomer(notOwner, order.id)).isFailure)
    }

    "list customer's orders" in {
      val count = 20
      for (_ <- 1 to count) {
        createOrder()
      }
      orderDao.list(customer.id, Page(0, orders.size), ListFilter.NoFilter).map(_.toSet) should be(Success(orders))
      orderDao.list(customer.id, Page(1, orders.size), ListFilter.NoFilter).map(_.toSet) should be(Success(Set.empty))
      orderDao.list(customer.id, Page(1, 2), ListFilter.NoFilter).map(_.toSet) match {
        case Success(values) =>
          values should have size 2
        case other =>
          fail(s"Unable to list orders: $other")
      }

      orderDao.list(customer.id, Page(1, 2), ListFilter.ForProduct("strange one")).map(_.toSet) match {
        case Success(values) =>
          values should have size 0
        case other =>
          fail(s"Unable to list orders for product: $other")
      }
    }

    "get agency client orders" in {
      val agencyId = ClientIdGen.next
      val agencyClients = {
        CustomerIdGen.next(5).toList.map(_.copy(agencyId = Some(agencyId)))
      }
      val orders = agencyClients.map(agencyClient => createOrder(customerId = Some(agencyClient)))
      orderDao.get(GetFilter.ForAgency(agencyId)).get should contain theSameElementsAs orders
    }

    "incoming transaction" in {
      val request = TotalIncomesFromBeginningsRequest(1, "1", order.id, 100)

      orderDao.totalIncome(request) match {
        case Success((_, updatedOrder)) =>
          updatedOrder.balance2.current should be(100)
          updatedOrder.balance2.totalIncome should be(100)
          updatedOrder.balance2.totalSpent should be(0)
          checkBalancesConformity(updatedOrder)
        case other =>
          fail(s"Unable to realize incoming transaction: $other")
      }

      info("Duplicated id")
      assert(orderDao.totalIncome(request).isFailure)
    }

    "withdraw transaction" in {
      val date = now()

      val secondTrId = "2"
      val secondTrAmount = 15
      val secondTrRequest = withdrawRequest(date, order.id, 15)

      orderDao.withdraw2(secondTrId, secondTrRequest) match {
        case Success(response: WithdrawResponse2) =>
          val updatedOrder = response.order
          updatedOrder.balance2.current should be(85)
          updatedOrder.balance2.totalIncome should be(100)
          updatedOrder.balance2.totalSpent should be(15)
        case other =>
          fail(s"Unable to realize withdraw transaction: $other")
      }

      val secondTrFirstAttempt = orderDao
        .getTransaction(
          secondTrId,
          secondTrRequest.orderId,
          OrderTransactions.Withdraw
        )
        .get

      secondTrFirstAttempt.isDefined shouldBe true
      val firstEpoch = secondTrFirstAttempt.flatMap(_.epoch)
      firstEpoch.isDefined shouldBe true

      orderDao.withdraw2(secondTrId, secondTrRequest) match {
        case Success(response: WithdrawResponse2) =>
          val updatedOrder = response.order
          updatedOrder.balance2.current should be(85)
          updatedOrder.balance2.totalIncome should be(100)
          updatedOrder.balance2.totalSpent should be(15)
        case other =>
          fail(s"Unable to realize withdraw transaction: $other")
      }

      val secondTrSecondAttempt = orderDao
        .getTransaction(
          secondTrId,
          secondTrRequest.orderId,
          OrderTransactions.Withdraw
        )
        .get

      secondTrSecondAttempt.isDefined shouldBe true
      val secondEpoch = secondTrSecondAttempt.flatMap(_.epoch)
      secondEpoch.isDefined shouldBe true

      (firstEpoch.get < secondEpoch.get) shouldBe true

      val thirdTrId = "3"
      val thirdTrAmount = 6

      orderDao.withdraw2(thirdTrId, withdrawRequest(date.plus(1), order.id, 6)) match {
        case Success(response: WithdrawResponse2) =>
          val updatedOrder = response.order
          updatedOrder.balance2.current should be(79)
          updatedOrder.balance2.totalIncome should be(100)
          updatedOrder.balance2.totalSpent should be(21)
          checkBalancesConformity(updatedOrder)
        case other =>
          fail(s"Unable to realize withdraw transaction: $other")
      }

      val getResult = orderDao.getTransactions(WithdrawModifiedSinceFilter(0L, None, 100))

      getResult match {
        case Success(source) =>
          val buffer = source.toSeq
          buffer.size shouldBe 2

          val second = buffer.head
          second.id shouldBe secondTrId
          second.orderId shouldBe order.id
          second.amount shouldBe secondTrAmount

          val third = buffer(1)
          third.id shouldBe thirdTrId
          third.orderId shouldBe order.id
          third.amount shouldBe thirdTrAmount
          source.map(_.epoch.get).toSeq.isOrdered shouldBe true
        case other =>
          fail(s"Unexpected result: $other")
      }
    }

    "correction transaction" in {
      val date = now()

      orderDao.correct(System.currentTimeMillis().toString, order.id, date, 1, "Correction by 1") match {
        case Success((_, updatedOrder)) =>
          updatedOrder.balance2.current should be(80)
          updatedOrder.balance2.totalIncome should be(100)
          updatedOrder.balance2.totalSpent should be(20)
          checkBalancesConformity(updatedOrder)
        case other =>
          fail(s"Unable to realize correction transaction: $other")
      }

      info("Negative correction")
      intercept[IllegalArgumentException] {
        Correction(System.currentTimeMillis().toString, order.id, date, -1, "Correction by -1")
      }

      info("Empty comment")
      intercept[IllegalArgumentException] {
        Correction(System.currentTimeMillis().toString, order.id, date, 1, "")
      }
    }

    "withdraw emon transaction" in {
      val emonEvents = Seq(
        customEventStateGen(
          order.id,
          10,
          fixedProductEventGen.next
        ).next,
        customEventStateGen(
          order.id,
          5,
          fixedProductEventGen.next
        ).next
      )
      val emonTrRequest = EmonTransactionRequest(emonEvents, "group1")

      inside(orderDao.withdrawEvent(emonTrRequest)) { case Success(WithdrawEventResponse(_, _, _, updatedOrder)) =>
        updatedOrder.balance2.current should be(65)
        updatedOrder.balance2.totalIncome should be(100)
        updatedOrder.balance2.totalSpent should be(35)
      }
    }

    "withdraw second emon transaction" in {
      val emonTrAmount2 = 5

      val eventState2 = customEventStateGen(
        order.id,
        emonTrAmount2,
        fixedProductEventGen.next
      ).next

      inside(orderDao.withdrawEvent(EmonTransactionRequest(Seq(eventState2), "group2"))) {
        case Success(WithdrawEventResponse(_, _, _, updatedOrder)) =>
          updatedOrder.balance2.current should be(60)
          updatedOrder.balance2.totalIncome should be(100)
          updatedOrder.balance2.totalSpent should be(40)
          checkBalancesConformity(updatedOrder)
      }
    }

    "list transaction" in {
      val date = now()

      val interval = DateTimeInterval(date.minusMinutes(1), date)

      val transactions = orderDao.listTransactions(customer.id, order.id, interval, Transparent, Page(0, 10)) match {
        case Success(trs) =>
          trs
        case other =>
          fail(s"Unable to get transactions $other")
      }

      transactions.values.toList match {
        case Withdraw(_, _, _, 5.0, _, _, _)
            :: Withdraw(_, _, _, 15.0, _, _, _)
            :: Correction(_, _, _, 1.0, "Correction by 1", _)
            :: Withdraw2(_, _, 6.0, _, _)
            :: Withdraw2(_, _, 15.0, _, _)
            :: Incoming("1", _, _, 100.0, _) :: Nil =>
        case other =>
          fail(s"Unable to get predicted transaction records: $other")
      }

      info("Check date time for transactions in last 1 min")
      assert(
        transactions.values.count(t => t.timestamp.isBefore(date) && t.timestamp.isAfter(date.minusMinutes(1))) === 6
      )
    }

    "list transaction for non-exist order" in {
      val date = now()
      val transactions = orderDao
        .listTransactions(customer.id, 0, DateTimeInterval(date.minusDays(1), date), Transparent, Page(0, 10))
        .getOrElse {
          fail(s"Unable to get transactions")
        }
      assert(transactions.total === 0)
    }

    "get orders with filters" in {

      def checkFilter(filter: Filter, expected: Seq[Order], unexpected: Seq[Order]): Unit = {
        val result = orderDao.getOrders(filter).get.map(_.value)
        expected.foreach { el =>
          result should contain(el)
        }
        unexpected.foreach { el =>
          result should not(contain(el))
        }
      }

      val existsOrder = orderDao.get(GetFilter.ForCustomer(order.owner, order.id)).get.head
      var anotherOrder = createOrder(Some(1000))

      checkFilter(ModifiedSince(0), Seq(existsOrder), Seq(anotherOrder))
      checkFilter(All, Seq(existsOrder), Seq(anotherOrder))

      val response = orderDao.withdraw2("foo", withdrawRequest(anotherOrder.id, 100)).get
      anotherOrder = response.order

      checkFilter(ModifiedSince(0), Seq(existsOrder, anotherOrder), Seq.empty)
      checkFilter(All, Seq(existsOrder, anotherOrder), Seq.empty)

      val interval = DateTimeInterval(now().minusHours(1), now().plusHours(1))
      val transactions =
        orderDao.listTransactions(anotherOrder.owner, anotherOrder.id, interval, Transparent, Page(0, 100)).get.values

      val transaction = transactions.head

      checkFilter(ModifiedSince(transaction.epoch.get), Seq(anotherOrder), Seq(existsOrder))
      checkFilter(All, Seq(existsOrder, anotherOrder), Seq.empty)
    }

    "get total spent" in {
      val order = createOrder(Some(1000))
      val t1 = now().minusHours(2)
      val t2 = now().minusHours(1)
      orderDao.withdraw2("w1", withdrawRequest(t1, order.id, 100)).get
      orderDao.withdraw2("w2", withdrawRequest(t2, order.id, 200)).get
      val got = orderDao.get(GetFilter.ForOrderIds(order.id)).get.head
      got.balance2.totalSpent shouldBe 300
      orderDao.getTotalSpent(Iterable(order.id), t1).get.contains(order.id) shouldBe false
      orderDao.getTotalSpent(Iterable(order.id), t2).get(order.id) shouldBe 100
      orderDao.getTotalSpent(Iterable(order.id), now()).get(order.id) shouldBe 300
    }

    "get orders info" in {
      orderDao.getOrdersInfo() match {
        case Success(infos) =>
          infos.values.foreach { info =>
            import info.{count, totalIncome, totalSpent}
            assert(count > 0)
            assert(totalIncome > 0)
            assert(totalSpent > 0)
            assert(totalIncome > totalSpent)
          }
        case other => fail(s"Unexpected $other")
      }
    }

    "list transaction with filters" in {
      val date = now()
      orderDao.listTransactions(
        customer.id,
        order.id,
        DateTimeInterval(date.minusMinutes(5), date),
        Transparent,
        Page(0, 10)
      ) match {
        case Success(transactions) =>
          transactions.find(_.getType == OrderTransactions.Overdraft) should be(None)
        case other => fail(s"Unexpected $other")
      }

      orderDao.withdraw2(
        date.plus(1).getMillis.toString,
        withdrawRequest(date.plus(1), order.id, 300)
      ) match {
        case Success(_) => info(s"Done")
        case other => fail(s"Unexpected $other")
      }

      orderDao.listTransactions(
        customer.id,
        order.id,
        DateTimeInterval(date.minusMinutes(5), now()),
        Transparent,
        Page(0, 10)
      ) match {
        case Success(transactions) =>
          transactions.find(_.getType == OrderTransactions.Overdraft) should not be None
        case other => fail(s"Unexpected $other")
      }
      orderDao.listTransactions(
        customer.id,
        order.id,
        DateTimeInterval(date.minusMinutes(5), now()),
        WithoutOverdraft,
        Page(0, 10)
      ) match {
        case Success(transactions) =>
          transactions.find(_.getType == OrderTransactions.Overdraft) should be(None)
        case other => fail(s"Unexpected $other")
      }
    }

    "empty in out order" in {
      val order = createOrder()

      orderDao.getTransactionsOutcome(order.id).map(_.toInOut) match {
        case Success(OrderInOut(0, 0)) => info(s"Done")
        case other => fail(s"Unexpected $other")
      }
    }

    "in out order" in {
      val order = createOrder(Some(1000))
      val withdraw = withdrawRequest(now(), order.id, 100)
      val withdraw2 = withdrawRequest(now().plusDays(1), order.id, 100)

      orderDao.withdraw2(withdraw.snapshot.time.getMillis.toString, withdraw)
      orderDao.withdraw2(withdraw2.snapshot.time.getMillis.toString, withdraw2)

      val current = TransactionsOutcomeQuery(order.id, Some(DateTimeInterval.currentDay))
      val previous = TransactionsOutcomeQuery(order.id, Some(DateTimeInterval.previousDay))

      orderDao.getTransactionsOutcome(current).map(_.toInOut) match {
        case Success(OrderInOut(1000, 100)) => info(s"Done")
        case other => fail(s"Unexpected $other")
      }

      orderDao.getTransactionsOutcome(previous).map(_.toInOut) match {
        case Success(OrderInOut(0, 0)) => info(s"Done")
        case other => fail(s"Unexpected $other")
      }

      orderDao.getTransactionsOutcome(order.id).map(_.toInOut) match {
        case Success(OrderInOut(1000, 200)) => info(s"Done")
        case other => fail(s"Unexpected $other")
      }

    }

    "outcome series" in {
      val income = 10000
      val order = createOrder(Some(income))
      val withdraw = withdrawRequest(now(), order.id, 100)
      val withdraw2 = withdrawRequest(now().plusDays(1), order.id, 100)
      val withdraw3 = withdrawRequest(now().minusDays(1), order.id, 100)

      orderDao.withdraw2(withdraw.snapshot.time.getMillis.toString, withdraw)
      orderDao.withdraw2(withdraw2.snapshot.time.getMillis.toString, withdraw2)
      orderDao.withdraw2(withdraw3.snapshot.time.getMillis.toString, withdraw3)

      val filter = TransactionsOutcomeSeriesQuery(
        order.id,
        None,
        DateTimeInterval(withdraw3.snapshot.time.withTimeAtStartOfDay(), withdraw2.snapshot.time)
      )
      val expectedResult = OrderTransactionsOutcomePoint(
        withdraw3.snapshot.time.toLocalDate,
        OrderTransactionsOutcome(0, withdraw3.amount, 0, 0, 0)
      ) ::
        OrderTransactionsOutcomePoint(
          withdraw.snapshot.time.toLocalDate,
          OrderTransactionsOutcome(income, withdraw.amount, 0, 0, 0)
        ) ::
        OrderTransactionsOutcomePoint(
          withdraw2.snapshot.time.toLocalDate,
          OrderTransactionsOutcome(0, withdraw2.amount, 0, 0, 0)
        ) ::
        Nil

      val series = orderDao.getTransactionsOutcomeSeries(filter)

      series shouldBe Success(expectedResult)
    }

    "outcome series without skip" in {
      val income = 10000
      val order = createOrder(Some(income))
      val withdraw = withdrawRequest(now(), order.id, 100)
      val withdraw2 = withdrawRequest(now().minusDays(1), order.id, 100)

      orderDao.withdraw2(withdraw.snapshot.time.getMillis.toString, withdraw)
      orderDao.withdraw2(withdraw2.snapshot.time.getMillis.toString, withdraw2)

      val filter = TransactionsOutcomeSeriesQuery(
        order.id,
        None,
        DateTimeInterval(withdraw2.snapshot.time.withTimeAtStartOfDay(), withdraw.snapshot.time.plusDays(2))
      )
      val expectedResult = OrderTransactionsOutcomePoint(
        withdraw2.snapshot.time.toLocalDate,
        OrderTransactionsOutcome(0, withdraw2.amount, 0, 0, 0)
      ) ::
        OrderTransactionsOutcomePoint(
          withdraw.snapshot.time.toLocalDate,
          OrderTransactionsOutcome(income, withdraw.amount, 0, 0, 0)
        ) ::
        OrderTransactionsOutcomePoint(
          withdraw.snapshot.time.toLocalDate.plusDays(1),
          OrderTransactionsOutcome(0, 0, 0, 0, 0)
        ) ::
        OrderTransactionsOutcomePoint(
          withdraw.snapshot.time.toLocalDate.plusDays(2),
          OrderTransactionsOutcome(0, 0, 0, 0, 0)
        ) ::
        Nil

      val series = orderDao.getTransactionsOutcomeSeries(filter)

      series shouldBe Success(expectedResult)
    }

    "outcome series with campaign id filter" in {
      val income = 10000
      val order = createOrder(Some(income))
      val withdraw = withdrawRequest(now(), order.id, 100)
      val withdraw2 = withdrawRequest(now().minusDays(1), order.id, 100)

      orderDao.withdraw2(withdraw.snapshot.time.getMillis.toString, withdraw)
      orderDao.withdraw2(withdraw2.snapshot.time.getMillis.toString, withdraw2)

      val filter =
        TransactionsOutcomeSeriesQuery(
          order.id,
          None,
          DateTimeInterval(withdraw2.snapshot.time.withTimeAtStartOfDay(), withdraw.snapshot.time.plusDays(2))
        )
      val filterWithdraw = filter.copy(campaignId = Some(withdraw.snapshot.campaignId))
      val filterWithdraw2 = filter.copy(campaignId = Some(withdraw2.snapshot.campaignId))

      val expectedResult =
        Seq(
          OrderTransactionsOutcomePoint(
            withdraw2.snapshot.time.toLocalDate,
            OrderTransactionsOutcome(0, withdraw2.amount, 0, 0, 0)
          ),
          OrderTransactionsOutcomePoint(
            withdraw.snapshot.time.toLocalDate,
            OrderTransactionsOutcome(income, withdraw.amount, 0, 0, 0)
          ),
          OrderTransactionsOutcomePoint(
            withdraw.snapshot.time.toLocalDate.plusDays(1),
            OrderTransactionsOutcome(0, 0, 0, 0, 0)
          ),
          OrderTransactionsOutcomePoint(
            withdraw.snapshot.time.toLocalDate.plusDays(2),
            OrderTransactionsOutcome(0, 0, 0, 0, 0)
          )
        )

      val withdrawOutcome =
        Seq(
          OrderTransactionsOutcomePoint(withdraw2.snapshot.time.toLocalDate, OrderTransactionsOutcome(0, 0, 0, 0, 0)),
          OrderTransactionsOutcomePoint(
            withdraw.snapshot.time.toLocalDate,
            OrderTransactionsOutcome(0, withdraw.amount, 0, 0, 0)
          ),
          OrderTransactionsOutcomePoint(
            withdraw.snapshot.time.toLocalDate.plusDays(1),
            OrderTransactionsOutcome(0, 0, 0, 0, 0)
          ),
          OrderTransactionsOutcomePoint(
            withdraw.snapshot.time.toLocalDate.plusDays(2),
            OrderTransactionsOutcome(0, 0, 0, 0, 0)
          )
        )

      val withdraw2Outcome =
        Seq(
          OrderTransactionsOutcomePoint(
            withdraw2.snapshot.time.toLocalDate,
            OrderTransactionsOutcome(0, withdraw2.amount, 0, 0, 0)
          ),
          OrderTransactionsOutcomePoint(withdraw.snapshot.time.toLocalDate, OrderTransactionsOutcome(0, 0, 0, 0, 0)),
          OrderTransactionsOutcomePoint(
            withdraw.snapshot.time.toLocalDate.plusDays(1),
            OrderTransactionsOutcome(0, 0, 0, 0, 0)
          ),
          OrderTransactionsOutcomePoint(
            withdraw.snapshot.time.toLocalDate.plusDays(2),
            OrderTransactionsOutcome(0, 0, 0, 0, 0)
          )
        )

      orderDao.getTransactionsOutcomeSeries(filter) shouldBe Success(expectedResult)
      orderDao.getTransactionsOutcomeSeries(filterWithdraw) shouldBe Success(withdrawOutcome)
      orderDao.getTransactionsOutcomeSeries(filterWithdraw2) shouldBe Success(withdraw2Outcome)
    }

    "upsert order" in {

      val order = Order(3334, customer.id, OrderPropertiesGen.next)
      orderDao.attach(order.owner, order.id, order.properties) match {
        case Success(o) =>
          o should be(order)
        case other => fail(s"Unexpected $other")
      }
      orderDao.attach(order.owner, order.id, order.properties.copy(productKey = "new-key")) match {
        case Failure(_: SQLException) => info("Done")
        case other => fail(s"Unexpected $other")
      }

      orderDao.attach(CustomerId(-1L, None), order.id, order.properties) match {
        case Failure(_: SQLException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }

    val order2 = createOrder()

    "operate total incoming for order 2" in {
      val iRequest = TotalIncomesFromBeginningsRequest(1, "4", order2.id, 100)
      orderDao.totalIncome(iRequest) match {
        case Success((_, updatedOrder)) =>
          updatedOrder.balance2.current should be(100)
          updatedOrder.balance2.totalIncome should be(100)
          updatedOrder.balance2.totalSpent should be(0)
          checkBalancesConformity(updatedOrder)
        case other =>
          fail(s"Unable to realize incoming transaction: $other")
      }
    }

    "operate transaction with custom snapshot fingerprint" in {
      val date = now()

      val wrequest = {
        val r = withdrawRequest(date, order2.id, 35)
        r.copy(snapshot = r.snapshot.copy(fingerprint = FingerprintImpl("")))
      }
      orderDao.withdraw2("5", wrequest) match {
        case Success(response: WithdrawResponse2) =>
          val updatedOrder = response.order
          updatedOrder.balance2.current should be(65)
          updatedOrder.balance2.totalIncome should be(100)
          updatedOrder.balance2.totalSpent should be(35)
        case other =>
          fail(s"Unable to realize withdraw transaction: $other")
      }

      val wrequest2 = {
        val r = withdrawRequest(date.plus(1), order2.id, 70)
        r.copy(snapshot = r.snapshot.copy(fingerprint = FingerprintImpl("")))
      }
      orderDao.withdraw2("6", wrequest2) match {
        case Success(response: WithdrawResponse2) =>
          val updatedOrder = response.order
          updatedOrder.balance2.current should be(0)
          updatedOrder.balance2.totalIncome should be(100)
          updatedOrder.balance2.totalSpent should be(100)
          checkBalancesConformity(updatedOrder)
        case other =>
          fail(s"Unable to realize withdraw transaction: $other")
      }
    }
    "list transaction with custom snapshot fingerprint" in {
      val date = now()

      val interval = DateTimeInterval(date.minusMinutes(1), date)

      val transactions = orderDao.listTransactions(customer.id, order2.id, interval, Transparent, Page(0, 10)) match {
        case Success(trs) => trs
        case other =>
          fail(s"Unable to get transactions $other")
      }

      val fingerprint = FingerprintImpl("")
      transactions.values.toList match {
        case Withdraw2("6", CampaignSnapshot(_, _, order2.id, _, `fingerprint`), 65, _, _)
            :: Overdraft("6", order2.id, _, _, 5, _, Some(`fingerprint`), _)
            :: Withdraw2("5", CampaignSnapshot(_, _, order2.id, _, `fingerprint`), 35, _, _)
            :: Incoming("4", _, _, 100, _) :: Nil =>
        case other =>
          fail(s"Unable to get predicted transaction records: $other")
      }
    }

  }

  private def checkBalancesConformity(order: Order): Unit = {
    val inOut = orderDao.getTransactionsOutcome(order.id).get.toInOut
    val balance = OrderBalance2(inOut.totalIncome, inOut.totalSpent)
    JdbcOrderDaoHelper.checkBalancesConformity(balance, order)
  }

}
