package ru.yandex.vertis.billing.backend

import org.joda.time.DateTime
import ru.yandex.vertis.billing.backend.DiscountCalculatorSpec.ExpectedDiscounts
import ru.yandex.vertis.billing.model_core.DiscountPolicy.LoyaltyDiscountPolicy
import ru.yandex.vertis.billing.model_core.{CustomerId, DiscountPolicy, DiscountType, WithdrawRequest2}
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.billing.util.DateTimeUtils.now

import scala.util.Random

/**
  * Runnable spec on [[DiscountCalculator]] with loyalty discount.
  *
  * @author alex-kovalenko
  */
class LoyaltyDiscountCalculatorSpec extends DiscountCalculatorSpec {

  lazy val policy = new DiscountPolicy {
    def amount = None
    def loyalty: Option[LoyaltyDiscountPolicy] = Some(TestDiscountPolicy)
  }

  def modifyInterval: DateTimeInterval = {
    val current = DateTimeInterval.currentDay
    DateTimeInterval(current.from.minusDays(Random.nextInt(policy.loyalty.get.loyaltyDaysWindow)), current.to)
  }

  def getExpected(e: ExpectedDiscounts, owner: CustomerId, time: DateTime): DiscountType = e.loyalty(owner, time)

  def withdrawModifier(w: WithdrawRequest2): WithdrawRequest2 = {
    w.copy(snapshot =
      w.snapshot
        .copy(
          product = testProduct,
          time = now().minusDays(Random.nextInt(policy.loyalty.get.loyaltyDaysWindow - 1) + 1)
        )
    )
  }

}
