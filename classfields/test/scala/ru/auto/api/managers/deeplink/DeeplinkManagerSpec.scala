package ru.auto.api.managers.deeplink

import org.mockito.Mockito._
import ru.auto.api.BaseSpec
import ru.auto.api.CatalogModel.CatalogScreenTab
import ru.auto.api.ResponseModel.OfferResponse
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.deeplink.DeeplinkManager.DeeplinkType
import ru.auto.api.managers.enrich.SearchItemManager
import ru.auto.api.managers.favorite.SearchHistoryItem
import ru.auto.api.managers.geo.GeoManager
import ru.auto.api.managers.offers.OfferCardManager
import ru.auto.api.model.CategorySelector.{Cars, Moto}
import ru.auto.api.model.ModelGenerators.PrivateUserRefGen
import ru.auto.api.model.parts.DeeplinkParseResult
import ru.auto.api.model.searcher.{ApiSearchRequest, OfferCardAdditionalParams}
import ru.auto.api.model.{ModelGenerators, OfferID, RequestParams}
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.web.WebClient
import ru.auto.api.testkit.TestData
import ru.auto.api.ui.UiModel.TristateTumblerGroup
import ru.auto.api.util.RequestImpl
import ru.auto.api.util.search.SearchMappings
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

class DeeplinkManagerSpec extends BaseSpec with MockitoSupport {
  implicit private val trace: Traced = Traced.empty

