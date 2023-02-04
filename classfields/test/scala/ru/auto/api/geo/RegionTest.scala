package ru.auto.api.geo

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers._
import org.scalatest.funsuite.AnyFunSuite

class RegionTest extends AnyFunSuite with OptionValues {

  val moscow = Region(213, 1, RegionTypes.City, "Москва", "", "", "", "", "", 55.753215, 37.622504, 3, None)
  val piter = Region(2, 10174, RegionTypes.City, "Санкт-Петербург", "", "", "", "", "", 59.938951, 30.315635, 3, None)
  val samara = Region(51, 11131, RegionTypes.City, "Самара", "", "", "", "", "", 53.195538, 50.101783, 4, None)

  test("getDistanceBetweenLocations") {
    Region.getDistanceBetweenLocations(
      moscow.longitude,
      moscow.latitude,
      piter.longitude,
      piter.latitude
    ) should (be > 600000.0 and be < 800000.0) // 634 км

    Region.getDistanceBetweenLocations(
      moscow.longitude,
      moscow.latitude,
      samara.longitude,
      samara.latitude
    ) should (be > 800000.0 and be < 900000.0) // 853 км

    Region.getDistanceBetweenLocations(
      piter.longitude,
      piter.latitude,
      samara.longitude,
      samara.latitude
    ) should (be > 1400000.0 and be < 1500000.0) // 1417 км
  }

  test("neighborhoodContains") {
    moscow.neighborhoodContains(700, piter.latitude, piter.longitude) shouldBe true
    moscow.neighborhoodContains(450, piter.latitude, piter.longitude) shouldBe false

    moscow.neighborhoodContains(900, samara.latitude, samara.longitude) shouldBe true
    moscow.neighborhoodContains(700, samara.latitude, samara.longitude) shouldBe false
  }

  test("make lan-lon square by radius") {
    val radius = 500
    Region.calcLatFrom(moscow.latitude, radius) should (be > 51.0)
    Region.calcLatTo(moscow.latitude, radius) should (be > 60.0)
    Region.calcLonFrom(moscow.longitude, moscow.latitude, radius) should (be > 29.0)
    Region.calcLonTo(moscow.longitude, moscow.latitude, radius) should (be > 45.0)
  }
}
