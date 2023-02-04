package auto.dealers.dealer_calls_auction.logic.test.offers.promo_campaign

import auto.common.clients.promoCampaign.PromoCampaignServiceClient
import auto.common.clients.promoCampaign.testkit.PromoCampaignServiceClientMock
import auto.dealers.dealer_calls_auction.logic.offers.auction.OfferAuctionManager
import auto.dealers.dealer_calls_auction.logic.offers.promo_campaign.{
  PromoCampaignOffersManager,
  PromoCampaignOffersManagerLive
}
import auto.dealers.dealer_calls_auction.logic.test.CallsAuctionTestCommon
import auto.dealers.dealer_calls_auction.logic.testkit.common.ClockDependentMocks.{
  clockMock,
  dealerZoneOffsetManagerMock
}
import auto.dealers.dealer_calls_auction.logic.testkit.common.{CallStatsRepositoryMock, MarketStateRepositoryMock}
import auto.dealers.dealer_calls_auction.logic.testkit.market_indicator.MarketIndicatorServiceMock
import auto.dealers.dealer_calls_auction.logic.testkit.offers.OfferAuctionManagerMock
import auto.dealers.dealer_calls_auction.model.filter.{MarketSegmentsFilter, OffersFilter}
import auto.dealers.dealer_calls_auction.storage.calls.CallStatsRepository.OfferCallsCount
import auto.dealers.dealer_calls_auction.storage.marketstate.MarketStateRepository
import auto.searcher.auction.used_car_auction.{AuctionOffer, AuctionState}
import common.autoru.clients.public_api.testkit.PublicApiClientMock
import common.zio.logging.Logging
import ru.auto.api.api_offer_model.Offer
import ru.auto.api.request_model.RequestPagination
import ru.auto.api.response_model.OfferListingResponse
import ru.auto.dealer_calls_auction.proto.api_model.AuctionCurrentState
import ru.auto.dealer_calls_auction.proto.api_model.AuctionCurrentState.CurrentState
import ru.auto.dealer_calls_auction.proto.offer_auction_service.AuctionContext
import ru.auto.dealer_calls_auction.proto.promo_campaign_service.{
  DaysNumberFilters,
  GetFilteredOffersAndHistogramResponseEntry
}
import vsmoney.auction.auction_bids.AuctionContextState
import vsmoney.auction.common_model.{CriteriaValue, Money}
import vsmoney.auction_auto_strategy.promo_campaign_service.PredictBidsResponse
import vsmoney.auction_auto_strategy.promo_campaign_service.PredictBidsResponse.BidPrediction
import zio.{Has, ULayer, ZIO}
import zio.test._
import zio.test.Assertion._
import zio.test.mock.Expectation.value

object PromoCampaignOffersManagerSpec extends DefaultRunnableSpec with CallsAuctionTestCommon {

  private val extendedContextsToState = extendedContextsWithoutDealer.map(_.copy(dealerId = rawDealerId.toLong))

  private val marketStateRepositoryContextsMock = MarketStateRepositoryMock.ContextsByFilter(
    hasField("offerId", _.offerId, hasSameElements(0.to(5).map(num => s"autoru-$num"))),
    value(expectedAuctionContexts)
  ) ++ MarketStateRepositoryMock.StateByContexts(
    hasSameElements(extendedContextsToState),
    value(expectedStateByContexts)
  )

  private val marketStateRepositoryListingMock = MarketStateRepositoryMock.PlainListing(
    equalTo((extendedSearchParams, RequestPagination.defaultInstance)),
    value(
      OfferListingResponse(
        offers = (kiaRioOffers ++ bmwOffers).map(auctionOffer => Offer(id = auctionOffer.offerId)),
        searchParameters = Some(extendedSearchParams)
      )
    )
  )

  private val callStatsRepositoryMock =
    CallStatsRepositoryMock.GetRelevantCallsCountByOffer(
      anything,
      value(
        Seq(
          OfferCallsCount("1-rio", 4),
          OfferCallsCount("2-rio", 0),
          OfferCallsCount("3-rio", 3),
          OfferCallsCount("4-rio", 0),
          OfferCallsCount("5-rio", 10),
          OfferCallsCount("0-bmw", 6)
        )
      )
    )

  private val rawAuctionState = toDummyRawAuctionState(extendedContextsToState)

  private val customCurrentBids = (kiaRioOffers ++ bmwOffers).map(offer => offer.offerId -> 100L).toMap

  private val enrichedAuctionState = rawAuctionState.map { case (context, state) =>
    val currentBid = customCurrentBids.getOrElse(context.offerId, 100L)
    context -> AuctionCurrentState(
      currentState = CurrentState(
        currentBid = currentBid,
        basePrice = state.basePrice.map(_.kopecks).getOrElse(0L),
        minBid = state.minBid.map(_.kopecks).getOrElse(0L),
        maxBid = 10000L,
        oneStep = state.oneStep.map(_.kopecks).getOrElse(0L),
        limitExceeded = false,
        inProgress = false
      ),
      segments = List.empty
    )
  }

