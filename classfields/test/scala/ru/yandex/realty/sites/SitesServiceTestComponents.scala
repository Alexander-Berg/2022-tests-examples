package ru.yandex.realty.sites

import ru.yandex.realty.model.location.{GeoPoint, Location}
import ru.yandex.realty.model.offer.BuildingType
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.model.sites.{BuildingClass, House, Phase, Section, Site, SiteType}

import java.util
import scala.collection.JavaConverters._

trait SitesServiceTestComponents {
  val sitesService: SitesGroupingService = SitesServiceTestComponents.sitesService
}

object SitesServiceTestComponents {
  val capitalTowers = new Site(872687)
  capitalTowers.setRid(213)
  capitalTowers.setName("Capital Towers")
  capitalTowers.setFullName("МФК Capital Towers")
  capitalTowers.setLocativeFullName("в МФК Capital Towers")
  capitalTowers.setSiteType(SiteType.ZKH)
  capitalTowers.setAddress("наб. Краснопресненская, вл. 14")
  capitalTowers.setCity("Москва")
  capitalTowers.setGeoPoint(GeoPoint.getPoint(55.751446f, 37.548496f))
  capitalTowers.setBuildingClass(BuildingClass.BUSINESS)
  capitalTowers.setBuildingTypes(Seq(BuildingType.MONOLIT).asJava)
  val capitalTowersLocation = new Location()
  capitalTowersLocation.setRegionGraphId(NodeRgid.MOSCOW)
  capitalTowersLocation.setGeocoderLocation(
    "Россия, Москва, Краснопресненская набережная, вл14с1кБ",
    GeoPoint.getPoint(55.751457f, 37.548546f)
  )
  capitalTowers.setLocation(capitalTowersLocation)
  val capitalTowersPhase = new Phase(872783)
  capitalTowersPhase.setCode("1")
  capitalTowersPhase.setDescription("1 очередь")
  val capitalTowersPhaseHouse1 = new House(873178)
  capitalTowersPhaseHouse1.setCode("1")
  capitalTowersPhaseHouse1.setBuildingSiteName("Park Tower")
  val capitalTowersPhaseHouse1Location = new Location()
  capitalTowersPhaseHouse1Location.setRegionGraphId(NodeRgid.MOSCOW)
  capitalTowersPhaseHouse1Location.setGeocoderLocation(
    "Россия, Москва, Центральный административный округ, Пресненский район, Московский международный деловой центр Москва-Сити",
    GeoPoint.getPoint(55.753036f, 37.548527f)
  )
  capitalTowersPhaseHouse1.setLocation(capitalTowersPhaseHouse1Location)
  capitalTowersPhase.setHouses(Seq(capitalTowersPhaseHouse1).asJava)
  capitalTowers.setPhases(Seq(capitalTowersPhase).asJava)

  val willTower = new Site(2252263)
  willTower.setName("WILL TOWERS")
  willTower.setFullName("ЖК WILL TOWERS")
  willTower.setLocativeFullName("в ЖК WILL TOWERS")
  willTower.setSiteType(SiteType.ZKH)
  willTower.setAliases(Seq("Вилл Товэрсе").asJava)
  willTower.setAddress("пр. Генерала Дорохова")
  willTower.setCity("Москва")
  willTower.getGeoPoint

  willTower.setGeoPoint(GeoPoint.getPoint(55.70972f, 37.493168f))
  willTower.setBuildingClass(BuildingClass.BUSINESS)
  willTower.setBuildingTypes(Seq(BuildingType.MONOLIT).asJava)
  val willTowerLocation = new Location()
  willTowerLocation.setRegionGraphId(NodeRgid.MOSCOW)
  willTowerLocation.setGeocoderLocation(
    "Россия, Москва, Западный административный округ, район Раменки, жилой комплекс Вилл Тауэрс",
    GeoPoint.getPoint(55.708282f, 37.49274f)
  )
  willTower.setLocation(willTowerLocation)
  val willTowerPhase = new Phase(2252283)
  willTowerPhase.setCode("1")
  willTowerPhase.setDescription("1 очередь")
  val willTowerPhaseHouse1 = new House(873178)
  willTowerPhaseHouse1.setCode("1")
  willTowerPhaseHouse1.setBuildingSiteName("к. Creativity")
  val willTowerPhaseHouse1Location = new Location()
  willTowerPhaseHouse1Location.setRegionGraphId(NodeRgid.MOSCOW)
  willTowerPhaseHouse1Location.setGeocoderLocation(
    "Россия, Москва, Западный административный округ, район Раменки, жилой комплекс Вилл Тауэрс",
    GeoPoint.getPoint(55.708282f, 37.49274f)
  )
  willTowerPhaseHouse1.setLocation(willTowerPhaseHouse1Location)
  willTowerPhase.setHouses(Seq(willTowerPhaseHouse1).asJava)
  willTower.setPhases(Seq(willTowerPhase).asJava)

  val sites = Seq(capitalTowers, willTower)

  val sitesService: SitesGroupingService = new SitesGroupingService {
    override def getSiteById(siteId: Long): Site = ???

    override def getAllSites: util.Collection[Site] = sites.asJava

    override def getPhaseById(phaseId: Long): Phase = ???

    override def getHouseById(houseId: Long): House = ???

    override def getSitesByAddress(address: String): util.Set[Site] = ???

    override def getHousesByAddress(address: String): util.Set[House] = ???

    override def getSiteByPartnerHouseId(partnerHouseId: String): Site = ???

    override def getSectionByPartnerHouseId(houseId: String, sectionName: String): Section = ???

    override def getSitesByBuilderId(builderId: Long): util.Collection[Site] = ???
  }
}
