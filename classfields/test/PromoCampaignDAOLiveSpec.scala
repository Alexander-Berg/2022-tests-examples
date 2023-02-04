package vsmoney.auction_auto_strategy.storage.test

import common.models.finance.Money.Kopecks
import common.zio.doobie.testkit.TestMySQL
import common.zio.logging.Logging
import ru.auto.api.search.search_model.{CatalogFilter, SearchRequestParameters}
import ru.auto.dealer_calls_auction.proto.promo_campaign_service.DaysNumberFilters
import vsmoney.auction_auto_strategy.model.auction.Bid
import vsmoney.auction_auto_strategy.model.common._
import vsmoney.auction_auto_strategy.model.promo_campaign._
import vsmoney.auction_auto_strategy.storage.PromoCampaignDAO
import vsmoney.auction_auto_strategy.storage.formats._
import vsmoney.auction_auto_strategy.storage.impl.PromoCampaignDAOLive
import vsmoney.auction_auto_strategy.storage.table.PromoCampaignsTable._
import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.magic._

import java.time.{Duration, LocalDate}

object PromoCampaignDAOLiveSpec extends DefaultRunnableSpec with CommonFormats with JsonFormats {

  def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PromoCampaignDAOLive")(
      testM("should create and then read PromoCampaign") {
        for {
          dao <- ZIO.service[PromoCampaignDAO]
          now <- zio.clock.localDateTime.map(_.toLocalDate)
          promoCampaignExpectedSettings = buildPromoCampaignSettings(now)
          promoCampaignId <- dao.createPromoCampaign(promoCampaignExpectedSettings)
          promoCampaignActual <- dao.readPromoCampaign(promoCampaignId).someOrFailException
        } yield assertTrue(promoCampaignExpectedSettings == promoCampaignActual.settings)
      },
      testM("should be able to handle concurrency during creation") {
        for {
          dao <- ZIO.service[PromoCampaignDAO]
          now <- zio.clock.localDateTime.map(_.toLocalDate)
          promoCampaignSettings = buildPromoCampaignSettings(now)
          rs <- ZIO.foreachParN(10)((1 to 100).toList)(_ => dao.createPromoCampaign(promoCampaignSettings))
        } yield assertTrue(rs.size == 100)
      },
      testM("update PromoCampaign completely") {
        for {
          dao <- ZIO.service[PromoCampaignDAO]
          now <- zio.clock.localDateTime.map(_.toLocalDate)

          promoCampaignOriginalSettings = buildPromoCampaignSettings(now)

          nameUpdated = "Обновленная супер кампания"
          descriptionUpdated = "Обновлённое описание"
          dateFromUpdated = now.plusDays(10)
          dateToUpdated = now.plusDays(20)
          competitionDomainUpdated = CompetitionDomain.OfferFilterDomain(
            searcherFilters = SearchRequestParameters(catalogFilter = Seq(CatalogFilter(Some("BMW")))),
            maxOfferDailyCalls = 10,
            daysNumberFilters = DaysNumberFilters(
              daysOnStockFrom = Some(1),
              daysOnStockTo = None,
              daysWithoutCallsFrom = None,
              daysWithoutCallsTo = Some(10)
            )
          )
          biddingAlgorithmUpdated = BiddingAlgorithm.MaximumPositionForPrice(Bid(Kopecks(20000)))

          promoCampaignId <- dao.createPromoCampaign(promoCampaignOriginalSettings)
          _ <- dao.updatePromoCampaignSettings(
            promoCampaignId,
            Some(nameUpdated),
            Some(dateFromUpdated),
            Some(dateToUpdated),
            Some(competitionDomainUpdated),
            Some(biddingAlgorithmUpdated),
            changeCompetitionDomain = false,
            Some(descriptionUpdated)
          )
          promoCampaignUpdated <- dao.readPromoCampaign(promoCampaignId).someOrFailException
        } yield {
          assertTrue(promoCampaignUpdated.settings.name == nameUpdated) &&
          assertTrue(promoCampaignUpdated.settings.dateFrom == dateFromUpdated) &&
          assertTrue(promoCampaignUpdated.settings.dateTo == dateToUpdated) &&
          assertTrue(promoCampaignUpdated.settings.competitionDomain == competitionDomainUpdated) &&
          assertTrue(promoCampaignUpdated.settings.biddingAlgorithm == biddingAlgorithmUpdated)
        }
      },
      testM("update PromoCampaign partially") {
        for {
          dao <- ZIO.service[PromoCampaignDAO]
          now <- zio.clock.localDateTime.map(_.toLocalDate)

          promoCampaignOriginalSettings = buildPromoCampaignSettings(now)

          nameUpdated = "Обновленная супер кампания"

          promoCampaignId <- dao.createPromoCampaign(promoCampaignOriginalSettings)
          _ <- dao.updatePromoCampaignSettings(
            promoCampaignId,
            Some(nameUpdated),
            dateFromOpt = None,
            dateToOpt = None,
            competitionDomainOpt = None,
            biddingAlgorithmOpt = None,
            changeCompetitionDomain = false,
            descriptionOpt = None
          )
          promoCampaignUpdated <- dao.readPromoCampaign(promoCampaignId).someOrFailException
        } yield assertTrue(promoCampaignUpdated.settings.name == nameUpdated)
      },
      testM("update PromoCampaign competitionDomain") {
        for {
          dao <- ZIO.service[PromoCampaignDAO]
          now <- zio.clock.localDateTime.map(_.toLocalDate)

          promoCampaignOriginalSettings = buildPromoCampaignSettings(now)

          comercialDomainUpdated = CompetitionDomain.OfferFilterDomain(
            searcherFilters = SearchRequestParameters(catalogFilter = Seq(CatalogFilter(Some("BMW")))),
            maxOfferDailyCalls = 10,
            daysNumberFilters = DaysNumberFilters(
              daysOnStockFrom = Some(1),
              daysOnStockTo = None,
              daysWithoutCallsFrom = None,
              daysWithoutCallsTo = Some(10)
            )
          )

          promoCampaignId <- dao.createPromoCampaign(promoCampaignOriginalSettings)
          oldPromoCompainOpt <- dao.readPromoCampaign(promoCampaignId)
          oldPromoCompain <- ZIO.getOrFail(oldPromoCompainOpt)
          // _ <- ZIO.effect(Thread.sleep(1000))
          _ <- dao.updatePromoCampaignSettings(
            promoCampaignId,
            nameOpt = None,
            dateFromOpt = None,
            dateToOpt = None,
            competitionDomainOpt = Some(comercialDomainUpdated),
            biddingAlgorithmOpt = None,
            changeCompetitionDomain = true,
            descriptionOpt = None
          )
          promoCampaignUpdated <- dao.readPromoCampaign(promoCampaignId).someOrFailException
        } yield assertTrue(promoCampaignUpdated.changeAt != oldPromoCompain.changeAt)
      },
      testM("update PromoCampaign status") {
        for {
          dao <- ZIO.service[PromoCampaignDAO]
          now <- zio.clock.localDateTime.map(_.toLocalDate)
          promoCampaignOriginalSettings = buildPromoCampaignSettings(now)
          promoCampaignId <- dao.createPromoCampaign(promoCampaignOriginalSettings)
          result <- ZIO.foreach(Status.values) { status =>
            for {
              _ <- dao.updatePromoCampaignStatus(promoCampaignId, status)
              promoCampaignUpdated <- dao.readPromoCampaign(promoCampaignId).someOrFailException
            } yield status == promoCampaignUpdated.settings.status
          }
        } yield assert(result)(forall(isTrue))
      }
    ) @@ beforeAll(init) @@ after(truncate(table)) @@ sequential
  }.injectCustomShared(
    TestMySQL.managedTransactor,
    PromoCampaignDAOLive.live
  )

  private def buildPromoCampaignSettings(date: LocalDate): PromoCampaignSettings =
    PromoCampaignSettings(
      product = ProductId("call:cars:used"),
      userId = UserId("1"),
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

}
