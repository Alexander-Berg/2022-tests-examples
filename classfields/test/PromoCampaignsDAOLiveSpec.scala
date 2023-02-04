package vsmoney.auction_auto_strategy.storage.test

import cats.data.NonEmptyList
import common.models.finance.Money.Kopecks
import common.zio.doobie.testkit.TestMySQL
import ru.auto.api.search.search_model.{CatalogFilter, SearchRequestParameters}
import ru.auto.dealer_calls_auction.proto.promo_campaign_service.DaysNumberFilters
import vsmoney.auction_auto_strategy.model.auction.Bid
import vsmoney.auction_auto_strategy.model.common._
import vsmoney.auction_auto_strategy.model.promo_campaign._
import vsmoney.auction_auto_strategy.storage.{PromoCampaignDAO, PromoCampaignsDAO}
import vsmoney.auction_auto_strategy.storage.formats._
import vsmoney.auction_auto_strategy.storage.impl.{PromoCampaignDAOLive, PromoCampaignsDAOLive}
import vsmoney.auction_auto_strategy.storage.table.PromoCampaignsTable._
import zio._
import zio.test.TestAspect._
import zio.test._
import zio.magic._
import zio.test.Assertion._

import java.time.LocalDate

object PromoCampaignsDAOLiveSpec extends DefaultRunnableSpec with CommonFormats with JsonFormats {

  def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PromoCampaignsDAOLive")(
      testM("should read PromoCampaigns by id") {
        for {
          dao <- ZIO.service[PromoCampaignsDAO]
          now <- zio.clock.localDateTime.map(_.toLocalDate)

          (promoCampaigns1Expected, promoCampaigns2Expected) = buildPromoCampaigns(now)

          promoCampaignsIds1 <- ZIO.foreach(promoCampaigns1Expected)(createPromoCampaign)
          promoCampaignsIds2 <- ZIO.foreach(promoCampaigns2Expected)(createPromoCampaign)

          promoCampaigns1Actual <- dao.readPromoCampaignsByIds(NonEmptyList.fromList(promoCampaignsIds1).get).settings
          promoCampaigns2Actual <- dao.readPromoCampaignsByIds(NonEmptyList.fromList(promoCampaignsIds2).get).settings
        } yield {
          assertTrue(promoCampaigns1Actual == promoCampaigns1Expected) &&
          assertTrue(promoCampaigns2Actual == promoCampaigns2Expected)
        }
      },
      testM("should read PromoCampaigns") {
        for {
          dao <- ZIO.service[PromoCampaignsDAO]
          now <- zio.clock.localDateTime.map(_.toLocalDate)

          (promoCampaigns1Expected, promoCampaigns2Expected) = buildPromoCampaigns(now)

          _ <- ZIO.foreach_(promoCampaigns1Expected ::: promoCampaigns2Expected)(createPromoCampaign)

          promoCampaigns1 <- dao.readPromoCampaignsByUsersProducts(ProductId("p1"), UserId("1")).settings
          promoCampaigns2 <- dao.readPromoCampaignsByUsersProducts(ProductId("p1"), UserId("2")).settings
        } yield {
          assertTrue(promoCampaigns1 == promoCampaigns1Expected) &&
          assertTrue(promoCampaigns2 == promoCampaigns2Expected)
        }
      },
      testM("should read PromoCampaigns for specified statuses") {
        for {
          dao <- ZIO.service[PromoCampaignsDAO]
          now <- zio.clock.localDateTime.map(_.toLocalDate)

          (promoCampaigns1, promoCampaigns2) = buildPromoCampaigns(now)

          created <- ZIO.foreach(promoCampaigns1 ::: promoCampaigns2)(createPromoCampaign)
          _ <- ZIO.foreach_(created.take(2))(pausePromoCampaign)
          active = promoCampaigns1.drop(2) ::: promoCampaigns2

          promoCampaigns <- dao
            .readPromoCampaignsByProductsStatuses(ProductId("p1"), NonEmptyList.of(Status.Active), 5)
            .runCollect
        } yield assert(promoCampaigns.map(_.settings).toList)(hasSameElementsDistinct(active))
      }
    ) @@ beforeAll(init) @@ after(truncate(table)) @@ sequential
  }.injectCustomShared(
    TestMySQL.managedTransactor,
    PromoCampaignDAOLive.live,
    PromoCampaignsDAOLive.live
  )

  private def createPromoCampaign(promoCampaign: PromoCampaignSettings): RIO[Has[PromoCampaignDAO], PromoCampaignId] =
    for {
      dao <- ZIO.service[PromoCampaignDAO]
      promoCampaignId <- dao.createPromoCampaign(promoCampaign)
    } yield promoCampaignId

  private def pausePromoCampaign(promoCampaignId: PromoCampaignId): RIO[Has[PromoCampaignDAO], PromoCampaignId] =
    for {
      dao <- ZIO.service[PromoCampaignDAO]
      _ <- dao.updatePromoCampaignStatus(promoCampaignId, Status.ToBePaused)
    } yield promoCampaignId

  private def buildPromoCampaigns(date: LocalDate): (List[PromoCampaignSettings], List[PromoCampaignSettings]) =
    (1 to 20).toList
      .flatMap(_ => List(buildPromoCampaign("1", date), buildPromoCampaign("2", date)))
      .partition(_.userId.id == "1")

  private def buildPromoCampaign(
      userId: String,
      date: LocalDate): PromoCampaignSettings =
    PromoCampaignSettings(
      product = ProductId("p1"),
      userId = UserId(userId),
      name = "Супер кампания",
      dateFrom = date.minusDays(10),
      dateTo = date,
      competitionDomain = CompetitionDomain.OfferFilterDomain(
        searcherFilters = SearchRequestParameters(catalogFilter = Seq(CatalogFilter(Some("OPEL")))),
        maxOfferDailyCalls = 10,
        daysNumberFilters = DaysNumberFilters(
          daysOnStockFrom = Some(1),
          daysOnStockTo = None,
          daysWithoutCallsFrom = None,
          daysWithoutCallsTo = Some(10)
        )
      ),
      biddingAlgorithm = BiddingAlgorithm.MaximumPositionForPrice(Bid(Kopecks(10000))),
      status = Status.Active,
      description = "Some description"
    )

  implicit private class TaskPromoCampaignOpsOps(private val x: Task[List[PromoCampaign]]) extends AnyVal {
    def settings: Task[List[PromoCampaignSettings]] = x.map(_.map(_.settings))
  }

}
