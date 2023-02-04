package ru.auto.api.util.search

import ru.auto.api.TrucksModel.{LightTruck, TruckCategory}
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.TestRequestWithId
import ru.auto.api.model.CategorySelector.{Cars, Moto, Trucks}
import ru.auto.api.model.searcher.ApiSearchRequest
import ru.auto.api.model.{Paging, Sorting}
import ru.auto.api.search.SearchModel.{CarsSearchRequestParameters, CatalogFilter, MotoSearchRequestParameters, SearchRequestParameters, TrucksSearchRequestParameters}
import ru.auto.api.ui.UiModel._
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.auto.api.util.{Protobuf, RequestImpl}
import ru.auto.api.{BaseSpec, CarsModel, MotoModel}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

class SearchDiffUtilsSpec extends BaseSpec with TestRequestWithId with MockitoSupport {
  implicit val paging: Paging = Paging.Default
  implicit val sorting: Sorting = SearcherRequestMapper.topSortings.head

  val featureManager: FeatureManager = mock[FeatureManager]
  val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)
  when(featureManager.oldOptionsSearchMapping).thenReturn(feature)

  val defaultsMapper = new DefaultsMapper(featureManager)
  val searchMappings: SearchMappings = new SearchMappings(defaultsMapper, featureManager)
  val searchDiffUtils = new SearchDiffUtils(searchMappings, defaultsMapper)

  "SearchDiffUtils" should {
    "calculate correct diff count for cars filter with default state" in {
      val filter = ApiSearchRequest(
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
          .setDamageGroup(DamageGroup.NOT_BEATEN)
          .setYearFrom(100)
          .setYearTo(200)
          .addAllCatalogEquipment(Seq("1", "2").asJava)
          .build()
      )

      searchDiffUtils.diffFromDefault(filter).size shouldBe 10
    }

    "calculate correct diff count for cars filter with non default state" in {
      val filter = ApiSearchRequest(
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
          .setStateGroup(StateGroup.NEW)
          .setInStock(Stock.ANY_STOCK)
          .build()
      )

      searchDiffUtils.diffFromDefault(filter).size shouldBe 7
    }

    "calculate correct diff count for truck filter" in {
      val filter = ApiSearchRequest(
        Trucks,
        SearchRequestParameters
          .newBuilder()
          .setTrucksParams(
            TrucksSearchRequestParameters
              .newBuilder()
              .setTrucksCategory(TruckCategory.LCV)
              .addLightTruckType(LightTruck.BodyType.PICKUP)
          )
          .setOwnersCountGroup(OwnersCountGroup.LESS_THAN_TWO)
          .setOwningTimeGroup(OwningTimeGroup.MORE_THAN_3)
          .setCustomsStateGroup(CustomsGroup.DOESNT_MATTER)
          .setExchangeGroup(ExchangeGroup.POSSIBLE)
          .addSellerGroup(SellerGroup.COMMERCIAL)
          .setStateGroup(StateGroup.NEW)
          .setInStock(Stock.ANY_STOCK)
          .build()
      )

      searchDiffUtils.diffFromDefault(filter).size shouldBe 6
    }

    "AUTORUAPI-4998 test #1" in {
      val filter = ApiSearchRequest(
        Cars,
        Protobuf.fromJson[SearchRequestParameters](
          "{\"hasImage\":true,\"offerGrouping\":true,\"stateGroup\":\"ALL\",\"sellerGroup\":[\"ANY_SELLER\"],\"damageGroup\":\"NOT_BEATEN\",\"customsStateGroup\":\"CLEARED\"}"
        )
      )
      searchDiffUtils.diffFromDefault(filter).size shouldBe 0
    }

    "AUTORUAPI-4998 test #2" in {
      val filter = ApiSearchRequest(
        Cars,
        Protobuf.fromJson[SearchRequestParameters](
          "{\"hasImage\":true,\"inStock\":\"IN_STOCK\",\"offerGrouping\":true,\"stateGroup\":\"NEW\",\"damageGroup\":\"NOT_BEATEN\",\"customsStateGroup\":\"DOESNT_MATTER\"}"
        )
      )
      searchDiffUtils.diffFromDefault(filter).size shouldBe 2
    }

    "AUTORUAPI-5479 test #3" in {
      val filter = ApiSearchRequest(
        Moto,
        Protobuf.fromJson[SearchRequestParameters](
          "{\"motoParams\":{\"motoCategory\":\"MOTORCYCLE\"},\"hasImage\":true,\"inStock\":\"ANY_STOCK\",\"stateGroup\":\"ALL\",\"sellerGroup\":[\"ANY_SELLER\"],\"damageGroup\":\"NOT_BEATEN\",\"customsStateGroup\":\"DOESNT_MATTER\"}"
        )
      )
      searchDiffUtils.diffFromDefault(filter).size shouldBe 0
    }

    "AUTORUAPI-4998 test #4" in {
      val filter = ApiSearchRequest(
        Moto,
        Protobuf.fromJson[SearchRequestParameters](
          "{\"motoParams\":{\"motoCategory\":\"MOTORCYCLE\"},\"hasImage\":true,\"inStock\":\"IN_STOCK\",\"stateGroup\":\"NEW\",\"damageGroup\":\"NOT_BEATEN\",\"customsStateGroup\":\"DOESNT_MATTER\"}"
        )
      )
      searchDiffUtils.diffFromDefault(filter).size shouldBe 2
    }

    "ignore car engine_type" in {
      val filter = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .setCarsParams(
            CarsSearchRequestParameters
              .newBuilder()
              .addEngineType(CarsModel.Car.EngineType.HYBRID)
          )
          .build()
      )

      searchDiffUtils.diffFromDefault(filter).size shouldBe 0
    }

    "not ignore moto engine_type" in {
      val filter = ApiSearchRequest(
        Moto,
        SearchRequestParameters
          .newBuilder()
          .setMotoParams(
            MotoSearchRequestParameters
              .newBuilder()
              .addEngineType(MotoModel.Moto.Engine.ELECTRO)
          )
          .build()
      )

      val fields = searchDiffUtils.diffFromDefault(filter)
      fields.size shouldBe 1
      fields should contain("engine_type")
    }

    "correct counting of withDelivery default for any state group" in {
      implicit val r = mock[RequestImpl]
      when(r.isSupported(?)).thenReturn(true)

      val filter = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .setStateGroup(StateGroup.ALL)
          .setWithDelivery(TristateTumblerGroup.BOTH)
          .build()
      )

      searchDiffUtils.diffFromDefault(filter).size shouldBe 0

      val another = filter.copy(params = filter.params.toBuilder.setWithDelivery(TristateTumblerGroup.NONE).build)

      searchDiffUtils.diffFromDefault(another).size shouldBe 1
    }

    "correct counting of withDelivery default for new state group" in {
      implicit val r = mock[RequestImpl]
      when(r.isSupported(?)).thenReturn(true)

      val filter = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .setStateGroup(StateGroup.NEW)
          .setWithDelivery(TristateTumblerGroup.BOTH)
          .build()
      )

      searchDiffUtils.diffFromDefault(filter).size shouldBe 2

      val another = filter.copy(params = filter.params.toBuilder.setWithDelivery(TristateTumblerGroup.NONE).build)

      searchDiffUtils.diffFromDefault(another).size shouldBe 1
    }

    "correct counting of exclude catalog filter if necessary features wasn't provided" in {
      implicit val r = mock[RequestImpl]
      when(r.isSupported(?)).thenReturn(false)

      val filter = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .addExcludeCatalogFilter(CatalogFilter.newBuilder().setMark("Audi").build)
          .build()
      )

      searchDiffUtils.diffFromDefault(filter).size shouldBe 1
    }

    "correct counting of exclude catalog filter if necessary features was provided" in {
      implicit val r = mock[RequestImpl]
      when(r.isSupported(?)).thenReturn(true)

      val filter = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .addExcludeCatalogFilter(CatalogFilter.newBuilder().setMark("Audi").build)
          .build()
      )

      searchDiffUtils.diffFromDefault(filter).size shouldBe 0
    }

    "correct counting of default onlyNds search" in {
      implicit val r = mock[RequestImpl]
      when(r.isSupported(?)).thenReturn(true)

      val filter = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .setOnlyNds(false)
          .build()
      )

      searchDiffUtils.diffFromDefault(filter).size shouldBe 0
    }

    "correct counting of non default onlyNds search" in {
      implicit val r = mock[RequestImpl]
      when(r.isSupported(?)).thenReturn(true)

      val filter = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .setOnlyNds(true)
          .build()
      )

      searchDiffUtils.diffFromDefault(filter).size shouldBe 1
    }

    "correct counting of non default onlyOfficial search" in {
      implicit val r = mock[RequestImpl]
      when(r.isSupported(?)).thenReturn(true)

      val filter = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .setOnlyOfficial(true)
          .build()
      )

      searchDiffUtils.diffFromDefault(filter).size shouldBe 0
    }

    "correct counting of default onlyOfficial search" in {
      implicit val r = mock[RequestImpl]
      when(r.isSupported(?)).thenReturn(true)

      val filter = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .setOnlyOfficial(false)
          .build()
      )

      searchDiffUtils.diffFromDefault(filter).size shouldBe 0
    }

    "correct counting of non default onlyOfficial complex search" in {
      implicit val r = mock[RequestImpl]
      when(r.isSupported(?)).thenReturn(true)

      val filter = ApiSearchRequest(
        Cars,
        SearchRequestParameters
          .newBuilder()
          .setOnlyOfficial(true)
          .setOnlyNds(true)
          .build()
      )

      searchDiffUtils.diffFromDefault(filter).size shouldBe 1
    }
  }
}
