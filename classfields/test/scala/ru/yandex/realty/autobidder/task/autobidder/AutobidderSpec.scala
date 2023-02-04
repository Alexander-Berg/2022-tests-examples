package ru.yandex.realty.autobidder.task.autobidder

import java.util.UUID

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.autobidder.model.Strategy
import ru.yandex.realty.autobidder.task.autobidder.Campaign.Autobidder.State
import ru.yandex.realty.model.util.AuctionCommon
import ru.yandex.realty.model.util.AuctionCommon._

import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`

/**
  * @author rmuzhikov
  */
@RunWith(classOf[JUnitRunner])
class AutobidderSpec extends SpecBase with PropertyChecks with ModelsGen {

  private val BID_MIN = 60000
  private val STEP = 5000

  private def listingPlace(place: Int): Strategy =
    Strategy.newBuilder().setListingPlace(Strategy.ListingPlace.newBuilder().setPlace(place)).build()

  private val premium: Strategy =
    Strategy.newBuilder().setPremium(Strategy.Premium.newBuilder()).build()

  private val baseCampaign = Campaign(
    UUID.randomUUID().toString,
    1,
    1,
    1,
    1,
    BID_MIN,
    BID_MIN,
    STEP,
    BID_MIN,
    hasSuperCall = false,
    isWorkingNow = true,
    Some(
      Campaign.Autobidder(
        Campaign.Autobidder.Config(
          1,
          1,
          Strategy.newBuilder().setCardWinner(Strategy.CardWinner.newBuilder()).build(),
          Campaign.Autobidder.State.Active.status
        ),
        None,
        Campaign.Autobidder.State.Active
      )
    )
  )

  private def changeBidLimit(ab: Campaign.Autobidder, bidLimit: Long): Campaign.Autobidder = {
    ab.copy(config = ab.config.copy(bidLimit = bidLimit))
  }

  private def changeStrategy(ab: Campaign.Autobidder, strategy: Strategy): Campaign.Autobidder = {
    ab.copy(config = ab.config.copy(strategy = strategy))
  }

  "Autobidder" should {
    "retain same campaign count" in {

      val siteIds = Seq(1, 2, 3, 4, 5)
      val siteCampaignsGen = Gen.sequence(siteIds.map(id => list(1, 10, campaignGen(siteId = Some(id)))))

      forAll(siteCampaignsGen) { siteCampaigns =>
        val input = siteCampaigns.flatten.toSeq
        val result = Autobidder.run(input)
        result.map(_.campaignId).toSet should be(input.map(_.campaignId).toSet)
      }
    }

    "make card auction" in {
      val c1 = baseCampaign.copy(
        campaignId = UUID.randomUUID().toString,
        companyId = 1,
        bid = BID_MIN + STEP * 3,
        balance = BID_MIN,
        autobidder = baseCampaign.autobidder.map(changeBidLimit(_, BID_MIN))
      )
      val c2 = baseCampaign.copy(
        campaignId = UUID.randomUUID().toString,
        companyId = 2,
        bid = BID_MIN + STEP * 2,
        balance = BID_MIN,
        autobidder = baseCampaign.autobidder.map(changeBidLimit(_, BID_MIN))
      )
      val c3 = baseCampaign.copy(
        campaignId = UUID.randomUUID().toString,
        companyId = 3,
        bid = BID_MIN + STEP,
        balance = BID_MIN * 2,
        autobidder = baseCampaign.autobidder.map(changeBidLimit(_, BID_MIN * 2))
      )
      val c4 = baseCampaign.copy(
        campaignId = UUID.randomUUID().toString,
        companyId = 4,
        bid = BID_MIN,
        balance = BID_MIN * 3,
        autobidder = baseCampaign.autobidder.map(changeBidLimit(_, BID_MIN * 3))
      )
      val c5 = baseCampaign.copy(
        campaignId = UUID.randomUUID().toString,
        companyId = 5,
        bid = BID_MIN,
        balance = BID_MIN * 2,
        autobidder = baseCampaign.autobidder.map(changeBidLimit(_, BID_MIN * 4))
      )
      val input = Seq(c1, c2, c3, c4, c5)
      val result = Autobidder.run(input)
      result.size should be(input.size)
      result.find(_.campaignId == c4.campaignId).flatMap(_.getBidUpdate) shouldBe (c3.getBidLimit.map(_ + STEP))
      result.find(_.campaignId == c4.campaignId).flatMap(_.getState) shouldBe Some(State.Active)

      result.find(_.campaignId == c3.campaignId).flatMap(_.getBidUpdate) shouldBe (c3.getBidLimit)
      result.find(_.campaignId == c3.campaignId).flatMap(_.getState) shouldBe Some(State.BidLimitReached(c4.campaignId))

      result.find(_.campaignId == c2.campaignId).flatMap(_.getBidUpdate) shouldBe None
      result.find(_.campaignId == c2.campaignId).flatMap(_.getState) shouldBe Some(State.BidLimitReached(c4.campaignId))

      result.find(_.campaignId == c1.campaignId).flatMap(_.getBidUpdate) shouldBe None
      result.find(_.campaignId == c1.campaignId).flatMap(_.getState) shouldBe Some(State.BidLimitReached(c4.campaignId))

      result.find(_.campaignId == c5.campaignId).flatMap(_.getBidUpdate) shouldBe Some(c5.balance)
      result.find(_.campaignId == c5.campaignId).flatMap(_.getState) shouldBe Some(
        State.BalanceLimitReached(c4.campaignId)
      )
    }

    "make premium auction" in {
      val c1 = baseCampaign.copy(
        campaignId = UUID.randomUUID().toString,
        siteId = 1,
        geoId = 1,
        companyId = 1,
        bid = BID_MIN + STEP * 3,
        balance = BID_MIN,
        autobidder = baseCampaign.autobidder.map(changeBidLimit(_, BID_MIN))
      )
      val c2 = baseCampaign.copy(
        campaignId = UUID.randomUUID().toString,
        siteId = 1,
        geoId = 1,
        companyId = 2,
        bid = BID_MIN + STEP * 2,
        balance = BID_MIN,
        autobidder = baseCampaign.autobidder.map(ab => changeStrategy(changeBidLimit(ab, BID_MIN), premium))
      )
      val c3 = baseCampaign.copy(
        campaignId = UUID.randomUUID().toString,
        siteId = 2,
        geoId = 2,
        companyId = 3,
        bid = BID_MIN + STEP,
        balance = BID_MIN * 2,
        autobidder = baseCampaign.autobidder.map(changeBidLimit(_, BID_MIN * 2))
      )
      val c4 = baseCampaign.copy(
        campaignId = UUID.randomUUID().toString,
        siteId = 2,
        geoId = 2,
        companyId = 4,
        bid = BID_MIN,
        balance = BID_MIN * 3,
        autobidder = baseCampaign.autobidder.map(ab => changeStrategy(changeBidLimit(ab, BID_MIN * 3), premium))
      )
      val c5 = baseCampaign.copy(
        campaignId = UUID.randomUUID().toString,
        siteId = 3,
        geoId = 2,
        companyId = 5,
        bid = BID_MIN,
        balance = BID_MIN * 4,
        autobidder = baseCampaign.autobidder.map(ab => changeStrategy(changeBidLimit(ab, BID_MIN * 4), premium))
      )
      val input = Seq(c1, c2, c3, c4, c5)
      val result = Autobidder.run(input)
      result.size should be(input.size)
      result.find(_.campaignId == c5.campaignId).flatMap(_.getBidUpdate) shouldBe (c4.getBidLimit.map(_ + STEP))
      result.find(_.campaignId == c5.campaignId).flatMap(_.getState) shouldBe Some(State.Active)

      result.find(_.campaignId == c4.campaignId).flatMap(_.getBidUpdate) shouldBe (c4.getBidLimit)
      result.find(_.campaignId == c4.campaignId).flatMap(_.getState) shouldBe Some(State.BidLimitReached(c5.campaignId))

      result.find(_.campaignId == c3.campaignId).flatMap(_.getBidUpdate) shouldBe (c3.getBidLimit)
      result.find(_.campaignId == c3.campaignId).flatMap(_.getState) shouldBe Some(State.BidLimitReached(c4.campaignId))

      result.find(_.campaignId == c2.campaignId).flatMap(_.getBidUpdate) shouldBe None
      result.find(_.campaignId == c1.campaignId).flatMap(_.getBidUpdate) shouldBe None
    }

    "make listing place auction for same place based on limit" in {
      val c1 = baseCampaign.copy(
        campaignId = UUID.randomUUID().toString,
        siteId = 1,
        companyId = 1,
        bid = BID_MIN,
        balance = BID_MIN,
        autobidder = baseCampaign.autobidder.map(ab => changeStrategy(changeBidLimit(ab, BID_MIN), listingPlace(1)))
      )
      val c2 = baseCampaign.copy(
        campaignId = UUID.randomUUID().toString,
        siteId = 1,
        companyId = 2,
        bid = BID_MIN,
        balance = BID_MIN * 2,
        autobidder = baseCampaign.autobidder.map(ab => changeStrategy(changeBidLimit(ab, BID_MIN * 2), listingPlace(1)))
      )
      val c3 = baseCampaign.copy(
        campaignId = UUID.randomUUID().toString,
        siteId = 2,
        companyId = 3,
        bid = BID_MIN,
        balance = BID_MIN * 3,
        autobidder = baseCampaign.autobidder.map(ab => changeStrategy(changeBidLimit(ab, BID_MIN * 3), listingPlace(1)))
      )
      val c4 = baseCampaign.copy(
        campaignId = UUID.randomUUID().toString,
        siteId = 3,
        ctr = 2,
        companyId = 4,
        bid = BID_MIN,
        balance = BID_MIN * 2,
        autobidder = baseCampaign.autobidder.map(ab => changeStrategy(changeBidLimit(ab, BID_MIN * 2), listingPlace(1)))
      )
      val c5 = baseCampaign.copy(
        campaignId = UUID.randomUUID().toString,
        siteId = 3,
        ctr = 2,
        companyId = 4,
        bid = BID_MIN,
        balance = BID_MIN + STEP,
        autobidder = baseCampaign.autobidder.map(ab => changeStrategy(changeBidLimit(ab, BID_MIN * 2), listingPlace(1)))
      )
      val input = Seq(c1, c2, c3, c4, c5)
      val result = Autobidder.run(input)
      result.size should be(input.size)
      result.sortBy(-_.getCurrentCPM).map(_.campaignId) shouldBe (Seq(c4, c3, c5, c2, c1).map(_.campaignId))

      result.find(_.campaignId == c4.campaignId).flatMap(_.getBidUpdate) shouldBe (Some(
        AuctionCommon.nextValue(c3.getMaxCPM / c4.ctr, c4.minimalBid, c4.bidStep)
      ))
      result.find(_.campaignId == c4.campaignId).flatMap(_.getState) shouldBe Some(State.Active)

      result.find(_.campaignId == c3.campaignId).flatMap(_.getBidUpdate) shouldBe (c3.getBidLimit)
      result.find(_.campaignId == c3.campaignId).flatMap(_.getState) shouldBe Some(State.BidLimitReached(c4.campaignId))

      result.find(_.campaignId == c2.campaignId).flatMap(_.getBidUpdate) shouldBe (c2.getBidLimit)
      result.find(_.campaignId == c2.campaignId).flatMap(_.getState) shouldBe Some(State.BidLimitReached(c4.campaignId))

      result.find(_.campaignId == c1.campaignId).flatMap(_.getBidUpdate) shouldBe None
      result.find(_.campaignId == c1.campaignId).flatMap(_.getState) shouldBe Some(State.BidLimitReached(c2.campaignId))

      result.find(_.campaignId == c5.campaignId).flatMap(_.getBidUpdate) shouldBe Some(c5.balance)
      result.find(_.campaignId == c5.campaignId).flatMap(_.getState) shouldBe Some(
        State.BalanceLimitReached(c4.campaignId)
      )
    }
  }
}
