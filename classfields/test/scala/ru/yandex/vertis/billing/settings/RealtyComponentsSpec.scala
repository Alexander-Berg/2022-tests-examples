package ru.yandex.vertis.billing.settings

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.{CampaignSnapshot, CostPerCall, FixPrice, Funds, Placement, Product}
import ru.yandex.vertis.billing.model_core.gens.CampaignSnapshotGen
import ru.yandex.vertis.billing.model_core.gens.Producer

import scala.util.Random

/**
  * Spec on [[RealtyComponents]]
  *
  * @author ruslansd
  */
class RealtyComponentsSpec extends AnyWordSpec with Matchers {

  val callsRevenueConstraints = RealtyComponents.callsRevenueConstraints.get
  import callsRevenueConstraints.fitCost

  "fitCost" should {
    /*
      https://wiki.yandex-team.ru/realty/newbuildings/1yardstory/v2/v2-auction

      Example 1
     */
    "be correct for example 1" in {
      fitCost(400000, 500000) should be(405000)
      fitCost(400000, 400000) should be(400000)
      fitCost(400000, 300000) should be(300000)
    }
    /*
      https://wiki.yandex-team.ru/realty/newbuildings/1yardstory/v2/v2-auction

      Example 2
     */
    "be correct for example 2" in {
      fitCost(400000, 500000) should be(405000)
      fitCost(400000, 400000) should be(400000)
      fitCost(400000, 300000) should be(300000)
    }
    /*
      https://wiki.yandex-team.ru/realty/newbuildings/1yardstory/v2/v2-auction

      Example 3
     */
    "be correct for example 3" in {
      fitCost(0, 500000) should be(55000)
    }
    /*
      https://wiki.yandex-team.ru/realty/newbuildings/1yardstory/v2/v2-auction

      Example 4
     */
    "be correct for example 4" in {
      fitCost(60000, 60000) should be(60000)
      fitCost(60000, 60000) should be(60000)
      fitCost(60000, 60000) should be(60000)
    }
    /*
      https://wiki.yandex-team.ru/realty/newbuildings/1yardstory/v2/v2-auction

      Example 5
     */
    "be correct for example 5" in {
      fitCost(60000, 75000) should be(65000)
      fitCost(60000, 60000) should be(60000)
      fitCost(60000, 60000) should be(60000)
    }
    /*
      https://wiki.yandex-team.ru/realty/newbuildings/1yardstory/v2/v2-auction

      Example 6
     */
    "be correct for example 6" in {
      fitCost(70000, 75000) should be(75000)
      fitCost(70000, 60000) should be(60000)
      fitCost(70000, 60000) should be(60000)
    }

    "be correct for costMinimum" in {
      fitCost(0, callsRevenueConstraints.costMinimum) should
        be(callsRevenueConstraints.costMinimum)
      fitCost(callsRevenueConstraints.costMinimum - 1, callsRevenueConstraints.costMinimum) should
        be(callsRevenueConstraints.costMinimum)
      fitCost(callsRevenueConstraints.costMinimum + 1, callsRevenueConstraints.costMaximum) should
        be(
          callsRevenueConstraints.costMinimum + 1 +
            callsRevenueConstraints.costStep
        )
    }
  }

  "callPayloadCorrection" should {
    def snapshot(cost: Funds, byRevenue: Boolean = true): CampaignSnapshot = {
      val s = CampaignSnapshotGen.next
      val product = Product(Placement(CostPerCall(FixPrice(cost))))
      val ts = if (byRevenue) {
        RealtyComponents.UseCallRevenueStartDateTesting.plusHours(Random.nextInt(100))
      } else {
        RealtyComponents.UseCallRevenueStartDateTesting.minusHours(Random.nextInt(100))
      }
      s.copy(product = product, time = ts)
    }

    val correction = (a: CampaignSnapshot, b: Funds) => RealtyComponents.callPayloadCorrection(a, b)

    "be correct for example 1" in {
      correction(snapshot(5000), 4000) shouldBe 4000
      correction(snapshot(5000, byRevenue = false), 4000) shouldBe 5000
    }

    "be correct for example 2" in {
      correction(snapshot(1000), 4000) shouldBe 1000
      correction(snapshot(1000, byRevenue = false), 4000) shouldBe 1000
    }

  }

}
