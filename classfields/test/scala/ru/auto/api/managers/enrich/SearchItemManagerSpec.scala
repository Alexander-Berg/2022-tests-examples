package ru.auto.api.managers.enrich

import org.mockito.Mockito.verify
import org.scalacheck.Gen
import org.scalatest.OptionValues
import ru.auto.api.ApiOfferModel.Salon
import ru.auto.api.BaseSpec
import ru.auto.api.BreadcrumbsModel.{Entity, MarkEntity}
import ru.auto.api.CommonModel.ClientFeature
import ru.auto.api.TrucksModel.TruckCategory
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.catalog.CatalogManager
import ru.auto.api.managers.favorite.PersonalSavedSearch
import ru.auto.api.managers.features.AppsFeaturesManager
import ru.auto.api.model.CategorySelector.{Cars, Trucks}
import ru.auto.api.model.ModelGenerators.DeviceUidGen
import ru.auto.api.model.favorite.OfferSearchesDomain
import ru.auto.api.model.gen.BasicGenerators
import ru.auto.api.model.searcher.ApiSearchRequest
import ru.auto.api.model.{CategorySelector, ModelGenerators, RequestParams}
import ru.auto.api.search.SearchModel.{SearchRequestParameters, TrucksSearchRequestParameters}
import ru.auto.api.services.pushnoy.PushnoyClient
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.services.settings.SettingsClient
import ru.auto.api.testkit.TestData
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.auto.api.util.search.{SearchDiffUtils, SearchMappings}
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.catalog.model.api.ApiModel.{MarkCard, RawCatalog}
import ru.yandex.passport.model.api.ApiModel.SessionResult
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._

class SearchItemManagerSpec extends BaseSpec with MockitoSupport with OptionValues {

  private val pushnoyClient = mock[PushnoyClient]
  private val settingsClient = mock[SettingsClient]
  private val appsFeaturesManager = new AppsFeaturesManager(pushnoyClient, settingsClient)
  private val catalogManager = mock[CatalogManager]
  private val searcherClient = mock[SearcherClient]

