package ru.yandex.realty.searcher.search.collectors

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.model.offer.SalesAgentCategory
import ru.yandex.realty.searcher.search.stats.OffersStatsCollector
import ru.yandex.realty.searcher.search.stats.OffersStatsCollector._

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class OffersStatsCollectorSpec extends WordSpec with Matchers {
  val items = prepareData

  "OffersStatsCollector" should {
    "calculate stats" in {
      val collector = new OffersStatsCollector
      items.foreach(collector.process)
      val result = collector.getResult

      result.getTotalOffers should be(8)
      result.getTotalOffersFromOwners should be(3)
      result.getTotalOffersFromAgents should be(2)
      result.getTotalOffersFromDevelopers should be(3)

      result.getPriceMin should be(Some(11.0))
      result.getPriceMax should be(Some(18.0))

      result.getAreaMin should be(Some(101.0))
      result.getAreaMax should be(Some(108.0))

      result.getPricePerMeterMin should be(Some(211.0))
      result.getPricePerMeterMax should be(Some(218.0))
    }
  }

  "OffersStatsCollectorManager" should {
    "calculate stats in parallel (using reduce)" in {
      val resultParallel = {
        val collectors = (1 to 3).map(_ => OffersStatsCollectorManager.newCollector)
        val it = Iterator.continually(collectors).flatten
        items.foreach(item => it.next.process(item))
        OffersStatsCollectorManager.reduce(collectors.asJava)
      }
      val resultSingle = {
        val collector = new OffersStatsCollector
        items.foreach(collector.process)
        collector.getResult
      }

      resultParallel shouldEqual (resultSingle)
    }
  }

  private def prepareData: Seq[FoundItem] = {
    Seq(
      FoundItem(
        price = Some(11.0),
        area = Some(101.0),
        pricePerMeter = Some(211.0),
        agencyCategory = SalesAgentCategory.OWNER
      ),
      FoundItem(
        price = Some(12.0),
        area = Some(102.0),
        pricePerMeter = Some(212.0),
        agencyCategory = SalesAgentCategory.OWNER
      ),
      FoundItem(
        price = Some(13.0),
        area = Some(103.0),
        pricePerMeter = Some(213.0),
        agencyCategory = SalesAgentCategory.OWNER
      ),
      FoundItem(
        price = Some(14.0),
        area = Some(104.0),
        pricePerMeter = Some(214.0),
        agencyCategory = SalesAgentCategory.AGENCY
      ),
      FoundItem(
        price = Some(15.0),
        area = Some(105.0),
        pricePerMeter = Some(215.0),
        agencyCategory = SalesAgentCategory.AGENCY
      ),
      FoundItem(
        price = Some(16.0),
        area = Some(106.0),
        pricePerMeter = Some(216.0),
        agencyCategory = SalesAgentCategory.DEVELOPER
      ),
      FoundItem(
        price = Some(17.0),
        area = Some(107.0),
        pricePerMeter = Some(217.0),
        agencyCategory = SalesAgentCategory.DEVELOPER
      ),
      FoundItem(
        price = Some(18.0),
        area = Some(108.0),
        pricePerMeter = Some(218.0),
        agencyCategory = SalesAgentCategory.DEVELOPER
      )
    )
  }
}
