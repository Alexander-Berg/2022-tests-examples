package vsmoney.auction_auto_strategy.api.test

import com.google.protobuf.timestamp.Timestamp
import common.models.finance.Money.Kopecks
import io.scalaland.chimney.dsl._
import ru.auto.api.search.search_model.{CatalogFilter, SearchRequestParameters}
import ru.auto.dealer_calls_auction.proto.promo_campaign_service.DaysNumberFilters
import vsmoney.auction_auto_strategy.api.exceptions.ProtoParsingException
import vsmoney.auction_auto_strategy.model.auction.{
  AuctionKey,
  Bid,
  CriteriaContext,
  Criterion,
  CriterionKey,
  CriterionValue
}
import vsmoney.auction_auto_strategy.model.common.{ProductId, Project, UserId}
import vsmoney.auction_auto_strategy.model.promo_campaign.{PromoCampaignAuctionBid, PromoCampaignId}
import vsmoney.auction_auto_strategy.model.{promo_campaign => domain}
import vsmoney.auction_auto_strategy.promo_campaign.PromoCampaign
import vsmoney.auction_auto_strategy.promo_campaign_service.{PredictBidsResponse, PromoCampaignResponse}
import zio.ZIO
import zio.test.environment._
import zio.test._
import zio.test.Assertion._

import java.time._

object TransformersSpec extends DefaultRunnableSpec {

  import vsmoney.auction_auto_strategy.api.chimney.transformers._

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("Transformers")(
      testM("should convert LocalDate to Timestamp and vice versa") {
        val now = LocalDate.now()
        for {
          timestamp <- now.transformIntoZIO[Timestamp]
          fromTimestamp <- timestamp.transformIntoZIO[LocalDate]
        } yield assertTrue(fromTimestamp == now)
      },
      testM("should convert PromoCampaignSettings and vice versa") {
        val promoCampaignSettings =
          domain.PromoCampaignSettings(
            product = ProductId("p1"),
            userId = UserId("1"),
            name = "Супер кампания",
            dateFrom = LocalDate.now().minusDays(10),
            dateTo = LocalDate.now(),
            competitionDomain = domain.CompetitionDomain.OfferFilterDomain(
              searcherFilters = SearchRequestParameters(catalogFilter = Seq(CatalogFilter(Some("OPEL")))),
              maxOfferDailyCalls = 10,
              daysNumberFilters = DaysNumberFilters(
                daysOnStockFrom = Some(1),
                daysOnStockTo = None,
                daysWithoutCallsFrom = None,
                daysWithoutCallsTo = Some(5)
              )
            ),
            biddingAlgorithm = domain.BiddingAlgorithm.MaximumPositionForPrice(Bid(Kopecks(10000))),
            status = domain.Status.Active,
            description = ""
          )
        for {
          to <- promoCampaignSettings.transformIntoZIO[PromoCampaign]
          from <- to.transformIntoZIO[domain.PromoCampaignSettings]
        } yield assertTrue(from == promoCampaignSettings)
      },
      testM("should convert PromoCampaign and vice versa") {
        val promoCampaignSettings =
          domain.PromoCampaignSettings(
            product = ProductId("p1"),
            userId = UserId("1"),
            name = "Супер кампания",
            dateFrom = LocalDate.now().minusDays(10),
            dateTo = LocalDate.now(),
            competitionDomain = domain.CompetitionDomain.OfferFilterDomain(
              searcherFilters = SearchRequestParameters(catalogFilter = Seq(CatalogFilter(Some("OPEL")))),
              maxOfferDailyCalls = 10,
              daysNumberFilters = DaysNumberFilters(
                daysOnStockFrom = Some(1),
                daysOnStockTo = None,
                daysWithoutCallsFrom = None,
                daysWithoutCallsTo = Some(5)
              )
            ),
            biddingAlgorithm = domain.BiddingAlgorithm.MaximumPositionForPrice(Bid(Kopecks(10000))),
            status = domain.Status.Active,
            description = ""
          )
        val promoCampaignId = domain.PromoCampaignId(UserId("1"), 1)
        val promoCampaign = domain.PromoCampaign(promoCampaignId, promoCampaignSettings, LocalDateTime.now())
        for {
          to <- promoCampaign.transformIntoZIO[PromoCampaignResponse]
          from <- to.transformIntoZIO[domain.PromoCampaign]
        } yield assertTrue(from == promoCampaign)
      },
      testM("should convert bids to predict bids response") {
        val key = AuctionKey(
          project = Project.Autoru,
          product = ProductId("call:cars:used"),
          context = CriteriaContext(
            criteria = Seq(
              Criterion(key = CriterionKey("region_id"), value = CriterionValue("1"))
            )
          )
        )

        val obj = Criterion(
          key = CriterionKey("offer_id"),
          value = CriterionValue("2223224")
        )

        val id = PromoCampaignId(userId = UserId("user1"), userCampaignId = 123)

        val bid =
          PromoCampaignAuctionBid(
            key = key,
            auctionObject = obj,
            bid = Kopecks(123),
            promoCampaignId = id
          )

        assertM(
          ZIO
            .foreach(List(bid, bid))(_.transformIntoZIO[PredictBidsResponse.BidPrediction])
            .map(_.headOption.flatMap(_.bid).contains(vsmoney.auction.common_model.Money(123)))
        )(isTrue)
      },
      testM("should handle errors properly") {
        val validDomain =
          domain.PromoCampaignSettings(
            product = ProductId("p1"),
            userId = UserId("1"),
            name = "Супер кампания",
            dateFrom = LocalDate.now().minusDays(10),
            dateTo = LocalDate.now(),
            competitionDomain = domain.CompetitionDomain.OfferFilterDomain(
              searcherFilters = SearchRequestParameters(catalogFilter = Seq(CatalogFilter(Some("OPEL")))),
              maxOfferDailyCalls = 10,
              daysNumberFilters = DaysNumberFilters(
                daysOnStockFrom = Some(1),
                daysOnStockTo = None,
                daysWithoutCallsFrom = None,
                daysWithoutCallsTo = Some(5)
              )
            ),
            biddingAlgorithm = domain.BiddingAlgorithm.MaximumPositionForPrice(Bid(Kopecks(10000))),
            status = domain.Status.Active,
            description = ""
          )

        val testCase =
          for {
            validProto <- validDomain.transformIntoZIO[PromoCampaign]
            invalidProto = validProto.copy(competitionDomain = None)
            _ <- invalidProto.transformIntoZIO[domain.PromoCampaignSettings]
          } yield ()

        assertM(testCase.run)(failsWithA[ProtoParsingException])
      }
    )

}
