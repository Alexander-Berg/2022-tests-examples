package auto.dealers.dealer_calls_auction.logic.test.market_indicator

import auto.dealers.dealer_calls_auction.logic.market_indicator.{MarketIndicatorService, MarketIndicatorServiceLive}
import auto.dealers.dealer_calls_auction.logic.testkit.common.ClockDependentMocks.{
  clockMock,
  dealerZoneOffsetManagerMock
}
import auto.dealers.dealer_calls_auction.logic.test.CallsAuctionTestCommon
import auto.dealers.dealer_calls_auction.model.market.MarketIndicator
import auto.dealers.dealer_calls_auction.model.market.MarketIndicator.SegmentOffers
import ru.auto.dealer_calls_auction.proto.promo_campaign_service.DaysNumberFilters
import zio.ZIO
import zio.test._

object MarketIndicatorServiceSpec extends DefaultRunnableSpec with CallsAuctionTestCommon {

  private lazy val testLayer =
    clockMock ++
      marketStateRepositoryMock ++
      dealerZoneOffsetManagerMock(dealerClientId) ++
      promoCampaignServiceClientMock >>> MarketIndicatorServiceLive.live

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("MarketIndicatorService")(
      testM("should predict market indicator") {
        val expectedResult = MarketIndicator(
          List(
            SegmentOffers(List.empty),
            SegmentOffers(List.empty),
            SegmentOffers(List.empty),
            SegmentOffers(List("2-rio", "3-rio", "5-rio")),
            SegmentOffers(List("1-rio", "4-rio", "0-bmw")) // max attention
          )
        )
        for {
          marketIndicatorService <- ZIO.service[MarketIndicatorService]
          predictionResult <- marketIndicatorService.predict(
            filters = rawSearchParams,
            maxBid = Some(10000),
            dealerId = dealerClientId,
            promoCampaignLastChanges = None,
            daysNumberParams = DaysNumberFilters.defaultInstance,
            maxOfferDailyCalls = 0L
          )
        } yield assertTrue(predictionResult == expectedResult)
      }
    ).provideLayer(testLayer)
  }
}
