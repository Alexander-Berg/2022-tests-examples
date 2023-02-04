package ru.yandex.vertis.billing.settings

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.gens.{CampaignSnapshotGen, Producer}
import ru.yandex.vertis.billing.model_core.{CostPerCall, FixPrice, Placement, Product}

import scala.util.Random

/**
  * @author ruslansd
  */
class RealtyFirstPriceAuctionSpec extends AnyWordSpec with Matchers {

  private val FirstPriceAuctionEnd = RealtyComponents.UseCallRevenueStartDateTesting.minus(1)

  private def firstPriceAuctionDate = FirstPriceAuctionEnd.minusHours(Random.nextInt(100))
  private def revenueAuctionDate = FirstPriceAuctionEnd.plusHours(Random.nextInt(100))

  private def expected(actualCost: Long) =
    RealtyComponents.callsRevenueConstraints.get.fitCost(actualCost, callBid.units)

  private val callBid = FixPrice(100000L)
  private val product = Product(Placement(CostPerCall(callBid)))

  "Realty" should {
    "bil first price auction after start" in {
      val snapshot = CampaignSnapshotGen.next
        .copy(time = firstPriceAuctionDate, product = product)
      RealtyComponents.callPayloadCorrection(snapshot, 50000) shouldBe callBid.units
      RealtyComponents.callPayloadCorrection(snapshot, 0) shouldBe callBid.units
      RealtyComponents.callPayloadCorrection(snapshot, 500000) shouldBe callBid.units
    }

    "bil first price auction in start" in {
      val snapshot = CampaignSnapshotGen.next
        .copy(time = FirstPriceAuctionEnd, product = product)
      RealtyComponents.callPayloadCorrection(snapshot, 50000) shouldBe callBid.units
      RealtyComponents.callPayloadCorrection(snapshot, 0) shouldBe callBid.units
      RealtyComponents.callPayloadCorrection(snapshot, 500000) shouldBe callBid.units
    }
  }

}
