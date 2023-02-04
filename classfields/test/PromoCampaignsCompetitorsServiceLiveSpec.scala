package vsmoney.auction_auto_strategy.services.test

import auto.searcher.auction.used_car_auction.{AuctionOffer, AuctionState => HistogramEntry}
import cats.data.NonEmptyList
import common.models.finance.Money.Kopecks
import common.zio.logging.Logging
import infra.feature_toggles.client.testkit.TestFeatureToggles
import ru.auto.api.search.search_model.SearchRequestParameters
import ru.auto.dealer_calls_auction.proto.promo_campaign_service.DaysNumberFilters
import vsmoney.auction_auto_strategy.clients.domain.PromoCampaignOffersAndHistogram
import vsmoney.auction_auto_strategy.model.auction.CriterionKey._
import vsmoney.auction_auto_strategy.model.auction._
import vsmoney.auction_auto_strategy.model.common.ProductId.CallCarsUsed
import vsmoney.auction_auto_strategy.model.common.Project.Autoru
import vsmoney.auction_auto_strategy.model.common.UserId
import vsmoney.auction_auto_strategy.model.promo_campaign.BiddingAlgorithm.MaximumPositionForPrice
import vsmoney.auction_auto_strategy.model.promo_campaign.CompetitionDomain.OfferFilterDomain
import vsmoney.auction_auto_strategy.model.promo_campaign.Competitor.{ListingEntry, PromoCampaignCompetitor}
import vsmoney.auction_auto_strategy.model.promo_campaign.Status.Active
import vsmoney.auction_auto_strategy.model.promo_campaign.{
  PromoCampaign,
  PromoCampaignId,
  PromoCampaignSettings,
  PromoCampaignsCompetitorsGroup
}
import vsmoney.auction_auto_strategy.services.PromoCampaignsCompetitorsService
import vsmoney.auction_auto_strategy.services.impl.PromoCampaignsCompetitorsServiceLive
import vsmoney.auction_auto_strategy.services.testkit.{AuctionServiceMock, DealerCallsAuctionClientMock}
import zio.ZIO
import zio.clock.Clock
import zio.test.Assertion.equalTo
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation.value
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import java.time.{LocalDate, LocalDateTime}