  val featureManager: FeatureManager = mock[FeatureManager]
  val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)
  when(featureManager.oldOptionsSearchMapping).thenReturn(feature)

  val defaultsMapper = new DefaultsMapper(featureManager)
  val searchMappings: SearchMappings = new SearchMappings(defaultsMapper, featureManager)
  val searchDiffUtils = new SearchDiffUtils(searchMappings, defaultsMapper)

  private val manager = new SearchItemManager(
    appsFeaturesManager,
    catalogManager,
    searcherClient,
    TestData.tree,
    searchDiffUtils,
    searchMappings
  )

  implicit private val trace: Traced = Traced.empty
  implicit val request: Request = newRequest()

  def newRequest(): RequestImpl = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.iosApp)
    r
  }

  "SearchItemManager" should {
    "return dealer name in search title" in {
      val dealerId = Gen.choose(2000000, 3000000).next.toString
      val name = BasicGenerators.readableString.next
      when(searcherClient.getSalonByDealerId(eq(dealerId))(?)).thenReturnF(Salon.newBuilder().setName(name).build())
      val catalog = RawCatalog.newBuilder()
      when(catalogManager.exactByCatalogFilter(?, ?, ?, ?, ?)(?)).thenReturnF(catalog.build())
      val title = manager
        .getSearchTitleSafe(
          ApiSearchRequest(CategorySelector.Cars, SearchRequestParameters.newBuilder().setDealerId(dealerId).build())
        )
        .futureValue

      title shouldBe name + ", Все марки автомобилей"
    }

    "enrich cars saved searches listing" in {
      val savedSearches = Gen
        .nonEmptyListOf(
          ModelGenerators.personalSavedSearchGen(OfferSearchesDomain, optCategory = Some(Cars))
        )
        .next
      val catalog = RawCatalog.newBuilder()
      val marks = savedSearches
        .flatMap(_.optFilter)
        .map(_.getMarkModelNameplate(0))
        .map { mark =>
          mark -> MarkCard
            .newBuilder()
            .setEntity(
              Entity
                .newBuilder()
                .setId(mark)
                .setName(mark)
                .setMark(MarkEntity.getDefaultInstance)
            )
            .build()
        }
        .toMap
      catalog.putAllMark(marks.asJava)

      when(catalogManager.exactBySearchItem[PersonalSavedSearch](?, any[Seq[PersonalSavedSearch]](), ?, ?)(?))
        .thenReturnF(catalog.build())
      val res = manager.enrichSearches(savedSearches).futureValue

      res.head.view.value.getMarkModelNameplateGenViewsList shouldNot be(empty)
    }

    "enrich trucks saved searches listing" in {
      val savedSearches = Gen
        .nonEmptyListOf(
          ModelGenerators.personalSavedSearchGen(OfferSearchesDomain, optCategory = Some(Trucks))
        )
        .next

      val catalog = RawCatalog.newBuilder()
      val marks = savedSearches
        .flatMap(_.optFilter)
        .map(_.getMarkModelNameplate(0))
        .map { mark =>
          mark -> MarkCard
            .newBuilder()
            .setEntity(
              Entity
                .newBuilder()
                .setId(mark)
                .setName(mark)
                .setMark(MarkEntity.getDefaultInstance)
            )
            .build()
        }
        .toMap
      catalog.putAllMark(marks.asJava)

      when(catalogManager.exactBySearchItem[PersonalSavedSearch](?, any[Seq[PersonalSavedSearch]](), ?, ?)(?))
        .thenReturnF(catalog.build())

      val res = manager.enrichSearches(savedSearches).futureValue

      res.head.view.value.getMarkModelNameplateGenViewsList shouldNot be(empty)
    }

    "return search with unsupported fields" in {
      implicit val customReq: RequestImpl = new RequestImpl
      customReq.setTrace(trace)
      customReq.setRequestParams(RequestParams.construct("1.1.1.1"))
      customReq.setApplication(Application.iosApp)
      customReq.setUser(ModelGenerators.PersonalUserRefGen.next)
      val session = {
        val b = SessionResult.newBuilder()
        b.getSessionBuilder.setDeviceUid(DeviceUidGen.next)
        b.build()
      }
      customReq.setSession(session)

      val params = {
        val b = ModelGenerators.SearchFilterGen.next.toBuilder
        b.addMarkModelNameplate("secondMark").addCatalogEquipment("test equipment")
        b.setTrucksParams(
          TrucksSearchRequestParameters
            .newBuilder()
            .setTrucksCategory(TruckCategory.AGRICULTURAL)
        )
        b.build()
      }
      val savedSearch = ModelGenerators
        .personalSavedSearchGen(OfferSearchesDomain, optCategory = Some(Trucks), optFilter = Some(params))
        .next

      val catalog = RawCatalog.newBuilder()
      val marks = params.getMarkModelNameplateList.asScala.map { mark =>
        mark -> MarkCard
          .newBuilder()
          .setEntity(
            Entity
              .newBuilder()
              .setId(mark)
              .setName(mark)
              .setMark(MarkEntity.getDefaultInstance)
          )
          .build()
      }.toMap
      catalog.putAllMark(marks.asJava)

      when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map("features" -> "SEARCH_SPECIAL_TRUCKS"))
      when(catalogManager.exactBySearchItem[PersonalSavedSearch](?, any[Seq[PersonalSavedSearch]](), ?, ?)(?))
        .thenReturnF(catalog.build())
      val res = manager.enrichSearch(savedSearch).futureValue

      res.unsupportedFields.map(_.getFeature).toSet shouldEqual Set(
        ClientFeature.SEARCH_MMNG_MULTICHOICE,
        ClientFeature.SEARCH_TRUCKS_CATALOG_EQUIPMENT
      )

      verify(settingsClient).getSettings(SettingsClient.SettingsDomain, customReq.user.anonRef)(trace)
    }

    "return search without unsupported fields" in {
      implicit val customReq: RequestImpl = new RequestImpl
      customReq.setTrace(trace)
      customReq.setRequestParams(RequestParams.construct("1.1.1.1"))
      customReq.setApplication(Application.iosApp)
      customReq.setUser(ModelGenerators.PersonalUserRefGen.next)
      val session = {
        val b = SessionResult.newBuilder()
        b.getSessionBuilder.setDeviceUid(DeviceUidGen.next)
        b.build()
      }
      customReq.setSession(session)

      val params = {
        val b = ModelGenerators.SearchFilterGen.next.toBuilder
        b.addMarkModelNameplate("secondMark").addCatalogEquipment("test equipment")
        b.setTrucksParams(
          TrucksSearchRequestParameters
            .newBuilder()
            .setTrucksCategory(TruckCategory.AGRICULTURAL)
        )
        b.build()
      }
      val savedSearch = ModelGenerators
        .personalSavedSearchGen(OfferSearchesDomain, optCategory = Some(Trucks), optFilter = Some(params))
        .next

      val catalog = RawCatalog.newBuilder()
      val marks = params.getMarkModelNameplateList.asScala.map { mark =>
        mark -> MarkCard
          .newBuilder()
          .setEntity(
            Entity
              .newBuilder()
              .setId(mark)
              .setName(mark)
              .setMark(MarkEntity.getDefaultInstance)
          )
          .build()
      }.toMap
      catalog.putAllMark(marks.asJava)

      when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map("features" -> "SEARCH_SPECIAL_TRUCKS"))
      when(catalogManager.exactBySearchItem[PersonalSavedSearch](?, any[Seq[PersonalSavedSearch]](), ?, ?)(?))
        .thenReturnF(catalog.build())
      val res = manager.enrichSearch(savedSearch, withoutUnsupportedFields = true).futureValue

      res.unsupportedFields.map(_.getFeature).toSet should be(empty)

      verify(settingsClient).getSettings(SettingsClient.SettingsDomain, customReq.user.anonRef)(trace)
    }
  }
}
