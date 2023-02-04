package ru.auto.salesman.service.geoservice

import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.model.geo.{Region, RegionTypes}
import ru.auto.salesman.service.geoservice.impl.RegionServiceImpl
import zio.{Ref}
import ru.auto.salesman.model.RegionId

class RegionServiceImplSpec extends BaseSpec {

  val worldRegion = Region(
    id = 1L,
    parentId = 0,
    `type` = RegionTypes.Other,
    ruName = "World",
    tzOffset = 0
  )

  val russiaRegion = Region(
    id = 2L,
    parentId = 1L,
    `type` = RegionTypes.Country,
    ruName = "Russia",
    tzOffset = 0
  )

  val kemerovoRegion = Region(
    id = 3L,
    parentId = 2L,
    `type` = RegionTypes.FederalDistrict,
    ruName = "Kemerovo obl",
    tzOffset = 0
  )

  val cityKemerovo = Region(
    id = 4L,
    parentId = 3L,
    `type` = RegionTypes.City,
    ruName = "Kemerovo",
    tzOffset = 0
  )

  val regions = List(
    worldRegion,
    russiaRegion,
    kemerovoRegion,
    cityKemerovo
  )

  val regionsMap: Ref[Map[Long, Region]] =
    Ref.make(regions.map(region => region.id -> region).toMap).unsafeRun()
  val service = new RegionServiceImpl(regionsMap)

  "RegionServiceImpl" should {
    "getRegion" should {
      "should return region by id" in {
        service.getRegion(4L).success.value shouldBe Some(cityKemerovo)
      }

      "should return None if region not found in map" in {
        service.getRegion(42L).success.value shouldBe None
      }
    }
    "expandGeoIds" should {
      "should return empty list if region not found" in {
        service.expandGeoIds(RegionId(77)).success.value shouldBe Nil
      }

      "should retudn regions path if region found" in {
        service.expandGeoIds(RegionId(4L)).success.value shouldBe (List(
          RegionId(4L),
          RegionId(3L),
          RegionId(2L),
          RegionId(1L)
        ))
      }
    }
  }
}
