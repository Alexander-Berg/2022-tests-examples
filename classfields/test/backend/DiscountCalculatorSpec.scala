package ru.yandex.vertis.billing.backend

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.yandex.vertis.billing.backend.DiscountCalculatorSpec.ExpectedDiscounts
import ru.yandex.vertis.billing.dao.OrderDao
import ru.yandex.vertis.billing.exceptions.IncomeIgnoredException
import ru.yandex.vertis.billing.model_core.{
  CostPerDay,
  CustomerId,
  DiscountPolicy,
  DiscountType,
  Order,
  Placement,
  Product,
  Withdraw2,
  WithdrawRequest2,
  WithdrawUtils
}
import ru.yandex.vertis.billing.tasks.{DiscountCalculateBaseSpec, TransactionStatistics}
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core.gens.{CustomerHeaderGen, OrderGen, Producer}
import ru.yandex.vertis.billing.service.OrderService.WithdrawFilter
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.billing.util.DateTimeUtils.now

import scala.util.{Failure, Success}

/**
  * Specs on [[DiscountCalculator]]
  *
  * @author alex-kovalenko
  */
trait DiscountCalculatorSpec extends DiscountCalculateBaseSpec {

  val testProduct = Product(Placement(CostPerDay(1L)))

  def policy: DiscountPolicy

  val calculator = new DiscountCalculator(policy, orderService, discountService)

  def modifyInterval: DateTimeInterval

  def getExpected(e: ExpectedDiscounts, owner: CustomerId, time: DateTime): DiscountType

  "DiscountCalculator" should {
    val testCustomersNumber = 2
    val testOrderPerCustomerNumber = 2
    val testIterations = 10

    val customers = CustomerHeaderGen.next(testCustomersNumber)
    val orders = customers.flatMap { c =>
      customerDao.create(c).get
      val orders =
        OrderGen.next(testOrderPerCustomerNumber).map(_.copy(owner = c.id))
      orders.map { order =>
        orderDao.create(c.id, order.properties) match {
          case Success(o) => o
          case other => fail(s"Unexpected $other")
        }
      }
    }

    val expected = ExpectedDiscounts(orders, orderDao, policy)

    "provide discounts if no transactions and task doesn't start" in {
      customers.foreach { c =>
        discountService.get(c.id, now()) match {
          case Success(discounts) if discounts.isEmpty => info(s"Done")
          case other => fail(s"Unexpected $other")
        }
      }
    }

    "provide discounts if no transactions" in {
      calculator.calculate(DateTimeInterval.previousDay).get
      customers.foreach { c =>
        discountService.get(c.id, now()) match {
          case Success(discounts) if discounts.isEmpty => info(s"Done")
          case other => fail(s"Unexpected $other")
        }
      }
    }

    "provide discounts for various transactions" in {
      val statistics = new TransactionStatistics

      def checkDiscounts(): Unit = {
        val transactions =
          transactionsGen(orders).next(testOrderPerCustomerNumber * 2)
        transactions.foreach(statistics.append)

        transactions.foreach { t =>
          orderService.execute2(t) match {
            case Success(_) => ()
            case Failure(_: IllegalArgumentException) =>
              statistics.illegal += 1
            case Failure(_: IncomeIgnoredException) =>
              statistics.incomeIgnored += 1
            case other => fail(s"Unexpected $other")
          }
        }

        calculator.calculate(DateTimeInterval.previousDay).get
        customers.foreach { c =>
          discountService.get(c.id, now()) match {
            case Success(discounts) =>
              discounts.map(_.value) match {
                case Nil => info(s"Done")
                case Seq(discount) =>
                  statistics.discountsChecked += 1
                  discount shouldBe getExpected(expected, c.id, now())
                case other => fail(s"Unexpected $other")
              }
            case other => fail(s"Unexpected $other")
          }
        }

      }

      for (_ <- 1 to testIterations) {
        checkDiscounts()
        statistics.dump()
      }
    }

    "provide discounts for changed transactions" in {
      val statistics = new TransactionStatistics

      val modified = for {
        transactions <- orderDao.listTransactions(modifyInterval, WithdrawFilter)
        withdraws = transactions.collect { case w: Withdraw2 =>
          w.copy(amount = Gen.posNum[Int].next)
        }
      } yield withdraws

      def checkTransaction(t: Withdraw2): Unit = {
        val request = WithdrawRequest2(t.snapshot, t.amount)
        orderDao.withdraw2(t.id, request) match {
          case Success(_) => statistics.withdraw += 1
          case _ => statistics.illegal += 1
        }

        calculator.calculate(DateTimeInterval.previousDay).get
        customers.foreach { c =>
          discountService.get(c.id, now()) match {
            case Success(discounts) =>
              discounts.map(_.value) match {
                case Nil => info(s"Done")
                case Seq(discount) =>
                  statistics.discountsChecked += 1
                  discount shouldBe getExpected(expected, c.id, now())
                case other => fail(s"Unexpected $other")
              }
            case other => fail(s"Unexpected $other")
          }
        }
        statistics.dump()
      }

      modified.get.foreach(checkTransaction)
    }

  }
}

object DiscountCalculatorSpec {

  case class ExpectedDiscounts(orders: Iterable[Order], orderDao: OrderDao, policy: DiscountPolicy) {

    val order2customer = orders.groupBy(_.owner)

    def loyalty(owner: CustomerId, time: DateTime): DiscountType = {
      val current = DateTimeInterval.dayIntervalFrom(time.withTimeAtStartOfDay())
      val interval = DateTimeInterval(current.from.minusDays(181), current.to.minusDays(1))
      val activityDays = orderDao.listActivityDays(interval).get
      val dates = activityDays.getOrElse(owner, Iterable.empty)
      policy.loyalty.get.loyalty(dates, interval)
    }

    def amount(owner: CustomerId, time: DateTime): DiscountType = {
      val interval = DateTimeInterval.previousDayFrom(time)
      val transactions = orderDao.listTransactions(interval, WithdrawFilter)
      val orders =
        order2customer.getOrElse(owner, Iterable.empty[Order]).map(_.id).toList

      val withdraws: Iterable[Withdraw2] = transactions.get.collect {
        case w: Withdraw2 if orders.contains(w.orderId) => w
      }
      val events = WithdrawUtils.countEvents(withdraws)

      policy.amount.get.amount(events)
    }

  }

}
