package ru.yandex.realty.misc.servantlets.autocomplete

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.commercial.CommercialBuildingStorage
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.geo.RegionGraphTestComponents.{EkaterinburgCityNode, SpbNode}
import ru.yandex.realty.misc.UserInputDictionary
import ru.yandex.realty.misc.action.deeplink.EntryType
import ru.yandex.realty.misc.servantlets.autocomplete.MobileAutocompleteResponseBuilder.EntryBuilder.mobileAutocompleteEntry
import ru.yandex.realty.model.location.GeoPoint
import ru.yandex.realty.model.message.ExtDataSchema.RailwayStation
import ru.yandex.realty.regions.RegionNamesTranslations
import ru.yandex.realty.util.geo.GeoHelper
import ru.yandex.realty.index.RegionDocumentsStatistics
import ru.yandex.realty.microdistricts.MicroDistrictsTestComponents
import ru.yandex.realty.model.message.RealtySchema.GeoPointMessage
import ru.yandex.realty.storage.RegionDocumentsStatisticsStorage

import java.util

@RunWith(classOf[JUnitRunner])
class MobileAutocompleteResponseBuilderSpec extends SpecBase with RegionGraphTestComponents {
  private val geoHelperProvider = mock[Provider[GeoHelper]]
  private val commercialBuildingStorageProvider = mock[Provider[CommercialBuildingStorage]]
  private val regionNamesTranslationsProvider = mock[Provider[RegionNamesTranslations]]
  private val regionDocumentsStatisticsProvider = mock[Provider[RegionDocumentsStatisticsStorage]]
  private val responseBuilder = new MobileAutocompleteResponseBuilder(
    regionGraphProvider,
    geoHelperProvider,
    commercialBuildingStorageProvider,
    regionNamesTranslationsProvider
  )
  private val populatedRgids: java.util.Set[java.lang.Long] = new java.util.HashSet()
  populatedRgids.add(SpbNode.getId)
  populatedRgids.add(EkaterinburgCityNode.getId)

  "MobileAutocompleteResponseBuilder" should {
    "correctly build micro district response" in {
      val microDistrict = MicroDistrictsTestComponents.gbi

      val searchParams = new util.HashMap[String, util.List[String]]
      searchParams.put(
        UserInputDictionary.ToponymId,
        java.util.Collections.singletonList(String.valueOf(microDistrict.id))
      )

      mockAll()

      val expected = mobileAutocompleteEntry
        .name("микрорайон ЖБИ")
        .shortName("мкр. ЖБИ")
        .fullName("микрорайон ЖБИ")
        .scope("Свердловская область, Россия")
        .address("Свердловская область, город Екатеринбург")
        .point(GeoPoint.getPoint(56.834343f, 60.688816f))
        .lt(GeoPoint.getPoint(56.852894f, 60.667824f))
        .rb(GeoPoint.getPoint(56.81485f, 60.743317f))
        .`type`(EntryType.TOPONYM)
        .countryGeoId(225)
        .rgid(559132)
        .changeRegion(false)
        .searchParams(searchParams)
        .build
      val result = responseBuilder.convert(microDistrict)
      result shouldEqual expected
    }

    "correctly build railway station response" in {
      val railwayStation =
        RailwayStation
          .newBuilder()
          .setEsr(38205)
          .setTitle("Санкт-Петербург (Финляндский вокзал)")
          .setPoint(GeoPointMessage.newBuilder().setLatitude(59.9562577015f).setLongitude(30.3571142897f))
          .build()

      val searchParams = new util.HashMap[String, util.List[String]]
      searchParams.put(
        UserInputDictionary.RailwayStationEsr,
        java.util.Collections.singletonList(String.valueOf(38205))
      )

      mockAll()

      val expected = mobileAutocompleteEntry
        .name("ж/д станция Санкт-Петербург (Финляндский вокзал)")
        .shortName("Санкт-Петербург (Финляндский вокзал)")
        .fullName("ж/д станция Санкт-Петербург (Финляндский вокзал)")
        .scope("")
        .address("")
        .point(GeoPoint.getPoint(59.9562577015f, 30.3571142897f))
        .lt(GeoPoint.getPoint(59.965256f, 30.339142f))
        .rb(GeoPoint.getPoint(59.947258f, 30.375088f))
        .`type`(EntryType.RAILWAY_STATION)
        .countryGeoId(225)
        .rgid(143)
        .changeRegion(false)
        .searchParams(searchParams)
        .build
      val result = responseBuilder.convert(railwayStation)
      result shouldEqual expected
    }
  }

  private def mockAll(): Unit = {
    (regionNamesTranslationsProvider.get _)
      .expects()
      .anyNumberOfTimes()
      .returns(RegionNamesTranslations(Map.empty, populatedRgids))
    (regionDocumentsStatisticsProvider.get _)
      .expects()
      .anyNumberOfTimes()
      .returning(new RegionDocumentsStatisticsStorage(new java.util.HashSet[RegionDocumentsStatistics]()))
    (geoHelperProvider.get _)
      .expects()
      .anyNumberOfTimes()
      .returning(new GeoHelper(regionGraphProvider, regionDocumentsStatisticsProvider))
  }

}
