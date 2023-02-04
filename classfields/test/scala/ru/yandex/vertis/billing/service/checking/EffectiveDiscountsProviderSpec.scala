package ru.yandex.vertis.billing.service.checking

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.{
  CustomerId,
  Discount,
  DiscountSourceTypes,
  EffectiveDiscounts,
  PercentDiscount,
  Target
}
import ru.yandex.vertis.billing.service.EffectiveDiscountsProvider
import ru.yandex.vertis.billing.util.DateTimeUtils.now

import scala.util.{Failure, Success}

/**
  * @author ruslansd
  */
trait EffectiveDiscountsProviderSpec extends AnyWordSpec with Matchers {

  def target: Target

  def provider: EffectiveDiscountsProvider

  "EffectiveDiscountsProvider" should {
    val owner = CustomerId(1, None)

    val d1 = Discount(owner, target, DiscountSourceTypes.Amount, now(), PercentDiscount(10 * 1000))
    val d2 = Discount(owner, target, DiscountSourceTypes.Loyalty, now(), PercentDiscount(20 * 1000))
    val d3 = Discount(owner, target, DiscountSourceTypes.Manually, now(), PercentDiscount(30 * 1000))
    val exclusive = Discount(owner, target, DiscountSourceTypes.ExclusiveManually, now(), PercentDiscount(50 * 1000))
    val disabled = Discount(owner, target, DiscountSourceTypes.ExclusiveManually, now(), PercentDiscount(0))
    val usual = Seq(d1, d2, d3)

    def effective(d: Discount*) =
      provider.toEffectiveDiscounts(d.head.owner, d)

    "provide correct effective discounts" in {
      effective(d1, d2, d3).get should be(EffectiveDiscounts(owner, usual))
      effective(d1, d2, d3, exclusive).get should
        be(EffectiveDiscounts(owner, Iterable(exclusive)))
      effective(d1, d2, d3, disabled).get should
        be(EffectiveDiscounts(owner, usual :+ disabled))
    }

    "failure when few owners" in {
      val anotherOwner = CustomerId(2, None)
      val discount = d1.copy(owner = anotherOwner)

      effective(d2, discount) match {
        case Success(_) => fail(s"Expected failure")
        case Failure(_) =>
      }
    }

    "failure when incorrect target" in {
      val anotherTarget = Target.ForCampaign("test")
      val discount = d1.copy(target = anotherTarget)

      effective(discount) match {
        case Success(ds) => ds.discounts.size shouldBe 0
        case Failure(_) =>
      }
    }

    "failure when amount more than 100 percent" in {
      val discount = d1.copy(value = PercentDiscount(PercentDiscount.Max))

      effective(discount, d2) match {
        case Success(_) => fail(s"Expected failure")
        case Failure(_) =>
      }
    }
  }

}
