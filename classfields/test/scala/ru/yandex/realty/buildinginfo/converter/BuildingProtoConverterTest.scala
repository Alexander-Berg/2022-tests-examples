package ru.yandex.realty.buildinginfo.converter

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.buildinginfo.model.{Building, Metro, PriceInfo}
import ru.yandex.realty.model.building.{BuildingPriceStatistics, BuildingPriceStatisticsItem}
import ru.yandex.realty.model.location._
import ru.yandex.realty.model.offer.BuildingType

import scala.collection.JavaConverters._

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class BuildingProtoConverterTest extends FlatSpec with Matchers {

  "BuildingProtoConverter" should "serialize deserialize price statistics correctly" in {
    val building = getBuilding
    val msg = BuildingProtoConverter.toMessage(building)
    val deserializedBuilding = BuildingProtoConverter.fromMessage(msg)
    deserializedBuilding shouldEqual (building)
  }

  private def getBuilding: Building = {
    val builder: Building.Builder = new Building.Builder
    builder.buildingId = 1L
    builder.address = "Бульвар Менделеева д20"
    builder.latitude = 34f
    builder.longitude = 43f
    builder.metro = List(new Metro(42, TransportType.ON_FOOT, 10), new Metro(12, TransportType.ON_TRANSPORT, 5)).asJava
    builder.schools = List(new SchoolDistance(142, 100, 200), new SchoolDistance(112, 6, 5)).asJava
    builder.buildYear = 2016
    builder.buildingType = BuildingType.MONOLIT
    builder.buildingSeriesId = 6555L
    builder.parks = List(new ParkDistance(12345, 15, 42, GeoPoint.getPoint(54f, 32f))).asJava
    builder.ponds = List(new PondDistance(3445, 145, 412, GeoPoint.getPoint(51f, 32f))).asJava
    builder.airports = List(new AirportDistance(45, 145, 412), new AirportDistance(35, 145, 412)).asJava
    builder.floorsCount = 21
    builder.hasElevator = true
    builder.hasRubbishChute = true
    builder.hasSecurity = false
    builder.isGuarded = null
    builder.expectDemolition = false
    builder.price = Map(
      "k1" -> new PriceInfo(0, 1, 2, 3f),
      "k2" -> new PriceInfo(-1, 34531, 26546, 3456.56f)
    ).asJava
    builder.priceStatistics = BuildingPriceStatistics(
      sellPrice = Some(BuildingPriceStatisticsItem(1000f, 5L, None)),
      sellPriceByRooms = Map(1 -> BuildingPriceStatisticsItem(2000f, 6L, None)),
      rentPriceByRooms = Map(2 -> BuildingPriceStatisticsItem(3000f, 7L, None)),
      profitability = Some(30f)
    )
    builder.priceStatisticsSeries = null
    builder.build
  }

}
