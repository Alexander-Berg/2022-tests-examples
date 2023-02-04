package ru.yandex.vertis.billing.backend

import org.scalacheck.Gen
import ru.yandex.vertis.billing.backend.DiscountApplierSpec.ExpectedDiscountCorrection
import ru.yandex.vertis.billing.dao.OrderDao
import ru.yandex.vertis.billing.exceptions.IncomeIgnoredException
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core.gens.{CustomerHeaderGen, DiscountSourceGen, OrderGen, Producer}
import ru.yandex.vertis.billing.model_core.{
  CampaignSnapshot,
  CorrectionRequest,
  CostPerDay,
  CustomerId,
  Discount,
  DiscountPolicy,
  DiscountSourceTypes,
  Funds,
  Order,
  OrderId,
  PercentDiscount,
  Placement,
  Product,
  Rebate,
  Withdraw2,
  WithdrawRequest2
}
import ru.yandex.vertis.billing.service.DiscountService
import ru.yandex.vertis.billing.service.OrderService.{Transparent, WithdrawFilter}
import ru.yandex.vertis.billing.service.checking.EffectiveDiscountsProviders.RealtyCommercialEffectiveDiscountsProvider
import ru.yandex.vertis.billing.tasks.{DiscountCalculateBaseSpec, TransactionStatistics}
import ru.yandex.vertis.billing.util.DateTimeUtils.now
import ru.yandex.vertis.billing.util.{AutomatedContext, DateTimeInterval}

import scala.util.{Failure, Success, Try}

/**
  * Runnable specs on [[DiscountApplier]]
  *
  * @author ruslansd
  * @author alex-kovalenko
  */
class DiscountApplierSpec extends DiscountCalculateBaseSpec {

  val policy = TestDiscountPolicy

  val testTarget = policy.loyaltyTarget
  val testProduct = Product(Placement(CostPerDay(1L)))

  val discountApplier = new DiscountApplier(discountService, orderService)

  def withdrawModifier(w: WithdrawRequest2): WithdrawRequest2 = {
    val snapshot = snapshotModifier(w.snapshot)
    w.copy(snapshot = snapshot, amount = w.amount * 10)
  }

  def snapshotModifier(snapshot: CampaignSnapshot): CampaignSnapshot =
    snapshot.copy(product = testProduct, time = now())

