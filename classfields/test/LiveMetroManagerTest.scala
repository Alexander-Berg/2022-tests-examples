package ru.yandex.vertis.general.globe.public.test

import common.geobase.{GeobaseParser, Tree}
import common.geobase.model.RegionIds
import common.geobase.model.RegionIds.RegionId
import play.api.libs.json.{JsError, JsSuccess, Json}
import ru.yandex.vertis.general.globe.public.model.MetroLine.LineId
import ru.yandex.vertis.general.globe.public.model.MetroStation.MetroId
import ru.yandex.vertis.general.globe.public.model.{
  GeoPosition,
  MetroLine,
  MetroRawCitiesStorage,
  MetroStation,
  RegionNotFound
}
import ru.yandex.vertis.general.globe.public.metro.{LiveMetroManager, MetroManager, MetroSnapshot}
import zio.Ref
import zio.test.Assertion.{equalTo, fails, hasSize}
import zio.test.{assert, assertM, DefaultRunnableSpec, ZSpec}

object LiveMetroManagerTest extends DefaultRunnableSpec {

  private val FakeRegionId = RegionId(-18)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("LiveMetroManager")(
      testM("Suggest metro stations with limit") {
        for {
          result <- MetroManager.suggestStations(RegionIds.SaintPetersburg, "П", 1)
        } yield assert(result)(hasSize(equalTo(1)))
      },
      testM("Suggest metro stations") {
        for {
          normalSuggest <- MetroManager.suggestStations(RegionIds.SaintPetersburg, "Пионерская", 10)
          incompleteSuggest <- MetroManager.suggestStations(RegionIds.SaintPetersburg, "Пионерск", 10)
          englishLayoutSuggest <- MetroManager.suggestStations(RegionIds.SaintPetersburg, "Gbjythcrfz", 10)
          whitespaceSuggest <- MetroManager.suggestStations(RegionIds.SaintPetersburg, "   Пионерская     ", 10)
          wrongCaseSuggest <- MetroManager.suggestStations(RegionIds.SaintPetersburg, "пИоНеРсКАЯ", 10)
          allTyposSuggest <- MetroManager.suggestStations(RegionIds.SaintPetersburg, "  gBjYtHCr  ", 10)
        } yield assert(normalSuggest.map(_.name))(equalTo(Seq("Пионерская", "Пионерская"))) &&
          assert(normalSuggest.map(_.lines))(
            equalTo(
              Seq(
                Seq(MetroLine(LineId("2_2"), "Московско-Петроградская линия", Some("#16bdf0"))),
                Seq(MetroLine(LineId("2_5"), "Фрунзенско-Приморская линия", Some("#c063d1")))
              )
            )
          ) &&
          assert(incompleteSuggest)(equalTo(normalSuggest)) &&
          assert(englishLayoutSuggest)(equalTo(normalSuggest)) &&
          assert(whitespaceSuggest)(equalTo(normalSuggest)) &&
          assert(wrongCaseSuggest)(equalTo(normalSuggest)) &&
          assert(allTyposSuggest)(equalTo(normalSuggest))
      },
      testM("Fail to suggest metro stations for incorrect region id") {
        for {
          result <- MetroManager.suggestStations(FakeRegionId, "a", 1).run
        } yield assert(result)(fails(equalTo(RegionNotFound(FakeRegionId))))
      },
      testM("Get metro station by id") {
        for {
          result <- MetroManager.getStationById(MetroId(20322))
        } yield assert(result)(
          equalTo(
            MetroStation(
              MetroId(20322),
              RegionId(20322),
              GeoPosition(60.002517, 30.296662),
              "Пионерская",
              Seq(
                MetroLine(LineId("2_2"), "Московско-Петроградская линия", Some("#16bdf0")),
                MetroLine(LineId("2_5"), "Фрунзенско-Приморская линия", Some("#c063d1"))
              )
            )
          )
        )
      },
      testM("List all metro stations in given region") {
        assertM(MetroManager.listStations(RegionIds.SaintPetersburg))(hasSize(equalTo(73)))
      }
    ).provideCustomLayerShared {
      val regions = GeobaseParser.parse(LiveMetroManagerTest.getClass.getResourceAsStream("/regions"))
      val tree = Ref.make(new Tree(regions))
      val metroJson = Json.parse(LiveMetroManagerTest.getClass.getResourceAsStream("/metro"))
      val metro = Json.fromJson[MetroRawCitiesStorage](metroJson) match {
        case JsSuccess(value, _) => Ref.make(MetroSnapshot(value))
        case JsError(errors) => throw new RuntimeException(s"Can't parse metro json: $errors")
      }
      (for {
        t <- tree
        m <- metro
      } yield new LiveMetroManager(t, m)).toLayer[MetroManager.Service]
    }
}
