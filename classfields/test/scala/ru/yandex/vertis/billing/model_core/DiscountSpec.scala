package ru.yandex.vertis.billing.model_core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.Target.{AnyOfTargets, ForProductType}
import ru.yandex.vertis.billing.model_core.gens.{orderTransactionGen, OrderTransactionGenParams, Producer}

/**
  * Spec on [[Discount]]
  *
  * @author ruslansd
  */
class DiscountSpec extends AnyWordSpec with Matchers {

  "Discount" should {

    "correctly handle target creation" in {
      ForProductType(GoodTypes.Placement, CostTypes.CostPerDay)

      intercept[IllegalArgumentException] {
        ForProductType(GoodTypes.Placement, CostTypes.CostPerDay, Some("test"))
      }

      intercept[IllegalArgumentException] {
        ForProductType(GoodTypes.Custom, CostTypes.CostPerDay, None)
      }

      ForProductType(GoodTypes.Custom, CostTypes.CostPerDay, Some("test"))

      {
        val target = ForProductType(GoodTypes.Placement, CostTypes.CostPerDay)
        AnyOfTargets(Seq(target))
      }

      {
        val target = ForProductType(GoodTypes.Placement, CostTypes.CostPerDay)
        intercept[IllegalArgumentException] {
          AnyOfTargets(Seq(AnyOfTargets(Seq(target))))
        }
      }
    }
  }

  "Target" should {

    def withProduct(withdraw2: Withdraw2)(p: Product) =
      withdraw2.copy(snapshot = withdraw2.snapshot.copy(product = p))

    "correctly match transactions" in {
      val anyOfTargets = {
        val targets = Seq(
          ForProductType(GoodTypes.Placement, CostTypes.CostPerIndexing),
          ForProductType(GoodTypes.Custom, CostTypes.CostPerIndexing, Some("test"))
        )
        AnyOfTargets(targets)
      }
      val matchedProducts = Iterable(
        Product(Placement(CostPerIndexing(FixPrice(1)))),
        Product(Placement(CostPerIndexing(DynamicPrice()))),
        Product(Custom("test", CostPerIndexing(FixPrice(1)))),
        Product(Custom("test", CostPerIndexing(DynamicPrice())))
      )

      val nonMatchedProducts = Iterable(
        Product(Placement(CostPerDay(FixPrice(1)))),
        Product(Placement(CostPerDay(DynamicPrice()))),
        Product(Custom("test", CostPerDay(FixPrice(1)))),
        Product(Custom("lal", CostPerIndexing(DynamicPrice())))
      )

      val withdraw = orderTransactionGen(OrderTransactionGenParams().withType(OrderTransactions.Withdraw)).next
        .asInstanceOf[Withdraw2]

      matchedProducts.map(withProduct(withdraw)).foreach { t =>
        anyOfTargets.matches(t) shouldBe true
      }

      nonMatchedProducts.map(withProduct(withdraw)).foreach { t =>
        anyOfTargets.matches(t) shouldBe false
      }
    }
  }
}
