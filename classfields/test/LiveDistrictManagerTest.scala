package ru.yandex.vertis.general.globe.public.test

import common.geobase.model.RegionIds
import common.geobase.{GeobaseParser, Tree}
import ru.yandex.vertis.general.globe.public.model.District.DistrictId
import common.geobase.model.RegionIds.RegionId
import ru.yandex.vertis.general.globe.public.model.RegionNotFound
import ru.yandex.vertis.general.globe.public.district.{DistrictManager, DistrictSnapshot, LiveDistrictManager}
import zio.Ref
import zio.test.Assertion._
import zio.test._

object LiveDistrictManagerTest extends DefaultRunnableSpec {

  private val CaoId = RegionId(20279)
  private val ArbatId = DistrictId(117065)
  private val CentralDistrictId = DistrictId(20292)
  private val FakeRegionId = RegionId(-18)
  private val CentralFederalDistrictId = RegionId(3)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("LiveDistrictManager")(
      testM("List all districts for Moscow and Moscow-with-Moscow-region") {
        for {
          moscowDistricts <- DistrictManager.listDistricts(RegionIds.Moscow)
          moscowAndMoscowRegionDistricts <- DistrictManager.listDistricts(RegionIds.MoscowAndMoscowRegion)
        } yield assert(moscowAndMoscowRegionDistricts)(equalTo(moscowDistricts)) &&
          assert(moscowDistricts.map(_.regionId))(not(contains(CaoId))) && // does not contain first-level districts
          assert(moscowDistricts.map(_.districtId))(contains(ArbatId)) && // contains second-level districts
          assert(moscowDistricts)(hasSize(equalTo(144)))
      },
      testM("List all districts for Saint-Petersburg and Saint-Petersburg-with-Leningrad-oblast") {
        for {
          saintPetersburgDistricts <- DistrictManager.listDistricts(RegionIds.SaintPetersburg)
          saintPetersburgAndLeningradOblastDistricts <-
            DistrictManager.listDistricts(RegionIds.SaintPetersburgAndLeningradOblast)
        } yield assert(saintPetersburgAndLeningradOblastDistricts)(equalTo(saintPetersburgDistricts)) &&
          assert(saintPetersburgDistricts.map(_.districtId))(contains(CentralDistrictId)) &&
          assert(saintPetersburgDistricts)(hasSize(equalTo(18)))
      },
      testM("Fail to list all districts for incorrect region id") {
        for {
          districts <- DistrictManager.listDistricts(FakeRegionId).run
        } yield assert(districts)(fails(equalTo(RegionNotFound(FakeRegionId))))
      },
      testM("List 0 districts if region is not a city") {
        for {
          districts <- DistrictManager.listDistricts(CentralFederalDistrictId)
        } yield assert(districts)(hasSize(equalTo(0)))
      },
      testM("Suggest districts with limit") {
        for {
          result <- DistrictManager.suggestDistricts(RegionIds.Moscow, "К", 1)
        } yield assert(result)(hasSize(equalTo(1)))
      },
      testM("Suggest districts") {
        for {
          normalSuggest <- DistrictManager.suggestDistricts(RegionIds.SaintPetersburg, "Центральный", 10)
          incompleteSuggest <- DistrictManager.suggestDistricts(RegionIds.SaintPetersburg, "Центральн", 10)
          englishLayoutSuggest <- DistrictManager.suggestDistricts(RegionIds.SaintPetersburg, "Wtynhfkmysq", 10)
          whitespaceSuggest <- DistrictManager.suggestDistricts(RegionIds.SaintPetersburg, "   Центральный     ", 10)
          wrongCaseSuggest <- DistrictManager.suggestDistricts(RegionIds.SaintPetersburg, "цЕнТрАльнЫЙ", 10)
          allTyposSuggest <- DistrictManager.suggestDistricts(RegionIds.SaintPetersburg, "  wTyNhFkMy  ", 10)
        } yield assert(normalSuggest.map(_.districtId))(equalTo(Seq(CentralDistrictId))) &&
          assert(incompleteSuggest)(equalTo(normalSuggest)) &&
          assert(englishLayoutSuggest)(equalTo(normalSuggest)) &&
          assert(whitespaceSuggest)(equalTo(normalSuggest)) &&
          assert(wrongCaseSuggest)(equalTo(normalSuggest)) &&
          assert(allTyposSuggest)(equalTo(normalSuggest))
      },
      testM("Fail to suggest districts for incorrect region id") {
        for {
          result <- DistrictManager.suggestDistricts(FakeRegionId, "a", 1).run
        } yield assert(result)(fails(equalTo(RegionNotFound(FakeRegionId))))
      }
    ).provideCustomLayerShared {
      val regions = GeobaseParser.parse(LiveDistrictManagerTest.getClass.getResourceAsStream("/regions"))
      val tree = Ref.make(DistrictSnapshot(new Tree(regions)))
      tree.map(new LiveDistrictManager(_)).toLayer[DistrictManager.Service]
    }
}