  private type ContextStates = Map[AuctionContext, AuctionContextState]
  private type Bids = Map[String, Long]

  private val bidPrediction = (kiaRioOffers ++ bmwOffers).map(y =>
    BidPrediction(Some(CriteriaValue(key = "offer_id", value = y.offerId)), Some(Money(100)))
  )
  private val predictAnswer = PredictBidsResponse(bidPrediction)

  private val promoCampaignClientMocked =
    PromoCampaignServiceClientMock.Predict(
      anything,
      value(
        predictAnswer
      )
    )

  private val offerAuctionManagerRawStateMock = OfferAuctionManagerMock.RawAuctionStateBatch(
    hasSameElements(extendedContextsToState),
    value(rawAuctionState)
  ) ++ OfferAuctionManagerMock.EnrichedAuctionState(
    hasField[(ContextStates, Bids), ContextStates]("raw", _._1, hasSameElements(rawAuctionState)) &&
      hasField[(ContextStates, Bids), Bids]("bids", _._2, hasSameElements(customCurrentBids)),
    value(enrichedAuctionState)
  )

  private def createTestLayer(
      marketStateMock: ULayer[Has[MarketStateRepository]],
      offerAuctionManagerMock: ULayer[Has[OfferAuctionManager]],
      promoCampaignServiceClientMock: ULayer[Has[PromoCampaignServiceClient]]) =
    clockMock ++
      Logging.live ++
      PublicApiClientMock.empty ++
      dealerZoneOffsetManagerMock(dealerClientId) ++
      MarketIndicatorServiceMock.empty ++
      callStatsRepositoryMock ++
      marketStateMock ++
      promoCampaignServiceClientMock ++
      offerAuctionManagerMock >>> PromoCampaignOffersManagerLive.live

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PromoCampaignOffersManager")(
      testM("should return filtered and enriched offers") {
        val expectedResultParams = Some(extendedSearchParams)
        val expectedResultOfferIds = Seq(kiaRioOffers.map(_.offerId) :+ "0-bmw").flatten
        val callsLimitExceedResults = 1.to(4).map(_ => false) ++ 1.to(2).map(_ => true)

        for {
          promoCampaignOffersManager <- ZIO.service[PromoCampaignOffersManager]
          predictionResult <- promoCampaignOffersManager.getFilteredAndEnrichedOffers(
            pagination = RequestPagination.defaultInstance,
            filter = OffersFilter(
              searchRequest = rawSearchParams,
              dealerId = dealerClientId,
              promoCampaignLastChanges = None,
              maxOfferDailyCalls = Some(6),
              maxBid = Some(100),
              availableMarketSegments = MarketSegmentsFilter.All,
              daysNumberParams = DaysNumberFilters.defaultInstance
            )
          )
          resultOffers = predictionResult.offers
        } yield assertTrue(predictionResult.searchParameters == expectedResultParams) &&
          assert(resultOffers.map(_.id))(hasSameElements(expectedResultOfferIds)) &&
          assert(resultOffers.flatMap(_.promoCampaignListingFields).map(_.callsLimitExceeded))(
            hasSameElements(callsLimitExceedResults)
          )
      }.provideLayer(
        createTestLayer(
          marketStateRepositoryListingMock ++ marketStateRepositoryContextsMock,
          offerAuctionManagerRawStateMock,
          promoCampaignClientMocked
        )
      ),
      testM("should return filtered offers and histogram") {
        val expectedResult = List(
          GetFilteredOffersAndHistogramResponseEntry(
            context = Some(AuctionContext(regionId = 123, markCode = "KIA", modelCode = "RIO", superGenId = 456)),
            histogram = kiaRioHistogram,
            offers = kiaRioOffers
          ),
          GetFilteredOffersAndHistogramResponseEntry(
            context = Some(AuctionContext(regionId = 123, markCode = "BMW", superGenId = 645)),
            histogram = List(AuctionState(offerId = "0-bmw", callPriceKopecks = 5000, ctr = 1.0, position = 1)),
            offers = List(AuctionOffer(offerId = "0-bmw", ctr = 1.0))
          )
        )

        for {
          promoCampaignOffersManager <- ZIO.service[PromoCampaignOffersManager]
          predictionResult <- promoCampaignOffersManager.getFilteredOffersAndHistogram(
            searchRequest = rawSearchParams,
            dealerId = dealerClientId,
            promoCampaignLastChanges = None,
            maxOfferDailyCalls = None,
            daysNumberParams = DaysNumberFilters.defaultInstance
          )
        } yield assert(predictionResult)(hasSameElements(expectedResult))
      }.provideLayer(
        createTestLayer(marketStateRepositoryMock, OfferAuctionManagerMock.empty, PromoCampaignServiceClientMock.empty)
      )
    )
  }
}
