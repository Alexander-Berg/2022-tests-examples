package ru.yandex.vertis.general.globe.logic.test

import _root_.zio.test.environment.TestEnvironment
import common.clients.geocoder.GeocoderClient
import common.clients.geosuggest.GeosuggestClient
import common.clients.geosuggest.model.SuggestBases.SuggestBase
import common.clients.geosuggest.model.{Position, SuggestBases, SuggestItem}
import common.geobase.model.RegionIds
import common.geobase.model.RegionIds.RegionId
import common.geobase.{GeobaseParser, Tree}
import general.search.offer_count_model.OfferCountSnapshot
import play.api.libs.json.{JsError, JsSuccess, Json}
import ru.yandex.vertis.general.globe.logic.toponym.ToponymManager
import ru.yandex.vertis.general.globe.public.model.District.DistrictId
import ru.yandex.vertis.general.globe.public.model.MetroStation.MetroId
import ru.yandex.vertis.general.globe.public.model._
import ru.yandex.vertis.general.globe.public.model.MetroLine.LineId
import ru.yandex.vertis.general.globe.public.district.{DistrictManager, DistrictSnapshot, LiveDistrictManager}
import ru.yandex.vertis.general.globe.public.metro.{LiveMetroManager, MetroManager, MetroSnapshot}
import ru.yandex.vertis.general.globe.public.region.{
  DefaultRegionOfferCountManager,
  LiveRegionManager,
  RegionManager,
  RegionOfferCountSnapshot,
  RegionSnapshot
}
import yandex.maps.proto.common2.geo_object.geo_object.GeoObject
import yandex.maps.proto.common2.geometry.geometry.Point
import yandex.maps.proto.common2.metadata.metadata.Metadata
import yandex.maps.proto.common2.response.response.Response
import yandex.maps.proto.search.address.address
import yandex.maps.proto.search.geocoder.geocoder.{GeoObjectMetadata, GeocoderProto}
import yandex.maps.proto.search.geocoder_internal.geocoder_internal.{
  GeocoderInternalProto,
  MatchedComponent,
  ToponymInfo
}
import yandex.maps.proto.search.kind.kind.Kind
import yandex.maps.proto.search.kind.kind.Kind.{DISTRICT, METRO_STATION, PROVINCE, STREET}
import zio._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object LiveToponymManagerTest extends DefaultRunnableSpec {

  private val FakeRegionId = RegionId(-18)
  private val PushkinDistrictId = DistrictId(20297)
  private val PushkinDistrictRegionId = RegionId(20297)
  private val PushkinMetroId = MetroId(20343)
  private val PushkinLineId = LineId("2_1")
  private val PushkinCityId = RegionId(10884)
  private val NizhnyNovgorodId = RegionId(47)
  private val ArbatDistrictRegionId = RegionId(117065)
  private val DostoevskayaMetroId = MetroId(20352)
  private val DostoevskayaLineId = LineId("2_4")

  def createGeoObject(name: String, geoid: Int, kind: Kind) =
    GeoObject(
      name = Some(name),
      metadata = Seq(
        Metadata().withExtension(GeocoderProto.gEOOBJECTMETADATA)(
          Some(
            GeoObjectMetadata(address = address.Address(formattedAddress = "NONE"))
              .withExtension(GeocoderInternalProto.tOPONYMINFO)(
                Some(
                  ToponymInfo(
                    geoid = geoid,
                    point = Point(-1, -1),
                    matchedComponent = Seq(MatchedComponent(Some(kind)))
                  )
                )
              )
          )
        )
      )
    )

  def createSuggestItem(title: String, subtitle: String, tags: List[SuggestBase], position: Position, geoId: Long) =
    SuggestItem(
      title,
      subtitle,
      "",
      tags,
      position,
      geoId,
      None
    )

  def setGeocoderResults(geoObjects: GeoObject*) =
    for {
      resultSetter <- ZIO.service[GeocoderResultSetter]
      _ <- resultSetter.setGeocoderResult(ZIO.succeed(Response(reply = Some(GeoObject(geoObject = geoObjects)))))
    } yield ()

  def setGeosuggestResults(suggestItems: SuggestItem*): ZIO[Has[GeousggestResultSetter], Nothing, Unit] =
    for {
      resultSetter <- ZIO.service[GeousggestResultSetter]
      _ <- resultSetter.setSuggestResult(ZIO.succeed(suggestItems))
    } yield ()

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("LiveToponymManager")(
      testM("Suggest toponyms") {
        for {
          resultSetter <- ZIO.service[GeousggestResultSetter]
          _ <- resultSetter.setSuggestResult(
            ZIO.succeed(
              Seq(
                SuggestItem(
                  "метро Пушкинская",
                  "Россия, Санкт-Петербург",
                  "метро Пушкинская, Россия, Санкт-Петербург",
                  List(SuggestBases.MetroBase),
                  Position(0, 0),
                  PushkinMetroId.id,
                  None
                ),
                SuggestItem(
                  "Пушкинская улица",
                  "Россия, Санкт-Петербург",
                  "Пушкинская улица, Санкт-Петербург, Россия ",
                  List(SuggestBases.StreetBase),
                  Position(0, 0),
                  RegionIds.SaintPetersburg.id,
                  Some(RegionIds.SaintPetersburg.id)
                ),
                SuggestItem(
                  "Пушкинский район",
                  "Россия, Санкт-Петербург",
                  "Пушкинский район, Санкт-Петербург, Россия ",
                  List(SuggestBases.DistrictBase),
                  Position(0, 0),
                  PushkinDistrictRegionId.id,
                  None
                ),
                SuggestItem( // should be filtered out
                  "Пушкинский жилой комплекс",
                  "Россия, Санкт-Петербург",
                  "Пушкинский жилой комплекс, Санкт-Петербург, Россия ",
                  List(SuggestBases.DistrictBase),
                  Position(0, 0),
                  PushkinDistrictRegionId.id,
                  Some(PushkinDistrictRegionId.id)
                ),
                SuggestItem(
                  "Пушкин",
                  "Россия, Санкт-Петербург и Ленинградская область",
                  "Пушкин, Санкт-Петербург и Ленинградская область, Россия ",
                  List(SuggestBases.GeobaseCityBase),
                  Position(0, 0),
                  PushkinCityId.id,
                  None
                ),
                SuggestItem( // exceeds limit
                  "Нижний Новгород",
                  "Россия, Нижегородская область",
                  "Нижний Новгород, Нижегородская область, Россия",
                  List(SuggestBases.GeobaseCityBase),
                  Position(0, 0),
                  NizhnyNovgorodId.id,
                  None
                )
              )
            )
          )
          result <- ToponymManager.suggestToponyms(Some(RegionIds.SaintPetersburg), None, "Пушкин", 4)
        } yield assert(result)(hasSize(equalTo(4))) &&
          assert(result)(
            hasAt(0)(
              isSubtype[MetroStationToponym](
                hasField("metroStation.metroId", _.metroStation.metroId, equalTo(PushkinMetroId))
              )
            )
          ) &&
          assert(result)(
            hasAt(1)(
              isSubtype[AddressToponym](
                hasField("address.name", _.address.name, equalTo("Пушкинская ул."))
              )
            )
          ) &&
          assert(result)(
            hasAt(2)(
              isSubtype[DistrictToponym](
                hasField("district.districtId", _.district.districtId, equalTo(PushkinDistrictId))
              )
            )
          ) &&
          assert(result)(
            hasAt(3)(
              isSubtype[RegionToponym](
                hasField("region.id", _.region.id, equalTo(PushkinCityId.id))
              )
            )
          )
      },
      testM("Reverse geocoding returns Russia if Geocoder returns empty result") {
        for {
          resultSetter <- ZIO.service[GeocoderResultSetter]
          _ <- resultSetter.setGeocoderResult(ZIO.succeed(Response()))
          toponym <- ToponymManager.reverseGeocode(GeoPosition(4, 5))
        } yield assert(toponym)(
          isSubtype[RegionToponym](
            hasField[RegionToponym, Long]("region.id", _.region.id, equalTo(RegionIds.Russia.id)) &&
              hasField[RegionToponym, String]("region.name", _.region.ruName, equalTo("Россия")) &&
              hasField[RegionToponym, Double]("region.latitude", _.region.latitude, equalTo(4d)) &&
              hasField[RegionToponym, Double]("region.longitude", _.region.longitude, equalTo(5d))
          )
        )
      },
      testM("Reverse geocoding fails if a query to Geocoder fails") {
        for {
          resultSetter <- ZIO.service[GeocoderResultSetter]
          _ <- resultSetter.setGeocoderResult(ZIO.fail(new RuntimeException()))
          toponym <- ToponymManager.reverseGeocode(GeoPosition(4, 5)).run
        } yield assert(toponym)(dies(anything))
      },
      testM("Geocoding maps geocoder results to globe results") {
        for {
          _ <- setGeocoderResults(
            createGeoObject("Москва", 213, PROVINCE),
            createGeoObject("Ново-Переделкино", 117001, DISTRICT),
            createGeoObject("метро Пионерская", 20322, METRO_STATION),
            createGeoObject("Новоперелкинская", 213, STREET)
          )
          allToponyms <- ToponymManager.geocode("текст")

          _ <- setGeocoderResults(
            createGeoObject("Москва", -1, PROVINCE),
            createGeoObject("Ново-Переделкино", -1, DISTRICT),
            createGeoObject("метро Пионерская", -1, METRO_STATION),
            createGeoObject("Новоперелкинская", -1, STREET)
          )
          streetOnly <- ToponymManager.geocode("текст")

          _ <- setGeocoderResults(
            createGeoObject("Москва", -1, PROVINCE),
            createGeoObject("Ново-Переделкино", -1, DISTRICT),
            createGeoObject("метро Пионерская", -1, METRO_STATION)
          )
          moscowAsAddress <- ToponymManager.geocode("текст")

        } yield assert(allToponyms)(
          hasAt(0)(
            isSubtype[RegionToponym](hasField("id", _.region.id, equalTo(213L)))
          ) &&
            hasAt(1)(
              isSubtype[DistrictToponym](hasField("districtId", _.district.districtId, equalTo(DistrictId(117001))))
            ) &&
            hasAt(2)(
              isSubtype[MetroStationToponym](hasField("metroId", _.metroStation.metroId, equalTo(MetroId(20322))))
            ) &&
            hasAt(3)(
              isSubtype[AddressToponym](hasField("name", _.address.name, equalTo("Новоперелкинская")))
            ) &&
            hasSize(equalTo(4))
        ) && assert(streetOnly)(
          equalTo(Seq(AddressToponym(Address(RegionId(-1), "Новоперелкинская", GeoPosition(-1, -1)))))
        ) && assert(moscowAsAddress)(
          equalTo(Seq(AddressToponym(Address(RegionId(-1), "Москва", GeoPosition(-1, -1)))))
        )
      },
      testM("Reverse geocoding returns first result from Geocoder") {
        for {
          resultSetter <- ZIO.service[GeocoderResultSetter]
          groznyGeoObject = GeoObject(
            name = Some("Грозный"),
            metadata = Seq(
              Metadata().withExtension(GeocoderProto.gEOOBJECTMETADATA)(
                Some(
                  GeoObjectMetadata(address = address.Address(formattedAddress = "NONE"))
                    .withExtension(GeocoderInternalProto.tOPONYMINFO)(
                      Some(ToponymInfo(geoid = 101010, point = Point(-1, -1)))
                    )
                )
              )
            )
          )
          _ <- resultSetter.setGeocoderResult(
            ZIO.succeed(Response(reply = Some(GeoObject(geoObject = Seq(groznyGeoObject)))))
          )
          toponym <- ToponymManager.reverseGeocode(GeoPosition(4, 5))
        } yield assert(toponym)(
          equalTo(AddressToponym(Address(parentId = RegionId(101010), name = "Грозный", position = GeoPosition(4, 5))))
        )
      },
      testM("Get description for regions") {
        for {
          descriptions <- ToponymManager.getRegionDescriptions(
            List(RegionIds.Moscow, RegionIds.MoscowAndMoscowRegion, NizhnyNovgorodId)
          )
        } yield assert(descriptions)(equalTo(Seq("Россия", "Россия", "Нижегородская область")))
      },
      testM("Get description for disctricts") {
        for {
          descriptions <- ToponymManager.getDistrictDescription(List(PushkinDistrictRegionId, ArbatDistrictRegionId))
        } yield assert(descriptions)(
          equalTo(Seq("район, Санкт-Петербург", "район, Центральный административный округ, Москва"))
        )
      },
      testM("Get description for metro") {
        for {
          descriptions <- ToponymManager.getMetroDescription(
            List((PushkinMetroId, PushkinLineId), (DostoevskayaMetroId, DostoevskayaLineId))
          )
        } yield assert(descriptions)(
          equalTo(
            Seq(
              "метро, 1 линия, Адмиралтейский район, Санкт-Петербург",
              "метро, 4 линия, Центральный район, Санкт-Петербург"
            )
          )
        )
      },
      testM("Get description for address") {
        for {
          descriptions <- ToponymManager.getAddressDescription(List(RegionIds.Moscow, NizhnyNovgorodId))
        } yield assert(descriptions)(
          equalTo(Seq("Москва", "Нижний Новгород, Нижегородская область"))
        )
      },
      testM("Get position info") {
        for {
          _ <- setGeocoderResults(
            createGeoObject("Правильная улица (Санкт-Петербург)", 2, STREET),
            createGeoObject("Калининский район", 20283, DISTRICT),
            createGeoObject("Гражданка", 120619, DISTRICT)
          )
          positionInfo <- ToponymManager.getPositionInfo(GeoPosition(60.0125, 30.395))
        } yield assert(positionInfo.nearestMetroStation)(
          isSome(
            equalTo(
              MetroStation(
                metroId = MetroId(20327),
                regionId = RegionId(20327),
                position = GeoPosition(60.012746, 30.396079),
                name = "Академическая",
                lines = List(MetroLine(LineId("2_1"), "Кировско-Выборгская линия", Some("#f03d2f")))
              )
            )
          )
        ) && assert(positionInfo.region.id)(equalTo(2L)) &&
          assert(positionInfo.district.map(_.districtId))(isSome(equalTo(DistrictId(20283))))
      },
      testM("suggest metro with hack from CLASSBACK-1079") {
        val position = Position(60.050339d, 30.442616d)
        val equalItems = List(
          createSuggestItem("Девяткино", "", List(SuggestBases.MetroBase), position, 20325),
          createSuggestItem("Мурино", "родитель Девяткино", List(SuggestBases.MetroBase), position, 118936)
        )

        ZIO
          .foreach(equalItems) { item =>
            for {
              _ <- setGeosuggestResults(item)
              suggest <- ToponymManager.suggestToponyms(None, None, "девяткино", 1)
              metro <- MetroManager.getStationById(MetroId(20325))
            } yield {
              assert(suggest)(hasSize(equalTo(1))) &&
              assert(suggest)(forall(equalTo(MetroStationToponym(metro))))
            }
          }
          .map(_.reduce(_ && _))
      }
    ).provideCustomLayerShared {
      val regions = GeobaseParser.parse(LiveToponymManagerTest.getClass.getResourceAsStream("/regions"))
      val tree = new Tree(regions)
      val metroJson = Json.parse(LiveToponymManagerTest.getClass.getResourceAsStream("/metro"))
      val metro = Json.fromJson[MetroRawCitiesStorage](metroJson) match {
        case JsSuccess(value, _) => Ref.make(MetroSnapshot(value))
        case JsError(errors) => throw new RuntimeException(s"Can't parse metro json: $errors")
      }
      val regionOfferCountSnapshot = Ref.make(RegionOfferCountSnapshot(OfferCountSnapshot()))
      val offerCountManager = regionOfferCountSnapshot.map(new DefaultRegionOfferCountManager(_))

      val districtSnapshot = Ref.make(DistrictSnapshot(tree))
      val regionSnapshot = Ref.make(RegionSnapshot(tree))

      val districtManagerEffect: UIO[DistrictManager.Service] = districtSnapshot.map(new LiveDistrictManager(_))
      val regionManagerEffect: UIO[RegionManager.Service] = {
        for {
          t <- regionSnapshot
          oc <- offerCountManager
        } yield new LiveRegionManager(t, oc)
      }
      val metroManagerEffect: UIO[MetroManager.Service] = {
        for {
          t <- Ref.make(tree)
          m <- metro
        } yield new LiveMetroManager(t, m)
      }

      val geosuggestClientEffect = for {
        effectRef <- ZRef.make[Task[Seq[SuggestItem]]](ZIO.succeed(Seq.empty[SuggestItem]))
        client: GeosuggestClient.Service = new GeosuggestClient.Service {
          override def suggest(
              text: String,
              geoIds: Seq[Long],
              position: Option[Position],
              bases: Seq[SuggestBases.SuggestBase],
              limit: Int): Task[Seq[SuggestItem]] = effectRef.get.flatten
        }
        resultSetter: GeousggestResultSetter = new GeousggestResultSetter {
          override def setSuggestResult(result: Task[Seq[SuggestItem]]): UIO[Unit] = effectRef.set(result)
        }
      } yield Has.allOf(client, resultSetter)
      val geosuggestClientLayer = geosuggestClientEffect.toLayerMany

      val geocoderClientEffect = for {
        effectRef <- ZRef.make[Task[Response]](ZIO.succeed(Response()))
        client: GeocoderClient.Service = new GeocoderClient.Service {
          override def reverseGeocode(latitude: Double, longitude: Double): Task[Response] = effectRef.get.flatten
          override def geocode(text: String, limit: Int): Task[Response] = effectRef.get.flatten
        }
        resultSetter: GeocoderResultSetter = new GeocoderResultSetter {
          override def setGeocoderResult(result: Task[Response]): UIO[Unit] = effectRef.set(result)
        }
      } yield Has.allOf(client, resultSetter)
      val geocoderClientLayer = geocoderClientEffect.toLayerMany

      (districtManagerEffect.toLayer ++
        regionManagerEffect.toLayer ++
        metroManagerEffect.toLayer ++
        geosuggestClientLayer ++
        geocoderClientLayer) >+> ToponymManager.live
    } @@ sequential

  trait GeousggestResultSetter {
    def setSuggestResult(result: Task[Seq[SuggestItem]]): UIO[Unit]
  }

  trait GeocoderResultSetter {
    def setGeocoderResult(result: Task[Response]): UIO[Unit]
  }
}
