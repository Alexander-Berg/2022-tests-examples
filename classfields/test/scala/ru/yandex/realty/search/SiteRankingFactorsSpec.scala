package ru.yandex.realty.search

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}
import ru.yandex.realty.model.offer.Rooms
import ru.yandex.realty.model.sites.range.RangeImpl
import ru.yandex.realty.model.sites.{ExtendedSiteStatisticsAtom, SimpleSiteStatisticsResult, Site}
import ru.yandex.realty.search.model.{FoundItem, NewbuildingStats}
import ru.yandex.realty.search.ranking.SitesRankingFactorsExtractor.extractSiteFactors

import scala.collection.JavaConverters.mapAsJavaMapConverter

@RunWith(classOf[JUnitRunner])
class SiteRankingFactorsSpec extends WordSpec with MockFactory with Matchers with OneInstancePerTest {

  private val site = new Site(12345)

  private val areaRange = RangeImpl.create(float2Float(10), float2Float(100))
  private val priceRange = RangeImpl.create(float2Float(2000), float2Float(4000))
  private val simpleStat = new SimpleSiteStatisticsResult(
    areaRange,
    priceRange,
    RangeImpl.create(1, 100),
    1,
    1,
    1,
    1.0f
  )

  private val stats = NewbuildingStats(simpleStat, simpleStat)

  private val foundItem = FoundItem(
    site,
    exactMatch = true,
    isActual = true,
    stats = NewbuildingStats(SimpleSiteStatisticsResult.EMPTY, SimpleSiteStatisticsResult.EMPTY)
  )

  private val stat = ExtendedSiteStatisticsAtom.fromRooms(Map(Rooms._1 -> simpleStat).asJava)

  "SiteRankingFactors" should {

    "extract factors without err" in {
      val factors = extractSiteFactors(foundItem, stat)
      println("num", factors.numFactors)
      println("cat", factors.catFactors)
    }

    "correctly parse stat" in {
      val factors = extractSiteFactors(foundItem, stat)
      factors.numFactors("PRICE_FROM_1") shouldBe 2000f
      factors.numFactors("AREA_FROM_1") shouldBe 10f
    }

  }
}
