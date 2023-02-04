package ru.auto.api.util.search

import ru.auto.api.BaseSpec
import ru.auto.api.CarsModel.Car.{BodyType, EngineType}
import ru.auto.api.CommonModel.ClientFeature
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.model.CategorySelector.{Cars, Moto}
import ru.auto.api.model.searcher.ApiSearchRequest
import ru.auto.api.model.uaas.UaasResponse
import ru.auto.api.model.{MarkModelNameplateGeneration, Paging, RequestParams, Sorting}
import ru.auto.api.search.SearchModel.{CarsSearchRequestParameters, CatalogFilter, SearchRequestParameters, State}
import ru.auto.api.ui.UiModel._
import ru.auto.api.util.RequestImpl
import ru.auto.api.util.search.SearcherFieldNames._
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import java.util.Base64
import scala.jdk.CollectionConverters._

class SearchMappingsSpec extends BaseSpec with MockitoSupport {
  implicit val paging: Paging = Paging.Default
  implicit val sorting: Sorting = SearcherRequestMapper.topSortings.head
  implicit val trace: Traced = Traced.empty

  implicit val clientRequest: RequestImpl = {
    val req = new RequestImpl
    req.setApplication(Application.desktop)
    req.setTrace(trace)
    req.setRequestParams(RequestParams.empty)
    req
  }

