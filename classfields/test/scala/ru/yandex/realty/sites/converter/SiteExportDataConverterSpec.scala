package ru.yandex.realty.sites.converter

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.context.v2.AuctionResultStorage
import ru.yandex.realty.model.location.{GeoPoint, Location}
import ru.yandex.realty.model.offer.BuildingState
import ru.yandex.realty.model.sites.{ExtendedSiteStatistics, ExtendedSiteStatisticsAtom, House, Phase, Site}
import ru.yandex.realty.sites.config.DomainConfig
import ru.yandex.realty.sites.model.{SiteExportData, SiteUrls}
import ru.yandex.realty.sites.{CompaniesStorage, ExtendedSiteStatisticsStorage, SiteReviewsStorage}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class SiteExportDataConverterSpec extends SpecBase {

  private val converter = new SiteExportDataConverter(
    () => mock[CompaniesStorage],
    () => mock[ExtendedSiteStatisticsStorage],
    () => mock[AuctionResultStorage],
    () => mock[SiteReviewsStorage],
    new DomainConfig("desktopDomainUrl", "mobileDomainUrl"),
    "phoneEventUrl"
  )

  "SiteExportDataConverter" should {
    "convert site with exportPhases null returns 1 record " in {
      val site = initSiteWithPhasesAndHouses()
      val nodes = converter.convert(initSiteDataExport(site))
      nodes.length shouldBe 1
      nodes.head.get("name").get().stringValue() shouldBe ("name")
    }
    "convert site with exportPhases false returns 1 record " in {
      val site = initSiteWithPhasesAndHouses()
      site.setShouldExportPhases(false)
      val nodes = converter.convert(initSiteDataExport(site))
      nodes.length shouldBe 1
      nodes.head.get("name").get().stringValue() shouldBe ("name")
    }
  }

  private def initSiteDataExport(site: Site) = {
    val siteExportData = SiteExportData(
      site,
      ExtendedSiteStatistics.EMPTY,
      Map(),
      None,
      ExtendedSiteStatisticsAtom.EMPTY,
      None,
      None,
      None,
      newbuildingUrls = SiteUrls("url", "mobileUrl")
    )
    siteExportData
  }

  private def initSiteWithNoPhases() = {
    val site = new Site(1L)
    site.setLocation(initLocation())
    site.setName("noPhases")
    site.setPhases(Seq.empty[Phase].asJava)
    site
  }

  private def initSiteWithNoHouses() = {
    val site = new Site(1L)
    site.setLocation(initLocation())
    site.setName("noHouses")

    val phase11 = new Phase(11L)
    phase11.setSite(site)
    phase11.setState(BuildingState.UNFINISHED)
    phase11.setHouses(Seq.empty[House].asJava)

    val phase22 = new Phase(22L)
    phase22.setSite(site)
    phase22.setState(BuildingState.BUILT)
    phase22.setHouses(Seq.empty[House].asJava)
    site.setPhases(Seq(phase11, phase22).asJava)
    site
  }

  private def initSiteWithPhasesAndHouses() = {
    val site = new Site(1L)
    site.setLocation(initLocation())
    site.setName("name")

    val phase11 = new Phase(11L)
    phase11.setSite(site)
    phase11.setState(BuildingState.UNFINISHED)
    phase11.setHouses(Seq(initHouse(111L), initHouse(112L)).asJava)

    val phase22 = new Phase(22L)
    phase22.setSite(site)
    phase22.setState(BuildingState.BUILT)
    phase22.setHouses(Seq(initHouse(221L), initHouse(222L), initHouse(223L)).asJava)
    site.setPhases(Seq(phase11, phase22).asJava)
    site
  }

  private def initHouse(id: Long) = {
    val house = new House(id)
    house.setBuildingSiteName(s"name$id")
    house.setLocation(initLocation())
    house
  }

  private def initLocation() = {
    val location = new Location()
    val point = for {
      latitude <- Gen.chooseNum(0.0f, 80.0f)
      longitude <- Gen.chooseNum(0.0f, 80.0f)
    } yield new GeoPoint(latitude, longitude)
    location.setManualPoint(point.sample.get)
    location
  }
}