  "DiscountApplier" should {
    val testCustomersNumber = 2
    val testOrderPerCustomerNumber = 2
    val testIterations = 10
    val currentDay = DateTimeInterval.currentDay

    val customers = CustomerHeaderGen.next(testCustomersNumber).toList

    val discounts = customers.flatMap(c => {
      List(DiscountSourceTypes.Amount, DiscountSourceTypes.Loyalty).map(t => {
        val source = DiscountSourceGen.next.copy(
          effectiveSince = currentDay.from,
          value = PercentDiscount((Gen.posNum[Int].next % 15 + 5) * 1000),
          source = t,
          target = testTarget
        )
        Discount(c.id, source)
      })
    })

    val orders = customers.flatMap(c => {
      customerDao.create(c).get
      val orders =
        OrderGen.next(testOrderPerCustomerNumber).map(_.copy(owner = c.id))
      orders.map { order =>
        orderDao.create(c.id, order.properties) match {
          case Success(created) => created
          case other => fail(s"Unexpected $other")
        }
      }
    })

    discountService.upsert(discounts).get
    val expected =
      ExpectedDiscountCorrection(orders, orderDao, discountService, policy)

    def check(): Unit = {
      orders.groupBy(_.owner).foreach { case (owner, group) =>
        group.foreach { order =>
          val transaction = filterCorrection(order)
          transaction.get.foreach(c => c.amount shouldBe expected.discountCorrection(owner, currentDay, order.id))
        }
      }
    }

    def filterCorrection(order: Order): Try[Option[Rebate]] = {
      for {
        transaction <- orderDao.listTransactions(currentDay, Transparent)
        transactions = transaction.collect { case t: Rebate =>
          t
        }
        discountCorrections = transactions.find(_.orderId == order.id)
      } yield discountCorrections
    }

    "provide right corrections with Loyalty and Amount sources" in {
      val statistics = new TransactionStatistics

      val transactions =
        transactionsGen(orders)
          .next(testOrderPerCustomerNumber * 2 * testIterations)
          .filter(!_.isInstanceOf[CorrectionRequest])
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
      statistics.dump()
      discountApplier.apply(currentDay).get

      check()

    }

    "not generate new correction without new discounts and transactions" in {
      discountApplier.apply(currentDay)

      orders.foreach(order => {
        val discountCorrections = for {
          transaction <- orderDao.listTransactions(currentDay, Transparent)
          transactions = transaction.collect { case t: Rebate =>
            t
          }
          discountCorrections = transactions.filter(_.orderId == order.id)
        } yield discountCorrections
        discountCorrections.foreach(_.size should be <= 1)
      })

      check()
    }

    "correct work when added new transactions" in {
      val statistics = new TransactionStatistics

      val modified = for {
        transactions <- orderDao.listTransactions(
          DateTimeInterval(currentDay.from.minusDays(policy.loyaltyDaysWindow), currentDay.to),
          WithdrawFilter
        )
        withdraws = transactions.collect { case w: Withdraw2 =>
          w.copy(amount = Gen.posNum[Int].next, snapshot = snapshotModifier(w.snapshot))
        }
      } yield withdraws

      modified.get.foreach { t =>
        orderDao.withdraw2(t.id, WithdrawRequest2(t.snapshot, t.amount)) match {
          case Success(_) => statistics.withdraw += 1
          case _ => statistics.illegal += 1
        }
        statistics.dump()

        discountApplier.apply(currentDay).get
        check()
      }
    }

    "correct work if discounts modified" in {
      val modified = for {
        effective <- discountService.getEffective(customers.map(_.id), currentDay.to)
        modified = effective.flatMap(_.discounts).map(_.copy(value = PercentDiscount(10000)))
      } yield modified
      discountService.upsert(modified.get)
      discountApplier.apply(currentDay)

      check()
    }

    "correct work with Manually discount source" in {
      val manuallyDiscount = Discount(
        customers.head.id,
        testTarget,
        DiscountSourceTypes.Manually,
        currentDay.from,
        PercentDiscount(10L * 1000)
      )
      discountService.upsert(manuallyDiscount)
      discountApplier.apply(currentDay)

      check()
    }
    val exclusiveManuallyDiscount = Discount(
      customers.head.id,
      testTarget,
      DiscountSourceTypes.ExclusiveManually,
      currentDay.from,
      PercentDiscount(10L * 1000)
    )

    "correct work with ExclusiveManually discount source" in {
      discountService.upsert(exclusiveManuallyDiscount)
      discountApplier.apply(currentDay)

      check()
    }

    "correct work if ExclusiveManually discount sourceis disabled" in {
      val disabled = exclusiveManuallyDiscount.copy(value = PercentDiscount(0))

      discountService.upsert(disabled)
      discountApplier.apply(currentDay)

      check()
    }
  }
}

object DiscountApplierSpec {
  implicit protected val oc = AutomatedContext("DiscountApplyTaskSpec")

  case class ExpectedDiscountCorrection(
      orders: Iterable[Order],
      orderDao: OrderDao,
      discountService: DiscountService,
      policy: DiscountPolicy) {

    val customer2Order = orders.groupBy(_.owner)

    def discountCorrection(owner: CustomerId, interval: DateTimeInterval, order: OrderId): Funds = {
      val withdraws = for {
        transactions <- orderDao.listTransactions(interval, WithdrawFilter)
        orders = customer2Order.getOrElse(owner, Iterable.empty[Order]).map(_.id).toList
        withdraws = transactions.collect {
          case w: Withdraw2 if order == w.orderId && orders.contains(w.orderId) => w
        }
      } yield withdraws

      val effective = for {
        discounts <- discountService.get(owner, interval.to)
        effective <- RealtyCommercialEffectiveDiscountsProvider.toEffectiveDiscounts(owner, discounts)
      } yield effective

      val v = (effective, withdraws) match {
        case (Success(e), Success(wths)) =>
          wths.groupBy(_.orderId).map { p =>
            p._2.foldLeft(0L) { case (f, withdraw) =>
              val add = e.discounts.foldLeft(0L) { case (a, discount) =>
                a + discount.value.calc(withdraw.amount)
              }
              f + add
            }
          }
        case _ =>
          throw new IllegalStateException(s"$effective, $withdraws")
      }

      v.sum
    }
  }
}