object PromoCampaignsCompetitorsServiceLiveSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("PromoCampaignsCompetitorsService.getCompetitorsGroups")(
      testM("should work successfully with 1 promo campaign") {
        val dealerCallsAuctionClientMock =
          DealerCallsAuctionClientMock.GetFilteredOffersAndHistogram(
            equalTo(promoCampaign),
            value(offersAndHistograms)
          )
        val auctionServiceMock = AuctionServiceMock.GetSettings(
          equalTo(NonEmptyList.one(auctionKeyWithSuperGenId)),
          value(Map(auctionKeyWithSuperGenId -> auctionSettings))
        )

        val expected = List(
          PromoCampaignsCompetitorsGroup(
            auctionKeyWithSuperGenId,
            auctionSettings,
            List(
              ListingEntry(
                auctionKeyWithSuperGenId,
                Criterion(offerIdKey, CriterionValue(offerId)),
                Kopecks(callPriceKopecks),
                ctr
              )
            ),
            List(
              PromoCampaignCompetitor(
                auctionKeyWithSuperGenId,
                Criterion(offerIdKey, CriterionValue(offerId)),
                ctr,
                promoCampaign
              )
            )
          )
        )

        val res = for {
          featureTogglesClient <- ZIO.service[TestFeatureToggles.Service]
          _ = featureTogglesClient.set("auction_autostrategy_cars_used_super_gen_id_ignore", false)
          service <- ZIO.service[PromoCampaignsCompetitorsService]
          competitorsGroup <- service.getCompetitorsGroups(promoCampaigns)
        } yield competitorsGroup

        assertM(res)(equalTo(expected)).provideCustomLayer(
          featureTogglesMock ++ dealerCallsAuctionClientMock ++ auctionServiceMock >+> PromoCampaignsCompetitorsServiceLive.live
        )
      },
      testM("should ignore super gen id when feature enabled") {
        val dealerCallsAuctionClientMock =
          DealerCallsAuctionClientMock.GetFilteredOffersAndHistogram(
            equalTo(promoCampaign),
            value(offersAndHistograms)
          )
        val auctionServiceMock = AuctionServiceMock.GetSettings(
          equalTo(NonEmptyList.one(auctionKey)),
          value(auctionSettingsMap)
        )

        val expected = List(
          PromoCampaignsCompetitorsGroup(
            auctionKey,
            auctionSettings,
            List(
              ListingEntry(
                auctionKeyWithSuperGenId,
                Criterion(offerIdKey, CriterionValue(offerId)),
                Kopecks(callPriceKopecks),
                ctr
              )
            ),
            List(
              PromoCampaignCompetitor(
                auctionKeyWithSuperGenId,
                Criterion(offerIdKey, CriterionValue(offerId)),
                ctr,
                promoCampaign
              )
            )
          )
        )

        val res = for {
          featureTogglesClient <- ZIO.service[TestFeatureToggles.Service]
          _ = featureTogglesClient.set("auction_autostrategy_cars_used_super_gen_id_ignore", true)
          service <- ZIO.service[PromoCampaignsCompetitorsService]
          competitorsGroup <- service.getCompetitorsGroups(promoCampaigns)
        } yield competitorsGroup

        assertM(res)(equalTo(expected)).provideCustomLayer(
          featureTogglesMock ++ dealerCallsAuctionClientMock ++ auctionServiceMock >+> PromoCampaignsCompetitorsServiceLive.live
        )
      }
    )

  private val featureTogglesMock = (Logging.live ++ Clock.live) >>> TestFeatureToggles.live

  private val userId = UserId("1")
  private val regionValue = CriterionValue("1")
  private val markValue = CriterionValue("BMW")
  private val modelValue = CriterionValue("X5")
  private val generationValue = CriterionValue("12345")
  private val step = Kopecks(5000)
  private val minBid = Kopecks(30000)
  private val maxBid = Kopecks(100000)
  private val ctr = 2.5
  private val offerId = "offer1"
  private val callPriceKopecks = 40000

  private val auctionKey =
    AuctionKey(
      Autoru,
      CallCarsUsed,
      CriteriaContext(
        Seq(
          Criterion(regionKey, regionValue),
          Criterion(markKey, markValue),
          Criterion(modelKey, modelValue),
          Criterion(generationKey, CriterionValue("*"))
        )
      )
    )

  private val auctionKeyWithSuperGenId =
    AuctionKey(
      Autoru,
      CallCarsUsed,
      CriteriaContext(
        Seq(
          Criterion(regionKey, regionValue),
          Criterion(markKey, markValue),
          Criterion(modelKey, modelValue),
          Criterion(generationKey, generationValue)
        )
      )
    )

  private val auctionSettings = AuctionSettings(step, minBid)

  private val promoCampaign = PromoCampaign(
    PromoCampaignId(userId, 1L),
    PromoCampaignSettings(
      CallCarsUsed,
      userId,
      "test",
      LocalDate.now(),
      LocalDate.now().plusDays(1L),
      OfferFilterDomain(SearchRequestParameters.defaultInstance, 1L, DaysNumberFilters.defaultInstance),
      MaximumPositionForPrice(Bid(maxBid)),
      Active,
      "test promo campaign"
    ),
    LocalDateTime.now()
  )

  private val promoCampaigns = List(promoCampaign)

  private val offersAndHistograms = List(
    PromoCampaignOffersAndHistogram(
      auctionKeyWithSuperGenId,
      Seq(HistogramEntry(1, callPriceKopecks, ctr, offerId)),
      Seq(AuctionOffer(offerId, ctr)),
      promoCampaign
    )
  )

  private val auctionSettingsMap: Map[AuctionKey, AuctionSettings] = Map(
    auctionKey -> auctionSettings
  )
}
