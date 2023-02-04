package ru.yandex.vertis.billing.tasks

import com.typesafe.config.ConfigFactory
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.balance.model.Balance
import ru.yandex.vertis.billing.dao.impl.jdbc.order.JdbcOrderDao
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcCustomerDao, JdbcDiscountDao, JdbcSpecTemplate}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.orderTransactionRequestGen
import ru.yandex.vertis.billing.service.checking.EffectiveDiscountsProviders.RealtyCommercialEffectiveDiscountsProvider
import ru.yandex.vertis.billing.service.impl.{DiscountServiceImpl, OrderServiceImpl}
import ru.yandex.vertis.billing.settings.BalanceSettings
import ru.yandex.vertis.billing.util.AutomatedContext
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * Base for discount specs
  *
  * @author ruslansd
  * @author alesavin
  */
trait DiscountCalculateBaseSpec extends AnyWordSpec with Matchers with MockitoSupport with JdbcSpecTemplate {

  val customerDao = new JdbcCustomerDao(billingDatabase)
  val orderDao = new JdbcOrderDao(billingDualDatabase)
  val discountDao = new JdbcDiscountDao(billingDatabase)

  private val balanceSettings =
    BalanceSettings("realty", ConfigFactory.parseResources("application-test.conf").getConfig("service.realty.balance"))
  private val balance = mock[Balance]

  protected def withdrawModifier(w: WithdrawRequest2): WithdrawRequest2

  protected val orderService = new OrderServiceImpl(
    orderDao,
    balance,
    balanceSettings
  )

  protected val discountService =
    new DiscountServiceImpl(discountDao, RealtyCommercialEffectiveDiscountsProvider)
  implicit protected val oc = AutomatedContext("test")

  def transactionsGen(orders: Iterable[Order]): Gen[OrderTransactionRequest] =
    for {
      order <- Gen.oneOf(orders.toSeq)
      tr <- orderTransactionRequestGen(order.id)
      modified = tr match {
        case w: WithdrawRequest2 =>
          withdrawModifier(w)
        case other => other
      }
    } yield modified
}

class TransactionStatistics {
  var count = 0
  var income = 0
  var withdraw = 0
  var correction = 0

  var illegal = 0
  var incomeIgnored = 0

  var discountsChecked = 0

  def append(transaction: OrderTransactionRequest): Unit = {
    count += 1
    transaction match {
      case _: TotalIncomesFromBeginningsRequest => income += 1
      case _: WithdrawRequest2 => withdraw += 1
      case _: CorrectionRequest => correction += 1
      case other => throw new IllegalArgumentException(s"Unsupported $other")
    }
  }

  def dump(): Unit = {
    println(
      s"Count $count, Income: $income, Withdraw: $withdraw, " +
        s"Correction: $correction, Illegal: $illegal, " +
        s"Income ignored: $incomeIgnored, " +
        s"Discounts checked: $discountsChecked"
    )
  }
}