  val featureManager: FeatureManager = mock[FeatureManager]
  val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)
  when(featureManager.oldOptionsSearchMapping).thenReturn(feature)
  when(featureManager.allowSearcherRequestEnrichmentWithExpFlags).thenReturn(Feature("", _ => true))
  when(featureManager.allowSearcherRequestEnrichmentWithGlobalFlags).thenReturn(Feature("", _ => true))
  when(featureManager.dealerBoostCoefficient).thenReturn(Feature("", _ => 1.1f))

  val defaultsMapper = new DefaultsMapper(featureManager)
  val searchMappings: SearchMappings = new SearchMappings(defaultsMapper, featureManager)

  "SearchMappings" should {
    "expand groups for cars filter" in {
      val original = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .setCarsParams(
            CarsSearchRequestParameters
              .newBuilder()
              .addBodyTypeGroup(BodyTypeGroup.COUPE)
              .addEngineGroup(EngineGroup.ATMO)
              .addEngineGroup(EngineGroup.DIESEL)
              .addEngineGroup(EngineGroup.LPG)
          )
          .setOwnersCountGroup(OwnersCountGroup.LESS_THAN_TWO)
          .setOwningTimeGroup(OwningTimeGroup.MORE_THAN_3)
          .setCustomsStateGroup(CustomsGroup.DOESNT_MATTER)
          .setExchangeGroup(ExchangeGroup.POSSIBLE)
          .addSellerGroup(SellerGroup.COMMERCIAL)
          .setStateGroup(StateGroup.ALL)
          .setDamageGroup(DamageGroup.NOT_BEATEN)
          .build()
      )

      val resultReq = searchMappings.fromApiToSearcher(original, None)
      val result = resultReq.params

      //noinspection ScalaStyle
      result(AUTORU_BODY_TYPE) shouldEqual Set(BodyType.COUPE, BodyType.COUPE_HARDTOP, BodyType.SEDAN_2_DOORS)
        .map(_.toString)
      result(EXCHANGE_STATUS) shouldEqual Set("1")
      result(DEALER_ORG_TYPE) shouldEqual Set[String]("1", "2")
      result(STATE) shouldEqual Set(State.NEW, State.USED).map(_.toString)
      result(ENGINE_TYPE) shouldEqual Set(EngineType.DIESEL).map(_.toString)
      result(FEEDING_TYPE) shouldEqual Set("none")
      result(CATALOG_EQUIPMENT) shouldEqual Set("gbo")
      result(OWNERS_COUNT_FROM) shouldEqual Set("1")
      result(OWNERS_COUNT_TO) shouldEqual Set("2")
      result(OWNING_TIME_FROM) shouldEqual Set("36")
      result.contains(OWNING_TIME_TO) shouldBe false
      result(CUSTOM_STATE_KEY) shouldEqual Set("CLEARED", "NOT_CLEARED")

      result.find { case (k, _) => k.contains("group") } shouldBe None

      val reversedReq = searchMappings.fromSearcherToApi(resultReq)
      val reversed = reversedReq.params

      reversed.getCarsParams.getBodyTypeGroupList.asScala.toSet shouldEqual Set(BodyTypeGroup.COUPE)
      reversed.getExchangeGroup shouldBe ExchangeGroup.POSSIBLE
      reversed.getSellerGroupList.asScala.toSet shouldEqual Set(SellerGroup.COMMERCIAL)
      reversed.getStateGroup shouldBe StateGroup.ALL
      reversed.getDamageGroup shouldBe DamageGroup.NOT_BEATEN
      reversed.getCarsParams.getEngineGroupList.asScala.toSet shouldEqual Set(
        EngineGroup.ATMO,
        EngineGroup.DIESEL,
        EngineGroup.LPG
      )
      reversed.getOwnersCountGroup shouldBe OwnersCountGroup.LESS_THAN_TWO
      reversed.getOwningTimeGroup shouldBe OwningTimeGroup.MORE_THAN_3
      reversed.getCustomsStateGroup shouldBe CustomsGroup.DOESNT_MATTER
    }

    "process 'any' mappings" in {
      val original = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .setCarsParams(
            CarsSearchRequestParameters
              .newBuilder()
              .addBodyTypeGroup(BodyTypeGroup.ANY_BODY)
              .addEngineGroup(EngineGroup.ANY_ENGINE)
          )
          .setOwnersCountGroup(OwnersCountGroup.ANY_COUNT)
          .setOwningTimeGroup(OwningTimeGroup.ANY_TIME)
          .addSellerGroup(SellerGroup.ANY_SELLER)
          .build()
      )

      val resultReq = searchMappings.fromApiToSearcher(original, None)
      val result = resultReq.params

      //noinspection ScalaStyle
      result.contains("autoru_body_type") shouldBe false
      result.contains("engine_type") shouldBe false
      result.contains("owners_count_from") shouldBe false
      result.contains("owners_count_to") shouldBe false
      result.contains("owning_time_from") shouldBe false
      result.contains("owning_time_to") shouldBe false

      result("dealer_org_type") shouldEqual Set("1", "2", "4")

      result.find { case (k, _) => k.contains("group") } shouldBe None

      val reversedReq = searchMappings.fromSearcherToApi(resultReq)
      val reversed = reversedReq.params

      reversed.getCarsParams.getBodyTypeGroupList.asScala.toSet shouldEqual Set(BodyTypeGroup.ANY_BODY)
      reversed.getCarsParams.getEngineGroupList.asScala.toSet shouldEqual Set(EngineGroup.ANY_ENGINE)
      reversed.getSellerGroupList.asScala.toSet shouldEqual Set(SellerGroup.ANY_SELLER)
      reversed.getOwnersCountGroup shouldBe OwnersCountGroup.ANY_COUNT
      reversed.getOwningTimeGroup shouldBe OwningTimeGroup.ANY_TIME
    }

    "expand geo radius for cars filter" in {
      val original = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .addRid(213)
          .build()
      )

      val result = searchMappings.fromApiToSearcher(original, None)

      result.params("geo_radius") shouldEqual Set("35")

      val reversed = searchMappings.fromSearcherToApi(result).params

      reversed should be(Symbol("hasGeoRadius"))
    }

    "expand rids for cars filter" in {
      val original = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .addRid(11004)
          .build()
      )

      val result = searchMappings.fromApiToSearcher(original, None).params

      result("rid") shouldEqual Set("11004", "10995")
    }

    "decay default damage group" in {
      val original = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .setStateGroup(StateGroup.NEW)
          .build()
      )

      val result = searchMappings.fromApiToSearcher(original, None)

      result.params("state") shouldEqual Set(State.NEW.toString)
      result.params.find { case (k, _) => k.contains("group") } shouldBe None

      val reversed = searchMappings.fromSearcherToApi(result).params

      reversed.getStateGroup shouldBe StateGroup.NEW
      reversed.getDamageGroup shouldBe DamageGroup.NOT_BEATEN
    }

    "add defaults for new offers" in {
      val original = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .setStateGroup(StateGroup.NEW)
          .build()
      )

      val resultReq = searchMappings.fromApiToSearcher(original, None)
      val result = resultReq.params

      result("state") shouldEqual Set(State.NEW.toString)
      result("dealer_org_type") shouldEqual Set("1", "2", "4")
      result.find { case (k, _) => k.contains("group") } shouldBe None

      val reversed = searchMappings.fromSearcherToApi(resultReq).params

      reversed.getSellerGroupList.asScala.toSet shouldBe Set(SellerGroup.ANY_SELLER)
      reversed.hasOnlyOfficial shouldBe false
    }

    "add defaults for all offers" in {
      val original = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .setStateGroup(StateGroup.ALL)
          .build()
      )

      val resultReq = searchMappings.fromApiToSearcher(original, None)
      val result = resultReq.params

      result("state") shouldEqual Set(State.NEW, State.USED).map(_.toString)
      result("dealer_org_type") shouldEqual Set("1", "2", "4")
      result.find { case (k, _) => k.contains("group") } shouldBe None

      val reversed = searchMappings.fromSearcherToApi(resultReq).params

      reversed.getSellerGroupList.asScala.toSet shouldBe Set(SellerGroup.ANY_SELLER)
    }

    "keep state if passed" in {
      val original = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .addState(State.NEW)
          .build()
      )

      val resultReq = searchMappings.fromApiToSearcher(original, None)
      val result = resultReq.params

      result("state") shouldEqual Set(State.NEW.toString)
      result("dealer_org_type") shouldEqual Set("1", "2", "4")
      result.find { case (k, _) => k.contains("group") } shouldBe None

      val reversed = searchMappings.fromSearcherToApi(resultReq).params

      reversed.getSellerGroupList.asScala.toSet shouldBe Set(SellerGroup.ANY_SELLER)
    }

    "map old filters to catalogFilters when have one of each old type filters" in {
      val params = SearchRequestParameters
        .newBuilder()
        .addMarkModelNameplate(new MarkModelNameplateGeneration(Option("mark"), Option("model"), None, None).render())
      params.getCarsParamsBuilder
        .addTechParamId("12")
        .addComplectationId("12")
        .addComplectationName("some")

      val filters = searchMappings
        .enrichCatalogFilter(Cars, params.build(), Set(ClientFeature.SEARCH_CATALOG_FILTER_CARS))

      filters.getCatalogFilterCount shouldBe 1
    }

    "map old filters to catalogFilters when have one filter with many conditions" in {
      val params = SearchRequestParameters
        .newBuilder()
      params.getCarsParamsBuilder
        .addTechParamId("12")
        .addTechParamId("34")
        .addTechParamId("36")

      val filters = searchMappings
        .enrichCatalogFilter(Cars, params.build(), Set(ClientFeature.SEARCH_CATALOG_FILTER_CARS))

      filters.getCatalogFilterCount shouldBe 3
    }

    "not map old filters to catalogFilters when have many filter with many conditions" in {
      val params = SearchRequestParameters
        .newBuilder()
      params.getCarsParamsBuilder
        .addTechParamId("12")
        .addTechParamId("34")
        .addTechParamId("36")
        .addConfigurationId("5")

      val filters = searchMappings
        .enrichCatalogFilter(Cars, params.build(), Set(ClientFeature.SEARCH_CATALOG_FILTER_CARS))

      filters.getCatalogFilterCount shouldBe 0
    }

    "map old filters to catalogFilters when have many filter with many conditions counting grouping" in {
      val params = SearchRequestParameters
        .newBuilder()
      params.setGroupingId("tech_param_id=12,complectation_id=5")
      params.getCarsParamsBuilder
        .addTechParamId("12")
        .addComplectationId("5")

      val filters = searchMappings
        .enrichCatalogFilter(Cars, params.build(), Set(ClientFeature.SEARCH_CATALOG_FILTER_CARS))

      filters.getCatalogFilterCount shouldBe 1
    }

    "not map old filters to catalogFilters when have many filters with complectationName and Id" in {
      val params = SearchRequestParameters
        .newBuilder()
      params.getCarsParamsBuilder
        .addComplectationId("5")
        .addComplectationId("7")
        .addComplectationName("name")

      val filters = searchMappings
        .enrichCatalogFilter(Cars, params.build(), Set(ClientFeature.SEARCH_CATALOG_FILTER_CARS))

      filters.getCatalogFilterCount shouldBe 0
    }

    "not map old filters to catalogFilters when have many filter with many conditions counting grouping" in {
      val params = SearchRequestParameters
        .newBuilder()
      params.setGroupingId("tech_param_id=12,complectation_id=5")
      params.getCarsParamsBuilder
        .addTechParamId("13")
        .addComplectationId("5")

      val filters = searchMappings
        .enrichCatalogFilter(Cars, params.build(), Set(ClientFeature.SEARCH_CATALOG_FILTER_CARS))

      filters.getCatalogFilterCount shouldBe 0
    }

    "support moto filters mapping" in {
      val params = SearchRequestParameters
        .newBuilder()
        .addMarkModelNameplate("VENDOR1")

      val filters = searchMappings
        .enrichCatalogFilter(Moto, params.build(), Set(ClientFeature.SEARCH_CATALOG_FILTER_MOTO))

      filters.getCatalogFilterCount shouldBe 1
    }

    "throw exception on incomplete catalog filter starting from techparam" in {
      val params = SearchRequestParameters
        .newBuilder()
        .addCatalogFilter(CatalogFilter.newBuilder().setTechParam(21837981))
        .build()
      val request = ApiSearchRequest(Cars, params)

      an[IllegalArgumentException] shouldBe thrownBy {
        searchMappings.fromApiToSearcher(request, None)
      }
    }

    "throw exception on incomplete catalog filter starting from complectation" in {
      val params = SearchRequestParameters
        .newBuilder()
        .addCatalogFilter(CatalogFilter.newBuilder().setComplectation(21837981).setMark("test"))
        .build()
      val request = ApiSearchRequest(Cars, params)

      an[IllegalArgumentException] shouldBe thrownBy {
        searchMappings.fromApiToSearcher(request, None)
      }
    }

    "don't throw an exception on complete catalog filter" in {
      val params = SearchRequestParameters
        .newBuilder()
        .addCatalogFilter(
          CatalogFilter
            .newBuilder()
            .setTechParam(21837981)
            .setConfiguration(27362786)
            .setGeneration(127423)
            .setModel("test")
            .setMark("test")
        )
        .build()
      val request = ApiSearchRequest(Cars, params)

      searchMappings.fromApiToSearcher(request, None)
    }

    "erase offer_grouping on mixed search" in {
      val params = SearchRequestParameters
        .newBuilder()
        .setStateGroup(StateGroup.ALL)
        .setOfferGrouping(true)
        .build()
      val request = ApiSearchRequest(Cars, params)

      val res = searchMappings.fromApiToSearcher(request, None)
      res.params.get("offer_grouping") shouldBe None
    }

    "don't erase offer_grouping on new search" in {
      val params = SearchRequestParameters
        .newBuilder()
        .setStateGroup(StateGroup.NEW)
        .setOfferGrouping(true)
        .build()
      val request = ApiSearchRequest(Cars, params)

      val res = searchMappings.fromApiToSearcher(request, None)
      res.params.get("offer_grouping") shouldEqual Some(Set("true"))
    }

    "do not add default 'with_delivery' if there is no feature on request" in {
      val params = SearchRequestParameters.getDefaultInstance
      val request = ApiSearchRequest(Cars, params)
      val res = searchMappings.fromApiToSearcher(request, None)
      res.params should not contain key("with_delivery")
    }

    "add default 'with_delivery' if there is feature on request" in {
      implicit val customReq: RequestImpl = {
        val req = new RequestImpl
        req.setRequestParams(
          RequestParams.construct("1.1.1.1", xFeatures = Some(Set("SEARCH_WITH_DELIVERY_IN_DEFAULTS")))
        )
        req
      }

      val params = SearchRequestParameters.getDefaultInstance
      val request = ApiSearchRequest(Cars, params)
      val res = searchMappings.fromApiToSearcher(request, None)(customReq)
      (res.params should contain).key("with_delivery")
    }

    "ignore invalid yexpflags json" in {
      val apiSearchRequest = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .setCarsParams(
            CarsSearchRequestParameters
              .newBuilder()
              .addEngineGroup(EngineGroup.ATMO)
              .addEngineGroup(EngineGroup.DIESEL)
              .addEngineGroup(EngineGroup.LPG)
          )
          .setDamageGroup(DamageGroup.NOT_BEATEN)
          .build()
      )
      val yExpFlags = Base64.getEncoder.encodeToString("[{]}".stripMargin.getBytes())
      implicit val expFlagsReq: RequestImpl = {
        val req = new RequestImpl
        req.setRequestParams(
          RequestParams.construct(
            ip = "0.0.0.0",
            uaasResponse = new UaasResponse(
              expFlags = Some(yExpFlags),
              expConfigVersion = None,
              expBoxesCrypted = None,
              expBoxes = None
            )
          )
        )
        req
      }
      val resultReq = searchMappings.fromApiToSearcher(apiSearchRequest, None)(expFlagsReq)
      val result = resultReq.params

      //noinspection ScalaStyle
      result(ENGINE_TYPE) shouldEqual Set(EngineType.DIESEL).map(_.toString)
      result(STATE) shouldEqual Set(State.NEW, State.USED).map(_.toString)
    }

    "search for beaten electro cars instead of (new, used) diesels" in {
      val apiSearchRequest = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .setCarsParams(
            CarsSearchRequestParameters
              .newBuilder()
              .addEngineGroup(EngineGroup.DIESEL)
          )
          .setDamageGroup(DamageGroup.NOT_BEATEN)
          .build()
      )

      val yExpFlags = Base64.getEncoder.encodeToString(
        ("[{\"HANDLER\":\"AUTO_RU\"," +
          "\"CONTEXT\":" +
          "{\"AUTO_RU\":" +
          "{\"SEARCHER\":" +
          "{\"engine_type\":\"ELECTRO\"," +
          "\"state\":\"BEATEN\"}}}}]").stripMargin.getBytes()
      )
      implicit val expFlagsReq: RequestImpl = {
        val req = new RequestImpl
        req.setRequestParams(
          RequestParams.construct(
            ip = "0.0.0.0",
            uaasResponse = new UaasResponse(
              expFlags = Some(yExpFlags),
              expConfigVersion = None,
              expBoxesCrypted = None,
              expBoxes = None
            )
          )
        )
        req
      }
      val resultReq = searchMappings.fromApiToSearcher(apiSearchRequest, None)(expFlagsReq)
      val result = resultReq.params

      //noinspection ScalaStyle
      result(ENGINE_TYPE) shouldEqual Set(EngineGroup.ELECTRO).map(_.toString)
      result(STATE) shouldEqual Set(State.BEATEN).map(_.toString)
    }
  }
}