  implicit private val request: RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
    r.setUser(PrivateUserRefGen.next)
    r.setApplication(Application.iosApp)
    r.setToken(TokenServiceImpl.iosApp)
    r.setTrace(trace)
    r
  }

  val webClient: WebClient = mock[WebClient]
  val searchItemManager: SearchItemManager = mock[SearchItemManager]
  val geoManager: GeoManager = mock[GeoManager]
  val offerCardManager: OfferCardManager = mock[OfferCardManager]

  val featureManager: FeatureManager = mock[FeatureManager]
  val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)
  when(featureManager.oldOptionsSearchMapping).thenReturn(feature)

  when(featureManager.allowSearcherRequestEnrichmentWithExpFlags).thenReturn(Feature("", _ => true))
  when(featureManager.allowSearcherRequestEnrichmentWithGlobalFlags).thenReturn(Feature("", _ => true))
  when(featureManager.dealerBoostCoefficient).thenReturn(Feature("", _ => 1.1f))

  val searchMappings: SearchMappings = new SearchMappings(new DefaultsMapper(featureManager), featureManager)

  val manager: DeeplinkManager = new DeeplinkManager(
    webClient,
    searchItemManager,
    offerCardManager,
    TestData.tree,
    geoManager,
    "http://m.auto.ru",
    searchMappings
  )

  after {
    reset(webClient, searchItemManager)
  }

  "DeeplinkManager" should {
    "parse search link" in {
      val filter = ModelGenerators.SearchFilterGen.next
      val historyItem = ModelGenerators.SearchHistoryItemGen.next
      val link = "/moskva/cars/audi/a4/used/?price_from=1000000&autoru_body_type=SEDAN"
      when(webClient.parseSearchLink(?)(?)).thenReturnF(ApiSearchRequest(Cars, filter))
      when(searchItemManager.enrichSearch[SearchHistoryItem](?, ?, ?)(?)).thenReturnF(historyItem)

      val resp = manager.parseDeepLink(link, None).futureValue

      historyItem.view.foreach(v => resp.getSearchData.getView shouldBe v)
      resp.getSearchData.getParams shouldBe historyItem.filter

      verify(webClient).parseSearchLink(eq(link))(?)
      verify(searchItemManager).enrichSearch(?, ?, eq(true))(?)
    }

    "parse sorting" in {
      val query = "price_from=1000000&autoru_body_type=SEDAN&sort=price-desc&mark_model_nameplate=BMW%233ER"

      val resp = manager.parseSorting(query).get

      resp.getName shouldBe "price"
      resp.getDesc shouldBe true
    }

    "parse search moto link" in {
      val filter = ModelGenerators.SearchFilterGen.next
      val historyItem = ModelGenerators.SearchHistoryItemGen.next
      val link = "/moskva/mototsikly/bmw/used"
      when(webClient.parseSearchLink(?)(?)).thenReturnF(ApiSearchRequest(Cars, filter))
      when(searchItemManager.enrichSearch[SearchHistoryItem](?, ?, ?)(?)).thenReturnF(historyItem)

      val resp = manager.parseDeepLink(link, None).futureValue

      historyItem.view.foreach(v => resp.getSearchData.getView shouldBe v)
      resp.getSearchData.getParams shouldBe historyItem.filter

      verify(webClient).parseSearchLink(eq("/moskva/mototsikly/bmw/used/?"))(?)
      verify(searchItemManager).enrichSearch(?, ?, eq(true))(?)
    }

    "parse moto offer link" in {
      val link = "/mototsikly/used/sale/honda/cbr_1000_rr/2917535-3dbc9cc8/"
      when(offerCardManager.getOfferCardResponse(?, ?, ?)(?)).thenReturnF(OfferResponse.getDefaultInstance)
      val resp = manager.parseDeepLink(link, None).futureValue
      resp.getOfferData shouldBe OfferResponse.getDefaultInstance
      verify(offerCardManager).getOfferCardResponse(eq(Moto), eq(OfferID.parse("2917535-3dbc9cc8")), any())(eq(request))
    }

    "parse offer in group link" in {
      val link = "/cars/new/group/changan/cs35plus/21641203/21641255/1095368932-8e43ca59/"
      when(offerCardManager.getOfferCardResponse(?, ?, ?)(?)).thenReturnF(OfferResponse.getDefaultInstance)
      val resp = manager.parseDeepLink(link, None).futureValue
      resp.getOfferData shouldBe OfferResponse.getDefaultInstance
      verify(offerCardManager).getOfferCardResponse(eq(Cars), eq(OfferID.parse("1095368932-8e43ca59")), any())(
        eq(request)
      )
    }

    "parse moto offer link with params" in {
      val link = "/mototsikly/used/sale/honda/cbr_1000_rr/2917535-3dbc9cc8/?geo_radius=200&geo_id=213"
      when(offerCardManager.getOfferCardResponse(?, ?, ?)(?)).thenReturnF(OfferResponse.getDefaultInstance)
      val resp = manager.parseDeepLink(link, None).futureValue
      resp.getOfferData shouldBe OfferResponse.getDefaultInstance
      verify(offerCardManager).getOfferCardResponse(
        eq(Moto),
        eq(OfferID.parse("2917535-3dbc9cc8")),
        eq(OfferCardAdditionalParams(List(213), Some(200), TristateTumblerGroup.BOTH))
      )(eq(request))
    }

    "parse moto search without subcategory" in {
      val filter = ModelGenerators.SearchFilterGen.next
      val historyItem = ModelGenerators.SearchHistoryItemGen.next
      val link = "/moskva/moto/all/"
      when(webClient.parseSearchLink(?)(?)).thenReturnF(ApiSearchRequest(Cars, filter))
      when(searchItemManager.enrichSearch[SearchHistoryItem](?, ?, ?)(?)).thenReturnF(historyItem)

      val resp = manager.parseDeepLink(link, None).futureValue

      historyItem.view.foreach(v => resp.getSearchData.getView shouldBe v)
      resp.getSearchData.getParams shouldBe historyItem.filter

      verify(webClient).parseSearchLink(eq("/moskva/moto/all/?"))(?)
      verify(searchItemManager).enrichSearch(?, ?, eq(true))(?)
    }

    "parse catalog link to model_card tab" in {
      val link = "/catalog/cars/renault/arkana/21570852/21570905/"

      val resp = manager.parseDeepLink(link, None).futureValue

      val carsCatalogScreen = resp.getCatalogScreen.getCarsCatalogScreen
      carsCatalogScreen.getScreenTab shouldBe CatalogScreenTab.MODEL_CARD_TAB
      carsCatalogScreen.getConfigurationId shouldBe 21570905
    }

    "parse catalog link to characteristics tab" in {
      val link = "/catalog/cars/renault/arkana/21570852/21570905/specifications/"

      val resp = manager.parseDeepLink(link, None).futureValue

      val carsCatalogScreen = resp.getCatalogScreen.getCarsCatalogScreen
      carsCatalogScreen.getScreenTab shouldBe CatalogScreenTab.CHARACTERISTICS_TAB
      carsCatalogScreen.getConfigurationId shouldBe 21570905
    }

    "parse catalog link to complectation tab" in {
      val link = "/catalog/cars/renault/arkana/21570852/21570905/equipment/"

      val resp = manager.parseDeepLink(link, None).futureValue

      val carsCatalogScreen = resp.getCatalogScreen.getCarsCatalogScreen
      carsCatalogScreen.getScreenTab shouldBe CatalogScreenTab.COMPLECTATION_TAB
      carsCatalogScreen.getConfigurationId shouldBe 21570905
    }

    "parse catalog link with tech_params and complectation" in {
      val link = "/catalog/cars/renault/arkana/21570852/21570905/specifications/21570905_21593413_21593352/"

      val resp = manager.parseDeepLink(link, None).futureValue

      val carsCatalogScreen = resp.getCatalogScreen.getCarsCatalogScreen
      carsCatalogScreen.getScreenTab shouldBe CatalogScreenTab.CHARACTERISTICS_TAB
      carsCatalogScreen.getConfigurationId shouldBe 21570905
      carsCatalogScreen.getTechParamId shouldBe 21593352
      carsCatalogScreen.getComplectationId shouldBe 21593413
    }

    "throw error when catalog link has mistakes" in {
      val link = "/catalog/cars/renault/arkana/21570852/21570905/ERROR/21570905_21593413_21593352/"

      an[IllegalArgumentException] shouldBe thrownBy {
        manager.parseDeepLink(link, None).futureValue
      }
    }

    "throw error when catalog link has mistakes(2)" in {
      val link = "/catalog/cars/renault/arkana/21570852/21570905/ERRO"

      an[IllegalArgumentException] shouldBe thrownBy {
        manager.parseDeepLink(link, None).futureValue
      }
    }

    "parse link with russian letters in it" in {
      val filter = ModelGenerators.SearchFilterGen.next
      val historyItem = ModelGenerators.SearchHistoryItemGen.next
      val link = "/cars/vaz/1111/all/?query=ока&from=searchline"

      when(webClient.parseSearchLink(?)(?)).thenReturnF(ApiSearchRequest(Cars, filter))
      when(searchItemManager.enrichSearch[SearchHistoryItem](?, ?, ?)(?)).thenReturnF(historyItem)

      val resp = manager.parseDeepLink(link, None).futureValue

      historyItem.view.foreach(v => resp.getSearchData.getView shouldBe v)
      resp.getSearchData.getParams shouldBe historyItem.filter

      verify(webClient).parseSearchLink(eq(link))(?)
      verify(searchItemManager).enrichSearch(?, ?, eq(true))(?)
    }

    "parse autoparts old format link" in {
      when(webClient.parsePartsLink(?)(?))
        .thenReturnF(
          DeeplinkParseResult(DeeplinkParseResult.PartsDeeplinkType.PartsOffer, Map("offerId" -> Set("v3fiumoch8hv3")))
        )
      manager.parseDeepLink("https://auto.ru/parts/?offerId=v3fiumoch8hv3", None)
      verify(webClient).parsePartsLink(eq("/parts/?offerId=v3fiumoch8hv3"))(?)
    }

    "parse autoparts new format link" in {
      when(webClient.parsePartsLink(?)(?))
        .thenReturnF(
          DeeplinkParseResult(DeeplinkParseResult.PartsDeeplinkType.PartsOffer, Map("offerId" -> Set("v3fiumoch8hv3")))
        )
      manager.parseDeepLink("https://parts.auto.ru/?offerId=v3fiumoch8hv3", None)
      verify(webClient).parsePartsLink(eq("/?offerId=v3fiumoch8hv3"))(?)
    }

    "parse autoparts combine format link" in {
      when(webClient.parsePartsLink(?)(?))
        .thenReturnF(
          DeeplinkParseResult(DeeplinkParseResult.PartsDeeplinkType.PartsOffer, Map("offerId" -> Set("v3fiumoch8hv3")))
        )
      manager.parseDeepLink("https://parts.test.avto.ru/parts/?offerId=v3fiumoch8hv3", None)
      verify(webClient).parsePartsLink(eq("/parts/?offerId=v3fiumoch8hv3"))(?)
    }

    "correctly define group links" in {
      DeeplinkManager.defineLinkType("auto.ru", "/cars/new/group/<mark>/<model>/<tech_param_id>/") shouldBe DeeplinkType.Search
      DeeplinkManager.defineLinkType("auto.ru", "/cars/new/group/<mark>/<model>/<tech_param_id>/<complectation_id>/") shouldBe DeeplinkType.Search
      DeeplinkManager.defineLinkType("auto.ru", "/cars/new/group/bmw/x5/21307931-21307996/") shouldBe DeeplinkType.Search
      DeeplinkManager.defineLinkType(
        "auto.ru",
        "/cars/new/group/<mark>/<model>/<tech_param_id>/<complectation_id>/<sale_id>-<sale_hash>/"
      ) shouldBe DeeplinkType.OfferCard
    }
  }
}
