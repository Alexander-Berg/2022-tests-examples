package ru.auto.api.geo

import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.geo.GeoUtils.distance
import ru.auto.api.managers.enrich.enrichers.OfferDistanceEnricher.getGeoPoint
import ru.auto.api.testkit.TestData
import ru.yandex.vertis.mockito.MockitoSupport

class GeoUtilsSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with OptionValues {
  private val geoTree = TestData.tree

  "GeoUtils" should {
    "calculate correct distance" in {
      distance(getGeoPoint(geoTree.region(213).value), getGeoPoint(geoTree.region(213).value)).toInt shouldBe 0
      distance(getGeoPoint(geoTree.region(213).value), getGeoPoint(geoTree.region(21652).value)).toInt shouldBe 21
    }
  }
}
