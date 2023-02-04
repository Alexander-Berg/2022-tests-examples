package ru.yandex.realty.render.stats

import java.util.Collections.emptyList
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.building.model.Building.BuildingPriceStatisticsSeries
import ru.yandex.realty.context.v2.AuctionResultStorage
import ru.yandex.realty.generator.Generators
import ru.yandex.realty.generators.BnbSearcherGenerators.renderParamsGen
import ru.yandex.realty.graph.MutableRegionGraph
import ru.yandex.realty.model.offer.Rooms
import ru.yandex.realty.model.sites.{ExtendedSiteStatisticsAtom, SimpleSiteStatisticsResult, Site}
import ru.yandex.realty.render.stats.NewbuildingPriceStatisticsSeriesStateType.IS_PRESENT
import ru.yandex.realty.sites.campaign.CampaignStorage
import ru.yandex.realty.storage.CurrencyStorage
import ru.yandex.vertis.generators.ProducerProvider._

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class StatsRendererSpec extends WordSpec with MockFactory with Matchers with OneInstancePerTest {

  private val site = mock[Site]
  private val allRooms = StatsRenderer.AllRooms
  private val emptyBuildingPriceStatisticsSeries = BuildingPriceStatisticsSeries.getDefaultInstance

  "StatsRenderer" should {

    "not render min price when it is absent in all non-soldout offers" in {
      val extendedSiteStats = Generators
        .extendedSiteStatisticsGen(
          withMinPrice = false,
          possibleRooms = allRooms
        )
        .next
      (site.getBuildingPriceStatisticsSeries _).expects().once().returning(emptyBuildingPriceStatisticsSeries)

      val stats = TestStatsRenderer.renderSiteStats(site, extendedSiteStats, renderParamsGen.next)

      stats.from.isDefined shouldBe (false)
    }

    "not render min price when stats for required rooms are not provided" in {
      val extendedSiteStats = Generators
        .extendedSiteStatisticsGen(
          withMinPrice = false,
          possibleRooms = Seq(Rooms._5, Rooms._7)
        )
        .next
      (site.getBuildingPriceStatisticsSeries _).expects().once().returning(emptyBuildingPriceStatisticsSeries)

      val stats = TestStatsRenderer.renderSiteStats(site, extendedSiteStats, renderParamsGen.next)

      stats.from.isDefined shouldBe (false)
    }

    "render min price when it is provided in any non-soldout offers" in {
      val extendedSiteStats = Generators
        .extendedSiteStatisticsGen(
          minRoomsCount = Some(2),
          possibleRooms = allRooms
        )
        .next
      (site.getBuildingPriceStatisticsSeries _).expects().once().returning(emptyBuildingPriceStatisticsSeries)

      val stats = TestStatsRenderer.renderSiteStats(site, extendedSiteStats, renderParamsGen.next)

      stats.from.isDefined shouldBe (true)
    }

    "render min price when it is provided in single non-soldout offer" in {
      val extendedSiteStats = Generators
        .extendedSiteStatisticsGen(
          maxRoomsCount = Some(1),
          possibleRooms = allRooms
        )
        .next
      (site.getBuildingPriceStatisticsSeries _).expects().once().returning(emptyBuildingPriceStatisticsSeries)

      val stats = TestStatsRenderer.renderSiteStats(site, extendedSiteStats, renderParamsGen.next)

      stats.from.isDefined shouldBe (true)
    }

    "render not empty building price statistics series " in {
      (site.getBuildingPriceStatisticsSeries _)
        .expects()
        .once()
        .returning(Generators.buildBuildingPriceStatisticsSeriesGen(10).next)
      val trustedOffers = 100
      val extendedSiteStats = Generators.extendedSiteStatisticsGen().next
      val actualSiteStats = new SimpleSiteStatisticsResult(
        extendedSiteStats.getTotal.getAreaRange,
        extendedSiteStats.getTotal.getPriceRange,
        extendedSiteStats.getTotal.getPriceSqMRange,
        trustedOffers,
        extendedSiteStats.getTotal.getUntrustedOffers,
        extendedSiteStats.getTotal.getWithPlan,
        extendedSiteStats.getTotal.getAvgPriceSqM,
        extendedSiteStats.getTotal.getSaleStatus
      )
      val actualExtSiteStats = new ExtendedSiteStatisticsAtom(actualSiteStats, extendedSiteStats.getByRooms)

      val stats = TestStatsRenderer.renderSiteStats(site, actualExtSiteStats, renderParamsGen.next)

      stats.newbuildingPriceStatisticsSeriesState.series.size shouldBe 4
      stats.newbuildingPriceStatisticsSeriesState.state shouldBe IS_PRESENT
    }

  }

  private object TestStatsRenderer extends StatsRenderer {

    override def auctionResultProvider: Provider[AuctionResultStorage] =
      () => mock[AuctionResultStorage]

    override def currencyProvider: Provider[CurrencyStorage] =
      () => new CurrencyStorage(emptyList(), emptyList(), () => new MutableRegionGraph())

    override def campaignProvider: Provider[CampaignStorage] =
      () => new CampaignStorage(emptyList())

  }

}
