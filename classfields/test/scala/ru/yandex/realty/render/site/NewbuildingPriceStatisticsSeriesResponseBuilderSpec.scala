package ru.yandex.realty.render.site

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.building.model.Building.{
  BuildingPriceStatisticsAllRoomsSeries,
  BuildingPriceStatisticsSeries,
  BuildingPriceStatisticsSeriesItem
}
import ru.yandex.realty.generator.Generators.{
  buildBuildingPriceStatisticsSeriesGen,
  buildingPriceStatisticsSeriesItemGen
}
import ru.yandex.realty.model.sites.Site
import ru.yandex.realty.render.stats.NewbuildingPriceStatisticsSeriesStateType.{
  EMPTY_NEW_SITE,
  EMPTY_NO_OFFERS,
  IS_PRESENT
}
import ru.yandex.realty.response.NewbuildingPriceStatisticsSeriesResponseBuilder
import ru.yandex.vertis.generators.ProducerProvider._

import java.time.LocalDateTime
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class NewbuildingPriceStatisticsSeriesResponseBuilderSpec extends SpecBase {

  "NewbuildingPriceStatisticsSeriesResponseBuilder in buildCardResponse" should {
    "return is_present state if there is enough building price statistics series items" in {
      val site = buildSite(10)
      val response = NewbuildingPriceStatisticsSeriesResponseBuilder.buildCardResponse(site, 100)
      response.state shouldBe IS_PRESENT
      response.series.size shouldBe 4
    }

    "return empty_new_site state if there isn't enough building price statistics series items and site has offers" in {
      val site = buildSite(1)
      val response = NewbuildingPriceStatisticsSeriesResponseBuilder.buildCardResponse(site, 10)
      response.state shouldBe EMPTY_NEW_SITE
      response.series.isEmpty shouldBe true
    }

    "return empty_no_offers state if there isn't enough building price statistics series items and site hasn't offers" in {
      val site = buildSite(0)
      val response = NewbuildingPriceStatisticsSeriesResponseBuilder.buildCardResponse(site, 0)
      response.state shouldBe EMPTY_NO_OFFERS
      response.series.isEmpty shouldBe true
    }

    "return fixed series if series contains outliers" in {
      val now = LocalDateTime.now()
      val items = Seq(
        buildingPriceStatisticsSeriesItemGen(now.minusMonths(10), 120000000, 3200000).next,
        buildingPriceStatisticsSeriesItemGen(now.minusMonths(8), 1200000, 32000).next,
        buildingPriceStatisticsSeriesItemGen(now.minusMonths(6), 1300000, 33000).next,
        buildingPriceStatisticsSeriesItemGen(now.minusMonths(4), 110000000, 3100000).next,
        buildingPriceStatisticsSeriesItemGen(now.minusMonths(2), 100000000, 3000000).next,
        buildingPriceStatisticsSeriesItemGen(now, 1000000, 31000).next
      )
      val series = buildBuildingPriceStatisticsSeries(items)
      val site = new Site(1)
      site.setBuildingPriceStatisticsSeries(series)
      val response = NewbuildingPriceStatisticsSeriesResponseBuilder.buildCardResponse(site, 100)
      val expectedMeanPriceList = Seq(1300000, 1100000, 1000000, 1000000)
      val expectedMeanPricePerM2List = Seq(33000, 31000, 30000, 31000)
      response.state shouldBe IS_PRESENT
      response.series.map(_.getMeanPrice) shouldEqual expectedMeanPriceList
      response.series.map(_.getMeanPricePerM2) shouldEqual expectedMeanPricePerM2List
    }
  }

  private def buildSite(priceStatisticsSeriesItemsCount: Int): Site = {
    val site = new Site(1)
    site.setBuildingPriceStatisticsSeries(buildBuildingPriceStatisticsSeriesGen(priceStatisticsSeriesItemsCount).next)
    site
  }

  private def buildBuildingPriceStatisticsSeries(
    items: Seq[BuildingPriceStatisticsSeriesItem]
  ): BuildingPriceStatisticsSeries = {
    BuildingPriceStatisticsSeries
      .newBuilder()
      .setAllRoomsSeries(BuildingPriceStatisticsAllRoomsSeries.newBuilder().addAllItems(items.asJava))
      .build()
  }

}
