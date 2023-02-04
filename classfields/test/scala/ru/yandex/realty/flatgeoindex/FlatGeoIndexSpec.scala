package ru.yandex.realty.flatgeoindex

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.location.GeoPoint

@RunWith(classOf[JUnitRunner])
class FlatGeoIndexSpec extends SpecBase with FlatGeoIndexSpecComponents {

  private val nearGeoPoint = new GeoPoint(60.15f, 60.15f)

  private val farGeoPoint = new GeoPoint(65.015f, 65.015f)

  "PondIndex in getNearbyElements" should {
    "return not empty list if there are nearby ponds" in {
      pondIndex.getNearbyElements(nearGeoPoint) shouldEqual nearbyPonds
    }

    "return empty list if there are not nearby ponds" in {
      pondIndex.getNearbyElements(farGeoPoint).size shouldEqual 0
    }
  }

  "ParkIndex in getNearbyElements" should {
    "return not empty list if there are nearby parks" in {
      parkIndex.getNearbyElements(nearGeoPoint) shouldEqual nearbyParks
    }

    "return empty list if there are not nearby parks" in {
      parkIndex.getNearbyElements(farGeoPoint).size shouldEqual 0
    }
  }
}
