package auto.dealers.dealer_calls_auction.logic.test

import auto.common.clients.promoCampaign.testkit.PromoCampaignServiceClientMock
import auto.common.model.ClientId
import auto.dealers.dealer_calls_auction.logic.testkit.common.MarketStateRepositoryMock
import auto.dealers.dealer_calls_auction.logic.AuctionContextConvertor.NarrowExpandedContext
import auto.dealers.dealer_calls_auction.storage.marketstate.MarketStateRepository
import auto.searcher.auction.used_car_auction.{
  AuctionOffer,
  AuctionState,
  AuctionStateContext,
  AuctionStateContextExpanded,
  AuctionStateResponse
}
import ru.auto.api.api_offer_model.{Category, Section}
import ru.auto.api.search.search_model.State.USED
import ru.auto.api.search.search_model.{CatalogFilter, SearchRequestParameters}
import ru.auto.dealer_calls_auction.proto.offer_auction_service.AuctionContext
import vsmoney.auction.auction_bids.AuctionContextState
import vsmoney.auction.common_model.{CriteriaValue, Money, Project}
import vsmoney.auction_auto_strategy.promo_campaign_service.PredictBidsResponse
import zio.Has
import zio.test.Assertion.{anything, equalTo, hasSameElements}
import zio.test.mock.Expectation
import zio.test.mock.Expectation.value

trait CallsAuctionTestCommon {

  val dealerClientId: ClientId = ClientId(20101L)
  val rawDealerId: String = dealerClientId.value.toString

  val rawSearchParams: SearchRequestParameters = SearchRequestParameters(
    priceFrom = Some(100000),
    priceTo = Some(2000000),
    yearFrom = Some(2010),
    yearTo = Some(2021),
    catalogFilter = Seq(
      CatalogFilter(mark = Some("KIA"), model = Some("RIO")),
      CatalogFilter(mark = Some("BMW"))
    )
  )

  val extendedSearchParams: SearchRequestParameters =
    rawSearchParams.copy(state = Seq(USED), clientId = Some(rawDealerId), multipostingHidden = Some(true))

  val kiaRioOffers: List[AuctionOffer] = List(
    AuctionOffer(offerId = "1-rio", ctr = 1.0),
    AuctionOffer(offerId = "2-rio", ctr = 0.1),
    AuctionOffer(offerId = "3-rio", ctr = 0.5),
    AuctionOffer(offerId = "4-rio", ctr = 1.0),
    AuctionOffer(offerId = "5-rio", ctr = 0.8)
  )

  val bmwOffers: List[AuctionOffer] = List(AuctionOffer(offerId = "0-bmw", ctr = 1.0))

  val expectedAuctionContexts: List[AuctionStateContextExpanded] = List(
    AuctionStateContextExpanded(
      context = Some(AuctionStateContext(rid = 123, markCode = "KIA", modelCode = "RIO", superGenId = 456L)),
      offers = kiaRioOffers
    ),
    AuctionStateContextExpanded(
      context = Some(AuctionStateContext(rid = 123, markCode = "BMW", modelCode = "", superGenId = 645L)),
      offers = bmwOffers
    )
  )

  val kiaRioHistogram: List[AuctionState] = kiaRioOffers.zipWithIndex.map { case (AuctionOffer(offerId, _, _), index) =>
    AuctionState(offerId = offerId, callPriceKopecks = 5000, ctr = 1.0 - 0.05 * index, position = index + 1)
  }

  val expectedStateByContexts: List[AuctionStateResponse] = List(
    AuctionStateResponse(
      histogram = kiaRioHistogram,
      current = Some(AuctionState(ctr = 1.0))
    ),
    AuctionStateResponse(
      histogram = Seq(AuctionState(offerId = "0-bmw", callPriceKopecks = 5000, ctr = 1.0, position = 1)),
      current = Some(AuctionState(ctr = 1.0))
    )
  )

  val marketStateRepositoryMock: Expectation[Has[MarketStateRepository]] = MarketStateRepositoryMock.ContextsByFilter(
    equalTo(extendedSearchParams),
    value(expectedAuctionContexts)
  ) ++ MarketStateRepositoryMock.StateByContexts(
    hasSameElements(expectedAuctionContexts.flatMap(_.narrow)),
    value(expectedStateByContexts)
  )

  val promoCampaignServiceClientMock =
    PromoCampaignServiceClientMock
      .Predict(
        anything,
        value(
          PredictBidsResponse(
            (kiaRioOffers ++ bmwOffers).map(x =>
              PredictBidsResponse.BidPrediction(Some(CriteriaValue(key = "", value = x.offerId)), Some(Money(5000)))
            )
          )
        )
      )

  val extendedContextsWithoutDealer: List[AuctionContext] = kiaRioOffers.map { offer =>
    AuctionContext(
      offerId = offer.offerId,
      category = Category.CARS,
      section = Section.USED,
      regionId = 123,
      markCode = "KIA",
      modelCode = "RIO",
      superGenId = 456
    )
  } ++ bmwOffers.map { offer =>
    AuctionContext(
      offerId = offer.offerId,
      category = Category.CARS,
      section = Section.USED,
      regionId = 123,
      markCode = "BMW",
      superGenId = 645
    )
  }

  def toDummyRawAuctionState(contexts: List[AuctionContext]): Map[AuctionContext, AuctionContextState] =
    contexts.map { context =>
      context -> AuctionContextState(
        project = Project.AUTORU,
        product = "auction-used",
        userId = context.userId.toString,
        basePrice = Some(Money(100)),
        oneStep = Some(Money(5000))
      )
    }.toMap
}
