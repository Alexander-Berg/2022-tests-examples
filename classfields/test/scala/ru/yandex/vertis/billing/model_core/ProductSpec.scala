package ru.yandex.vertis.billing.model_core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core.gens.{OfferIdGen, Producer}

/**
  * Spec on [[scala.Product]]
  *
  * @author ruslansd
  */
class ProductSpec extends AnyWordSpec with Matchers {

  private def premium(cost: Funds, target: Option[Good.Target] = target) =
    PremiumPlacement(CostPerOffer(cost), target)

  private def raising(cost: Funds, target: Option[Good.Target] = target) =
    `Raise+Highlighting`(CostPerOffer(cost), target)

  private def target = Some(Good.OfferIdTarget(OfferIdGen.next))

  "Product" should {

    "correct count revenue of few goods" in {
      Product(Set[Good](premium(100000), raising(10000))).revenue(Items(1)) should be(110000)

      Product(Set[Good](premium(100000), raising(10000), premium(100000))).revenue(Items(1)) should be(210000)
    }

    "correctly calculate total cost" in {
      Product(Set[Good](premium(100), raising(200))).totalCost should be(300)

      Product(Set[Good](premium(100), raising(100))).totalCost should be(200)

      Product(Set[Good](premium(100), Raising(CostPerOffer(DynamicPrice())))).totalCost should be(100)
    }

    "correctly checks valid price" in {
      Product(premium(100), None).hasDefinedCost should be(true)

      Product(Placement(CostPerIndexing(DynamicPrice(Some(100)))), None).hasDefinedCost should be(true)

      Product(Placement(CostPerIndexing(DynamicPrice())), None).hasDefinedCost should be(false)
    }

    "support custom goods with non-empty id" in {
      intercept[IllegalArgumentException] {
        Product(Custom("", CostPerIndexing(DynamicPrice())))
      }

      Product(Custom("badge11111", CostPerClick(5000L))).hasDefinedCost should be(true)
      Product(Custom("badge", CostPerIndexing(DynamicPrice()))).hasDefinedCost should be(false)
    }
  }
}
