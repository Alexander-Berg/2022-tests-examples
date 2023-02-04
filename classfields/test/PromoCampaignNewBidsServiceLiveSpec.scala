package vsmoney.auction_auto_strategy.services.test

import common.models.finance.Money._
import common.zio.logging.Logging
import common.zio.ops.prometheus.Prometheus
import ru.auto.api.search.search_model._
import ru.auto.dealer_calls_auction.proto.promo_campaign_service.DaysNumberFilters
import vsmoney.auction_auto_strategy.model.auction._
import vsmoney.auction_auto_strategy.model.common.{ProductId, Project, UserId}
import vsmoney.auction_auto_strategy.model.promo_campaign._
import vsmoney.auction_auto_strategy.services.PromoCampaignNewBidsService
import vsmoney.auction_auto_strategy.services.impl.PromoCampaignNewBidsServiceLive
import zio._
import zio.test.Assertion._
import zio.test._
import zio.test.environment._

import java.time.{LocalDate, LocalDateTime}

object PromoCampaignNewBidsServiceLiveSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("PromoCampaignNewBidsService.getNewBids")(
      testM("should respond with bigger bid if object was found in 2 PromoCampaign") {
        val group = PromoCampaignsCompetitorsGroup(
          key = key,
          settings = settings,
          listing = listing,
          competitors = List(
            buildCompetitor("4", "1", 1, 180, 0.5f),
            buildCompetitor("4", "1", 2, 150, 0.5f)
          )
        )

        for {
          service <- ZIO.service[PromoCampaignNewBidsService]
          newBids <- service.getNewBids(group)
        } yield {
          assertTrue(newBids.length == 1) &&
          assertTrue(newBids.head.bid == Rubles(160).asKopecks)
          assertTrue(newBids.head.promoCampaignId.userCampaignId == 1)
        }
      },
      testM("should respond with equal bids if PromoCampaign contains 2 objects") {
        val group = PromoCampaignsCompetitorsGroup(
          key = key,
          settings = settings,
          listing = listing,
          competitors = List(
            buildCompetitor("4", "1", 1, 180, 0.5f),
            buildCompetitor("5", "1", 1, 180, 0.5f)
          )
        )

        for {
          service <- ZIO.service[PromoCampaignNewBidsService]
          newBids <- service.getNewBids(group)
        } yield {
          assertTrue(newBids.length == 2) &&
          assertTrue(newBids.forall(_.bid == Rubles(160).asKopecks))
        }
      },
      testM("should rewrite any other bid with bid from PromoCampaign") {
        def test(currentObjectBid: Int, maxPromoCampaignBid: Int, expectedBid: Int) = {
          val group = PromoCampaignsCompetitorsGroup(
            key = key,
            settings = settings,
            listing = Competitor.ListingEntry(
              key,
              buildAuctionObject("4"),
              Rubles(currentObjectBid).asKopecks,
              0.5f
            ) :: listing,
            competitors = List(buildCompetitor("4", "1", 1, maxPromoCampaignBid, 0.5f))
          )

          for {
            service <- ZIO.service[PromoCampaignNewBidsService]
            newBids <- service.getNewBids(group)
          } yield {
            assertTrue(newBids.length == 1) &&
            assertTrue(newBids.headOption.exists(_.bid == Rubles(expectedBid).asKopecks))
          }
        }

        for {
          a1 <- test(200, 200, 160)
          a2 <- test(100, 200, 160)
          a3 <- test(210, 200, 160)
        } yield a1 && a2 && a3
      },
      testM("should respond with equal bids if 2 different users can take first place") {
        val group = PromoCampaignsCompetitorsGroup(
          key = key,
          settings = settings,
          listing = listing,
          competitors = List(
            buildCompetitor("4", "1", 1, 220, 0.8f),
            buildCompetitor("5", "2", 1, 220, 0.8f),
            buildCompetitor("6", "3", 1, 100, 0.3f),
            buildCompetitor("7", "4", 1, 170, 0.7f)
          )
        )

        for {
          service <- ZIO.service[PromoCampaignNewBidsService]
          newBids <- service.getNewBids(group)
        } yield {
          val actual = newBids.map(bid => bid.auctionObject.value.value -> bid.bid.asRoubles.value)
          val expected = Seq("4" -> 210, "5" -> 210, "6" -> 100, "7" -> 120)
          assert(actual)(hasSameElements(expected))
        }
      },
      testM("should respond with proper bid if there is only 1 entry in listing and only one competitor") {
        def test(currentObjectBid: Int, maxPromoCampaignBid: Int, expectedBid: Int) = {
          val group = PromoCampaignsCompetitorsGroup(
            key = key,
            settings = settings,
            listing =
              List(Competitor.ListingEntry(key, buildAuctionObject("1"), Rubles(currentObjectBid).asKopecks, 0.5f)),
            competitors = List(
              buildCompetitor("2", "1", 1, maxPromoCampaignBid, 0.5f)
            )
          )

          for {
            service <- ZIO.service[PromoCampaignNewBidsService]
            newBids <- service.getNewBids(group)
          } yield {
            assertTrue(newBids.length == 1) &&
            assertTrue(newBids.head.bid == Rubles(expectedBid).asKopecks)
          }
        }

        for {
          a1 <- test(200, 200, 200)
          a2 <- test(100, 200, 110)
          a3 <- test(210, 200, 100)
        } yield a1 && a2 && a3
      },
      testM("should respond with proper bids if there is only 1 entry in listing and there are many competitors") {
        val group = PromoCampaignsCompetitorsGroup(
          key = key,
          settings = settings,
          listing = List(Competitor.ListingEntry(key, buildAuctionObject("5"), Rubles(110).asKopecks, 0.5f)),
          competitors = List(
            buildCompetitor("1", "1", 1, 220, 0.8f),
            buildCompetitor("2", "2", 1, 220, 0.5f),
            buildCompetitor("3", "3", 1, 100, 0.3f),
            buildCompetitor("4", "4", 1, 150, 0.7f)
          )
        )

        for {
          service <- ZIO.service[PromoCampaignNewBidsService]
          newBids <- service.getNewBids(group)
        } yield {
          val actual = newBids.map(bid => bid.auctionObject.value.value -> bid.bid.asRoubles.value)
          val expected = Seq("1" -> 150, "2" -> 220, "3" -> 100, "4" -> 100)
          assert(actual)(hasSameElements(expected))
        }
      },
      testM(
        "should respond with proper bids if there is only 1 entry in listing and there are many competitors (another bids set)"
      ) {
        val group = PromoCampaignsCompetitorsGroup(
          key = key,
          settings = settings,
          listing = List(Competitor.ListingEntry(key, buildAuctionObject("5"), Rubles(200).asKopecks, 0.5f)),
          competitors = List(
            buildCompetitor("1", "1", 1, 240, 0.8f),
            buildCompetitor("2", "2", 1, 220, 0.5f),
            buildCompetitor("3", "3", 1, 100, 0.3f),
            buildCompetitor("4", "4", 1, 150, 0.6f)
          )
        )

        for {
          service <- ZIO.service[PromoCampaignNewBidsService]
          newBids <- service.getNewBids(group)
        } yield {
          val actual = newBids.map(bid => bid.auctionObject.value.value -> bid.bid.asRoubles.value)
          val expected = Seq("1" -> 150, "2" -> 210, "3" -> 100, "4" -> 100)
          assert(actual)(hasSameElements(expected))
        }
      },
      testM("should respond with min bid if listing is empty and there is only one competitor") {
        val group = PromoCampaignsCompetitorsGroup(
          key = key,
          settings = settings,
          listing = List.empty,
          competitors = List(
            buildCompetitor("4", "1", 1, 180, 0.5f)
          )
        )

        for {
          service <- ZIO.service[PromoCampaignNewBidsService]
          newBids <- service.getNewBids(group)
        } yield {
          assertTrue(newBids.length == 1) &&
          assertTrue(newBids.head.bid == Rubles(100).asKopecks)
        }
      },
      testM("should respond with proper bid if listing is empty and there are many competitors") {
        val group = PromoCampaignsCompetitorsGroup(
          key = key,
          settings = settings,
          listing = List.empty,
          competitors = List(
            buildCompetitor("1", "1", 1, 220, 0.8f),
            buildCompetitor("2", "2", 1, 220, 0.5f),
            buildCompetitor("3", "3", 1, 100, 0.3f),
            buildCompetitor("4", "4", 1, 150, 0.7f)
          )
        )

        for {
          service <- ZIO.service[PromoCampaignNewBidsService]
          newBids <- service.getNewBids(group)
        } yield {
          val actual = newBids.map(bid => bid.auctionObject.value.value -> bid.bid.asRoubles.value)
          val expected = Seq("1" -> 150, "2" -> 220, "3" -> 100, "4" -> 100)
          assert(actual)(hasSameElements(expected))
        }
      },
      testM("should ignore bid if min bid > max bid") {
        val group = PromoCampaignsCompetitorsGroup(
          key = key,
          settings = settings,
          listing = listing,
          competitors = List(buildCompetitor("4", "1", 1, 10, 0.5f))
        )

        for {
          service <- ZIO.service[PromoCampaignNewBidsService]
          newBids <- service.getNewBids(group)
        } yield assertTrue(newBids.isEmpty)
      },
      testM("should respond with max bid if bid > max_bid") {
        val group = PromoCampaignsCompetitorsGroup(
          key = key,
          settings = settings,
          listing = List(Competitor.ListingEntry(key, buildAuctionObject("3"), Rubles(95).asKopecks, 1f)),
          competitors = List(
            buildCompetitor("1", "1", 1, 100, 1f),
            buildCompetitor("2", "2", 1, 150, 0.7f)
          )
        )

        for {
          service <- ZIO.service[PromoCampaignNewBidsService]
          newBids <- service.getNewBids(group)
        } yield {
          val actual = newBids.map(bid => bid.auctionObject.value.value -> bid.bid.asRoubles.value)
          val expected = Seq("2" -> 150, "1" -> 100)
          assert(actual)(hasSameElements(expected))
        }
      },
      testM("should respond with bid always >= than min bid") {
        val group = PromoCampaignsCompetitorsGroup(
          key = key,
          settings = settings,
          listing = List(Competitor.ListingEntry(key, buildAuctionObject("3"), Rubles(80).asKopecks, 0.5f)),
          competitors = List(
            buildCompetitor("1", "1", 1, 110, 0.2f),
            buildCompetitor("2", "2", 1, 150, 0.7f)
          )
        )

        for {
          service <- ZIO.service[PromoCampaignNewBidsService]
          newBids <- service.getNewBids(group)
        } yield {
          val actual = newBids.map(bid => bid.auctionObject.value.value -> bid.bid.asRoubles.value)
          val expected = Seq("2" -> 100, "1" -> 100)
          assert(actual)(hasSameElements(expected))
        }
      },
      testM("should respond with min bid if current object has the lowest position") {
        val group = PromoCampaignsCompetitorsGroup(
          key = key,
          settings = settings,
          listing = List(Competitor.ListingEntry(key, buildAuctionObject("3"), Rubles(90).asKopecks, 0.5f)),
          competitors = List(
            buildCompetitor("1", "1", 1, 140, 0.6f),
            buildCompetitor("2", "2", 1, 210, 0.7f),
            buildCompetitor("4", "3", 1, 140, 0.5f),
            buildCompetitor("5", "4", 1, 120, 0.3f)
          )
        )

        for {
          service <- ZIO.service[PromoCampaignNewBidsService]
          newBids <- service.getNewBids(group)
        } yield {
          val actual = newBids.map(bid => bid.auctionObject.value.value -> bid.bid.asRoubles.value)
          val expected = Seq("2" -> 140, "1" -> 130, "4" -> 100, "5" -> 100)
          assert(actual)(hasSameElements(expected))
        }
      }
    ).provideCustomLayer(
      Logging.live ++ Prometheus.live >>> PromoCampaignNewBidsServiceLive.live
    )

  private def buildCompetitor(
      obj: String,
      userId: String,
      userCampaignId: Long,
      maxBid: Int,
      ctr: Double): Competitor.PromoCampaignCompetitor =
    Competitor.PromoCampaignCompetitor(
      key,
      buildAuctionObject(obj),
      ctr,
      buildPromoCampaign(PromoCampaignId(UserId(userId), userCampaignId), Rubles(maxBid))
    )

  private def buildPromoCampaign(promoCampaignId: PromoCampaignId, maxBid: Rubles): PromoCampaign =
    PromoCampaign(
      promoCampaignId,
      PromoCampaignSettings(
        product = ProductId.CallCarsUsed,
        userId = promoCampaignId.userId,
        name = "Супер кампания",
        dateFrom = LocalDate.now().minusDays(10),
        dateTo = LocalDate.now(),
        competitionDomain = CompetitionDomain.OfferFilterDomain(
          searcherFilters = SearchRequestParameters(),
          maxOfferDailyCalls = 10,
          daysNumberFilters = DaysNumberFilters(
            daysOnStockFrom = Some(1),
            daysOnStockTo = None,
            daysWithoutCallsFrom = None,
            daysWithoutCallsTo = Some(10)
          )
        ),
        biddingAlgorithm = BiddingAlgorithm.MaximumPositionForPrice(Bid(maxBid.asKopecks)),
        status = Status.Active,
        description = ""
      ),
      LocalDateTime.now()
    )

  private def buildAuctionObject(auctionObject: String): Criterion =
    Criterion(CriterionKey("offerId"), CriterionValue(auctionObject))

  private val context = CriteriaContext(
    Seq(
      Criterion(key = CriterionKey("region_id"), value = CriterionValue("42")),
      Criterion(key = CriterionKey("mark"), value = CriterionValue("bmw")),
      Criterion(key = CriterionKey("model"), value = CriterionValue("x5")),
      Criterion(key = CriterionKey("super_gen_id"), value = CriterionValue("1"))
    )
  )

  private val key = AuctionKey(
    project = Project.Autoru,
    product = ProductId.CallCarsUsed,
    context = context
  )

  private val settings = AuctionSettings(step = Rubles(10).asKopecks, minBid = Rubles(100).asKopecks)

  private val listing = List(
    Competitor.ListingEntry(key, buildAuctionObject("1"), Rubles(200).asKopecks, 0.8f),
    Competitor.ListingEntry(key, buildAuctionObject("2"), Rubles(150).asKopecks, 0.5f),
    Competitor.ListingEntry(key, buildAuctionObject("3"), Rubles(100).asKopecks, 0.6f)
  )

}
