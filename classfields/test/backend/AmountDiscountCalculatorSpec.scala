package ru.yandex.vertis.billing.backend

import org.joda.time.DateTime
import ru.yandex.vertis.billing.backend.DiscountCalculatorSpec.ExpectedDiscounts
import ru.yandex.vertis.billing.model_core.DiscountPolicy.AmountDiscountPolicy
import ru.yandex.vertis.billing.model_core.{CustomerId, DiscountPolicy, DiscountType, WithdrawRequest2}
import ru.yandex.vertis.billing.settings.discount.RealtyCommercialDiscountPolicy
import ru.yandex.vertis.billing.util.DateTimeInterval

/**
  * Runnable spec on [[DiscountCalculator]] with amount discount.
  *
  * @author alex-kovalenko
  */
class AmountDiscountCalculatorSpec extends DiscountCalculatorSpec {

  lazy val policy = new DiscountPolicy {
    def amount: Option[AmountDiscountPolicy] = Some(RealtyCommercialDiscountPolicy)
    def loyalty = None
  }

  def modifyInterval: DateTimeInterval = DateTimeInterval.previousDay

  protected def withdrawModifier(w: WithdrawRequest2) =
    w.copy(snapshot = w.snapshot.copy(product = testProduct))

  def getExpected(e: ExpectedDiscounts, owner: CustomerId, time: DateTime): DiscountType = e.amount(owner, time)
}
