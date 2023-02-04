package auto.dealers.loyalty.logic.test.placementdiscountservice

import auto.dealers.loyalty.logic.PlacementDiscountServiceLive.discountForStock
import ru.auto.loyalty.placement_discount_policies.PlacementDiscountPolicies
import ru.auto.loyalty.placement_discount_policies.PlacementDiscountPolicies.DiscountLevel
import zio.test.environment.TestEnvironment
import zio.test.{assertCompletes, assertTrue, DefaultRunnableSpec, ZSpec}

object DiscountForStockSpec extends DefaultRunnableSpec {

  private val discountsTable = List(
    DiscountLevel(minimumCarsOnStock = 500, placementDiscountPercent = 40),
    DiscountLevel(minimumCarsOnStock = 300, placementDiscountPercent = 25),
    DiscountLevel(minimumCarsOnStock = 100, placementDiscountPercent = 10)
  )

  private val examplePolicy = PlacementDiscountPolicies(discounts =
    discountsTable.permutations.toVector(3) // some arbitrary permutation to make it unsorted
  )

  override def spec: ZSpec[TestEnvironment, Any] = suite("discountForStock")(
    test("Скидка для склада больше максимального") {
      val discount = discountForStock(600, examplePolicy)
      assertTrue(discount == 40)
    },
    test("Скидка на границе максимального склада уже даёт максимальную скидку") {
      val discount = discountForStock(500, examplePolicy)
      assertTrue(discount == 40)
    },
    test("Скидка для склада где-то посреди таблицы") {
      val discount = discountForStock(350, examplePolicy)
      assertTrue(discount == 25)
    },
    test("Скидка на произвольном граничном значении склада") {
      val discount = discountForStock(300, examplePolicy)
      assertTrue(discount == 25)
    },
    test("Склад на минимальном граничном значении уже даёт скидку") {
      val discount = discountForStock(100, examplePolicy)
      assertTrue(discount == 10)
    },
    test("Склада меньше необходимого - скидка ноль") {
      val discount = discountForStock(30, examplePolicy)
      assertTrue(discount == 0)
    },
    test("Склад для нуля можно не прописывать в таблице, будет ноль") {
      val discount = discountForStock(0, examplePolicy)
      assertTrue(discount == 0)
    },
    test("Можно получить скидку при нулевом складе") {
      val unconditionalDiscount = PlacementDiscountPolicies(discounts =
        Seq(
          DiscountLevel(minimumCarsOnStock = 0, placementDiscountPercent = 15)
        )
      )

      val discount = discountForStock(0, unconditionalDiscount)
      assertTrue(discount == 15)
    },
    test("Не ломается в странных случаях") {
      assertTrue(discountForStock(Int.MinValue, examplePolicy) == 0) &&
      assertTrue(discountForStock(Int.MaxValue, examplePolicy) == 40) &&
      assertTrue(discountForStock(-150, examplePolicy) == 0) &&
      assertTrue(discountForStock(400, PlacementDiscountPolicies()) == 0)
    }
  )
}
