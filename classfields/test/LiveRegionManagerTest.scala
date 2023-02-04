package ru.yandex.vertis.general.globe.public.test

import common.geobase.{GeobaseParser, Tree}
import common.geobase.model.RegionIds
import common.geobase.model.RegionIds.RegionId
import general.search.offer_count_model.{OfferCountByCategory, OfferCountSnapshot}
import ru.yandex.vertis.general.globe.public.model.{GeoPosition, RegionNotFound, RegionSort}
import ru.yandex.vertis.general.globe.public.region.RegionManager
import zio.{Ref, ZIO}
import zio.test.Assertion.{contains, equalTo, fails, hasSize, not}
import zio.test.{assert, DefaultRunnableSpec, ZSpec}
import ru.yandex.vertis.general.globe.public.region._

object LiveRegionManagerTest extends DefaultRunnableSpec {

  private val SaintPetersburg = "Санкт-Петербург"
  private val Moscow = "Москва"
  private val VelikyNovgorod = "Великий Новгород"
  private val NizhnyNovgorod = "Нижний Новгород"
  private val MoscowAndMoscowRegion = "Москва и Московская область"
  private val Earth = "Земля"
  private val Russia = "Россия"
  private val CentralFederalDistrictId = RegionId(3)
  private val EarthId = RegionId(10000)
  private val PaveletskayaId = RegionId(20475)
  private val CaoId = RegionId(20279)
  private val FakeRegionId = RegionId(-18)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("LiveRegionManager")(
      testM("List near regions") {
        for {
          vyborgPosition <- ZIO.succeed(GeoPosition(60.7096, 28.7490))
          regions150km <- RegionManager.listNearRegions(vyborgPosition, 150, 1000)
          regions300km <- RegionManager.listNearRegions(vyborgPosition, 300, 1000)
          regions150kmNameSet = regions150km.map(_.ruName).toSet
          regions300kmNameSet = regions300km.map(_.ruName).toSet
        } yield assert(regions150kmNameSet)(contains(SaintPetersburg)) &&
          assert(regions150kmNameSet)(not(contains(VelikyNovgorod))) &&
          assert(regions300kmNameSet)(contains(SaintPetersburg)) &&
          assert(regions300kmNameSet)(contains(VelikyNovgorod))
      },
      testM("List near regions with limit") {
        for {
          vyborgPosition <- ZIO.succeed(GeoPosition(60.7096, 28.7490))
          regions <- RegionManager.listNearRegions(vyborgPosition, 7000, 2)
        } yield assert(regions)(hasSize(equalTo(2)))
      },
      testM("List chief cities") {
        for {
          chiefCities <- RegionManager.listChiefCities(RegionSort.ByNameAsc, None)
          names = chiefCities.map(_.ruName)
        } yield assert(names)(equalTo(Seq(Moscow, SaintPetersburg, NizhnyNovgorod)))
      },
      testM("Suggest regions with same suffix") {
        for {
          suggestResult <- RegionManager.suggestRegions("Новгород", 1000)
          suggestedNames = suggestResult.map(_.ruName)
        } yield assert(suggestedNames)(contains(VelikyNovgorod)) &&
          assert(suggestedNames)(contains(NizhnyNovgorod))
      },
      testM("Suggest regions with limit") {
        for {
          suggestResult <- RegionManager.suggestRegions("Новгород", 1)
        } yield assert(suggestResult)(hasSize(equalTo(1)))
      },
      testM("Suggest regions") {
        for {
          normalSuggest <- RegionManager.suggestRegions("Сестрорецк", 10)
          incompleteSuggest <- RegionManager.suggestRegions("Сестроре", 10)
          englishLayoutSuggest <- RegionManager.suggestRegions("Ctcnhjhtwr", 10)
          whitespaceSuggest <- RegionManager.suggestRegions("   Сестрорецк     ", 10)
          wrongCaseSuggest <- RegionManager.suggestRegions("сЕсТрОРЕцк", 10)
          allTyposSuggest <- RegionManager.suggestRegions("  cTcNhJhTw  ", 10)
        } yield assert(normalSuggest.map(_.id))(equalTo(Seq(102557L))) &&
          assert(incompleteSuggest)(equalTo(normalSuggest)) &&
          assert(englishLayoutSuggest)(equalTo(normalSuggest)) &&
          assert(whitespaceSuggest)(equalTo(normalSuggest)) &&
          assert(wrongCaseSuggest)(equalTo(normalSuggest)) &&
          assert(allTyposSuggest)(equalTo(normalSuggest))
      },
      testM("Get parent regions") {
        for {
          parentRegions <-
            RegionManager.getParentRegions(Seq(RegionIds.Moscow, RegionIds.MoscowAndMoscowRegion, EarthId))
          parentIds = parentRegions.map(_.map(_.id))
        } yield assert(parentIds)(equalTo(Seq(Some(RegionIds.MoscowAndMoscowRegion.id), Some(3), None)))
      },
      testM("Fail to get parent regions if incorrect id is present") {
        for {
          parentRegions <-
            RegionManager
              .getParentRegions(Seq(RegionIds.Moscow, RegionIds.MoscowAndMoscowRegion, EarthId, FakeRegionId))
              .run
        } yield assert(parentRegions)(fails(equalTo(RegionNotFound(FakeRegionId))))
      },
      testM("Get regions") {
        for {
          parentRegions <- RegionManager.getRegions(Seq(RegionIds.Moscow, RegionIds.MoscowAndMoscowRegion, EarthId))
          parentIds = parentRegions.map(_.ruName)
        } yield assert(parentIds)(equalTo(Seq(Moscow, MoscowAndMoscowRegion, Earth)))
      },
      testM("Fail to get regions if incorrect id is present") {
        for {
          parentRegions <-
            RegionManager
              .getRegions(Seq(RegionIds.Moscow, RegionIds.MoscowAndMoscowRegion, EarthId, FakeRegionId))
              .run
        } yield assert(parentRegions)(fails(equalTo(RegionNotFound(FakeRegionId))))
      },
      testM("Get searchable regions") {
        for {
          parentRegions <- RegionManager.getSearchableRegions(
            Seq(
              RegionIds.Moscow,
              RegionIds.MoscowAndMoscowRegion,
              EarthId,
              PaveletskayaId,
              CaoId,
              CentralFederalDistrictId
            ),
            filterEmptyRegions = true
          )
          parentIds = parentRegions.map(_.ruName)
        } yield assert(parentIds)(equalTo(Seq(Moscow, MoscowAndMoscowRegion, Russia, Moscow, Moscow, Russia)))
      },
      testM("Fail to get searchable regions if incorrect id is present") {
        for {
          parentRegions <-
            RegionManager
              .getSearchableRegions(
                Seq(RegionIds.Moscow, RegionIds.MoscowAndMoscowRegion, EarthId, FakeRegionId),
                filterEmptyRegions = true
              )
              .run
        } yield assert(parentRegions)(fails(equalTo(RegionNotFound(FakeRegionId))))
      }
    ).provideCustomLayerShared {
      val regions = GeobaseParser.parse(LiveRegionManagerTest.getClass.getResourceAsStream("/regions"))
      val tree = Ref.make(RegionSnapshot(new Tree(regions)))
      val offerCountRef = Ref.make(
        RegionOfferCountSnapshot(
          OfferCountSnapshot(
            Map(
              RegionIds.MoscowAndMoscowRegion.id -> OfferCountByCategory(Map("" -> 100)),
              RegionIds.Moscow.id -> OfferCountByCategory(Map("" -> 100)),
              RegionIds.SaintPetersburg.id -> OfferCountByCategory(Map("" -> 100)),
              RegionIds.NizhnyNovgorod.id -> OfferCountByCategory(Map("" -> 100)),
              RegionIds.Russia.id -> OfferCountByCategory(Map("" -> 100))
            )
          )
        )
      )

      val offerCountManager = offerCountRef.map(new DefaultRegionOfferCountManager(_))

      (for {
        t <- tree
        oc <- offerCountManager
      } yield new LiveRegionManager(t, oc)).toLayer[RegionManager.Service]
    }
}
