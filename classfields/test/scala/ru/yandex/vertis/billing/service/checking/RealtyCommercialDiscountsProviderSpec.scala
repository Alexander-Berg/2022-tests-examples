package ru.yandex.vertis.billing.service.checking

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.Target.{AnyOfTargets, ForCampaign}
import ru.yandex.vertis.billing.model_core.gens.{DiscountGen, Producer}
import ru.yandex.vertis.billing.model_core.{CustomerId, Discount, DiscountSourceTypes, PercentDiscount, Target}
import ru.yandex.vertis.billing.service.EffectiveDiscountsProvider
import ru.yandex.vertis.billing.service.checking.EffectiveDiscountsProviders.RealtyCommercialEffectiveDiscountsProvider
import ru.yandex.vertis.billing.settings.discount.RealtyCommercialDiscountPolicy

import scala.util.Success

/**
  * Spec on [[EffectiveDiscountsProviders.RealtyCommercialEffectiveDiscountsProvider]]
  *
  * @author ruslansd
  */
class RealtyCommercialDiscountsProviderSpec extends AnyWordSpec with Matchers with EffectiveDiscountsProviderSpec {

  lazy val target: Target = RealtyCommercialDiscountPolicy.PlacementTarget

  val reducedTarget: Target = {
    val ts = RealtyCommercialDiscountPolicy.PlacementTarget.targets
    AnyOfTargets(ts.drop(2))
  }

  val increasedTarget: Target = {
    val ts = RealtyCommercialDiscountPolicy.PlacementTarget.targets
    AnyOfTargets(ts ++ Seq(ForCampaign("new_campaign")))
  }
  val customer = CustomerId(1, None)
  lazy val provider: EffectiveDiscountsProvider = RealtyCommercialEffectiveDiscountsProvider

  "RealtyCommercialDiscountsProvider" should {
    "pass exclusive manually discounts" in {
      val manually = Discount(
        customer,
        Target.ForCampaign("not from main target"),
        DiscountSourceTypes.Manually,
        DateTime.now(),
        PercentDiscount(50000)
      )

      provider.toEffectiveDiscounts(customer, Iterable(manually)) match {
        case Success(ds) =>
          ds.discounts.size shouldBe 0
        case other =>
          fail(s"Unexpected $other")
      }

      val exclusive = manually.copy(source = DiscountSourceTypes.ExclusiveManually)

      provider.toEffectiveDiscounts(customer, Iterable(exclusive)) should matchPattern { case Success(_) =>
      }
    }

    "correctly process changed target" in {
      val discounts = DiscountGen
        .next(10)
        .toList
        .map(
          _.copy(owner = customer, target = target, source = DiscountSourceTypes.Amount, value = PercentDiscount(1000))
        )

      provider.toEffectiveDiscounts(customer, discounts) match {
        case Success(ds) =>
          ds.discounts should contain theSameElementsAs discounts
        case other =>
          fail(s"Unexpected $other")
      }
      val reducedDiscounts = discounts.map(_.copy(target = reducedTarget))

      provider.toEffectiveDiscounts(customer, discounts ++ reducedDiscounts) match {
        case Success(ds) =>
          ds.discounts should contain theSameElementsAs discounts
        case other =>
          fail(s"Unexpected $other")
      }

      val increasedDiscounts = discounts.map(_.copy(target = increasedTarget))

      provider.toEffectiveDiscounts(customer, discounts ++ reducedDiscounts ++ increasedDiscounts) match {
        case Success(ds) =>
          ds.discounts should contain theSameElementsAs discounts
        case other =>
          fail(s"Unexpected $other")
      }
    }
  }
}
