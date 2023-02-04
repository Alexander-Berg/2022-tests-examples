package ru.yandex.realty.geohub.managers

import com.google.protobuf.{Int32Value, StringValue}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.buildinginfo.storage.BuildingStorage
import ru.yandex.realty.clients.suggest.GeoSuggestClient
import ru.yandex.realty.context.street.StreetStorage
import ru.yandex.realty.features.Feature
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.geohub.api.handlers.suggest.RegionSuggestRequest
import ru.yandex.realty.geohub.proto.api.suggest.RegionSuggest
import ru.yandex.realty.geohub.search.EntrySearcher
import ru.yandex.realty.geohub.service.EntrySuggestResponseBuilder
import ru.yandex.realty.graph.core.GeoObjectType
import ru.yandex.realty.model.locale.RealtyLocale
import ru.yandex.realty.storage.RegionDocumentsStatisticsTestComponents
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.geo.GeoHelper
import ru.yandex.realty.util.protobuf.GeoObjectTypeProtoConverter

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class SuggestManagerSpec
  extends AsyncSpecBase
  with RegionGraphTestComponents
  with RegionDocumentsStatisticsTestComponents {

  private val geoHelperProvider: Provider[GeoHelper] = () =>
    new GeoHelper(regionGraphProvider, regionDocumentsStatisticsProvider)
  private val entrySearcher: EntrySearcher = mock[EntrySearcher]
  private val streetStorage: StreetStorage = mock[StreetStorage]
  private val buildingStorage: BuildingStorage = mock[BuildingStorage]
  private val geoSuggestClient: GeoSuggestClient = mock[GeoSuggestClient]
  private val responseBuilder: EntrySuggestResponseBuilder = mock[EntrySuggestResponseBuilder]
  private val suggestManager = new SuggestManager(
    regionGraphProvider,
    geoHelperProvider,
    entrySearcher,
    streetStorage,
    buildingStorage,
    geoSuggestClient,
    responseBuilder
  )
  private val rostovNaDonuRegion =
    buildRegion(
      39,
      214386,
      "город Ростов-на-Дону",
      GeoObjectType.CITY,
      Some("Ростовская область"),
      Some(200)
    )

  private val krasnayaPolanaRegion =
    buildRegion(
      10994,
      180000,
      "посёлок городского типа Красная Поляна",
      GeoObjectType.CITY,
      Some("Краснодарский край"),
      Some(500)
    )
  implicit private val traced: Traced = Traced.empty

  "SuggestManager" should {
    "autocomplete Ростов-на-Дону" in {
      val result = suggestManager.suggestRegion(buildRequest("Ростов-на-Дону")).futureValue
      result.getResponse.getRegionsList.asScala shouldBe Seq(rostovNaDonuRegion)
    }

    "autocomplete РОСТОВ-НА-ДОНУ" in {
      val result = suggestManager.suggestRegion(buildRequest("РОСТОВ-НА-ДОНУ")).futureValue
      result.getResponse.getRegionsList.asScala shouldBe Seq(rostovNaDonuRegion)
    }

    "autocomplete hjcnjd-yf-ljye (eng ростов-на-дону)" in {
      val result = suggestManager.suggestRegion(buildRequest("hjcnjd-yf-ljye")).futureValue
      result.getResponse.getRegionsList.asScala shouldBe Seq(rostovNaDonuRegion)
    }

    "autocomplete ростов на дону" in {
      val result = suggestManager.suggestRegion(buildRequest("ростов на дону")).futureValue
      result.getResponse.getRegionsList.asScala shouldBe Seq(rostovNaDonuRegion)
    }

    "autocomplete ростов на" in {
      val result = suggestManager.suggestRegion(buildRequest("ростов на")).futureValue
      result.getResponse.getRegionsList.asScala shouldBe Seq(rostovNaDonuRegion)
    }

    "autocomplete Красная Поляна" in {
      val result = suggestManager.suggestRegion(buildRequest("Красная Поляна")).futureValue
      result.getResponse.getRegionsList.asScala shouldBe Seq(krasnayaPolanaRegion)
    }
  }

  private def buildRequest(text: String, locale: RealtyLocale = RealtyLocale.RU): RegionSuggestRequest =
    RegionSuggestRequest(text, locale)

  private def buildRegion(
    geoId: Int,
    rgid: Long,
    name: String,
    geoObjectType: GeoObjectType,
    scope: Option[String],
    totalOffers: Option[Int]
  ): RegionSuggest.Region = {
    val region = RegionSuggest.Region
      .newBuilder()
      .setGeoId(geoId)
      .setRgid(rgid)
      .setName(name)
      .setType(GeoObjectTypeProtoConverter.toProto(geoObjectType))
    scope.map(StringValue.of).foreach(region.setScope)
    totalOffers.map(Int32Value.of).foreach(region.setTotalOffers)
    region.build()
  }
}
