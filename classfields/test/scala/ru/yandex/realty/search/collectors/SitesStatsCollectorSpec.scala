package ru.yandex.realty.search.collectors
import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.model.sites.BuildingClass
import ru.yandex.realty.search.lucene.collectors.sites.SitesStatsCollector
import ru.yandex.realty.search.lucene.collectors.sites.SitesStatsCollector._
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class SitesStatsCollectorSpec extends WordSpec with Matchers {
  val items = prepareData

  "SitesStatsCollector" should {
    "calculate stats" in {
      val collector = new SitesStatsCollector
      items.foreach(collector.process)
      val result = collector.getResult

      result.getTotalSites should be(7) // unique siteIds
      result.getTotalDevelopers should be(4)

      result.getTotalSitesEconom should be(2)
      result.getTotalSitesComfort should be(3)
      result.getTotalSitesElit should be(2)

      result.getTotalSitesWithDecoration should be(3)
      result.getTotalSitesWithInstallment should be(2)
      result.getTotalSitesWithFz214 should be(3)
      result.getTotalSitesWithMortgage should be(3)

      result.getAreaMin should be(Some(11.0))
      result.getAreaMax should be(Some(107.0))

      result.getPricePerMeterEconomMin should be(Some(211.0))
      result.getPricePerMeterEconomMax should be(Some(212.0))

      result.getPricePerMeterComfortMin should be(Some(213.0))
      result.getPricePerMeterComfortMax should be(Some(215.0))

      result.getPricePerMeterElitMin should be(Some(216.0))
      result.getPricePerMeterElitMax should be(Some(217.0))

      result.getMortgageRateMin should be(Some(3.0))

      result.getPriceMin should be(Some(1001.0))
      result.getPriceMax should be(Some(1017.0))
    }
  }

  "SitesStatsCollectorManager" should {
    "calculate stats in parallel (using reduce)" in {
      val resultParallel = {
        val collectors = (1 to 3).map(_ => SitesStatsCollectorManager.newCollector)
        val it = Iterator.continually(collectors).flatten
        items.foreach(item => it.next.process(item))
        SitesStatsCollectorManager.reduce(collectors.asJava)
      }
      val resultSingle = {
        val collector = new SitesStatsCollector
        items.foreach(collector.process)
        collector.getResult
      }

      resultParallel shouldEqual (resultSingle)
    }
  }

  private def prepareData: Seq[FoundItem] = {
    Seq(
      FoundItem(
        siteId = 1L,
        areaFrom = Some(11.0),
        areaTo = Some(101.0),
        pricePerMeter = Some(211.0),
        hasFz214 = false,
        hasDecoration = false,
        hasMortgage = false,
        developerIds = Set(1, 2, 3),
        hasInstallment = false,
        buildingClass = BuildingClass.ECONOM,
        mortgageMinRate = None,
        priceFrom = Some(1001.0),
        priceTo = Some(1011.0)
      ),
      FoundItem(
        siteId = 1L, // same site again to check for duplicates
        areaFrom = Some(11.5),
        areaTo = Some(101.5),
        pricePerMeter = Some(211.5),
        hasFz214 = false,
        hasDecoration = true,
        hasMortgage = false,
        developerIds = Set(),
        hasInstallment = false,
        buildingClass = BuildingClass.ECONOM,
        mortgageMinRate = None,
        priceFrom = Some(1001.5),
        priceTo = Some(1011.5)
      ),
      FoundItem(
        siteId = 2L,
        areaFrom = Some(12.0),
        areaTo = Some(102.0),
        pricePerMeter = Some(212.0),
        hasFz214 = false,
        hasDecoration = true,
        hasMortgage = false,
        developerIds = Set(2, 3, 4),
        hasInstallment = false,
        buildingClass = BuildingClass.STANDART,
        mortgageMinRate = None,
        priceFrom = Some(1002.0),
        priceTo = Some(1012.0)
      ),
      FoundItem(
        siteId = 3L,
        areaFrom = Some(13.0),
        areaTo = Some(103.0),
        pricePerMeter = Some(213.0),
        hasFz214 = true,
        hasDecoration = true,
        hasMortgage = true,
        developerIds = Set(2, 3),
        hasInstallment = true,
        buildingClass = BuildingClass.COMFORT,
        mortgageMinRate = Some(3.0),
        priceFrom = Some(1003.0),
        priceTo = Some(1013.0)
      ),
      FoundItem(
        siteId = 4L,
        areaFrom = Some(14.0),
        areaTo = Some(104.0),
        pricePerMeter = Some(214.0),
        hasFz214 = true,
        hasDecoration = false,
        hasMortgage = true,
        developerIds = Set(2, 3),
        hasInstallment = true,
        buildingClass = BuildingClass.COMFORT,
        mortgageMinRate = Some(4.0),
        priceFrom = Some(1004.0),
        priceTo = Some(1014.0)
      ),
      FoundItem(
        siteId = 5L,
        areaFrom = Some(15.0),
        areaTo = Some(105.0),
        pricePerMeter = Some(215.0),
        hasFz214 = false,
        hasDecoration = false,
        hasMortgage = true,
        developerIds = Set(1),
        hasInstallment = false,
        buildingClass = BuildingClass.COMFORT_PLUS,
        mortgageMinRate = Some(5.0),
        priceFrom = Some(1005.0),
        priceTo = Some(1015.0)
      ),
      FoundItem(
        siteId = 6L,
        areaFrom = Some(16.0),
        areaTo = Some(106.0),
        pricePerMeter = Some(216.0),
        hasFz214 = false,
        hasDecoration = false,
        hasMortgage = false,
        developerIds = Set(2),
        hasInstallment = false,
        buildingClass = BuildingClass.ELITE,
        mortgageMinRate = Some(6.0),
        priceFrom = Some(1006.0),
        priceTo = Some(1016.0)
      ),
      FoundItem(
        siteId = 7L,
        areaFrom = Some(17.0),
        areaTo = Some(107.0),
        pricePerMeter = Some(217.0),
        hasFz214 = true,
        hasDecoration = false,
        hasMortgage = false,
        developerIds = Set(3),
        hasInstallment = false,
        buildingClass = BuildingClass.BUSINESS,
        mortgageMinRate = Some(7.0),
        priceFrom = Some(1007.0),
        priceTo = Some(1017.0)
      )
    )
  }
}
