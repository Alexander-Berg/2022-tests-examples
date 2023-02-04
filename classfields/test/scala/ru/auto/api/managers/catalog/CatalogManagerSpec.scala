package ru.auto.api.managers.catalog

import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.CatalogModel.DictionaryV1
import ru.auto.api.ResponseModel.{BreadcrumbsResponse, DictionaryResponse}
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.dictionaries.DictionariesManager
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model.{CarSuggestParams, CategorySelector, ModelGenerators, RequestParams}
import ru.auto.api.services.catalog.CatalogClient
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eqq}
import ru.auto.catalog.model.api.ApiModel.RawFilterRequest.ReturnModeCase
import ru.auto.catalog.model.api.ApiModel.{CatalogByTag, CatalogByTagRequest, CatalogByTagResult, CatalogCardsWithoutOffersRequest, MarkCard, RawCatalog, RawFilterRequest, State}
import ru.yandex.auto.searcher.filters.MarkModelFilters.{MarkFiltersEntryMessage, MarkModelFiltersResultMessage, ModelFiltersEntryMessage, SupergenFiltersEntryMessage}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class CatalogManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with BeforeAndAfter {

  trait mocks {
    val catalogClient = mock[CatalogClient]
    val searcherClient = mock[SearcherClient]
    val featureManager = mock[FeatureManager]
    val dictionariesManager = mock[DictionariesManager]

    lazy val manager = new CatalogManager(catalogClient, searcherClient, featureManager, dictionariesManager)
    implicit val trace: Traced = Traced.empty

    implicit val request: RequestImpl = {
      val req = new RequestImpl
      req.setApplication(Application.desktop)
      req.setTrace(trace)
      req.setRequestParams(RequestParams.empty)
      req
    }
  }

  "BreadcrumbsManager" should {
    "call searcher manager for moto category" in new mocks {
      val filter = Map.empty[String, Set[String]]
      when(searcherClient.breadcrumbs(CategorySelector.Moto, filter))
        .thenReturnF(BreadcrumbsResponse.getDefaultInstance)
      val feature = mock[Feature[Boolean]]
      when(feature.value).thenReturn(true)
      when(featureManager.catalogBreadcrumbs).thenReturn(feature)
      val result = manager.breadcrumbs(CategorySelector.Moto, filter).futureValue
      verifyNoMoreInteractions(catalogClient)
      verify(searcherClient).breadcrumbs(CategorySelector.Moto, filter)
    }

    "parse parameters" in new mocks {
      val filter = Map(
        "bc_lookup" -> Set(
          "MERCEDES#GL_KLASSE#4986814#4986815#1",
          "BMW#1ER#7707451",
          "FORD#FOCUS",
          "AUDI"
        ),
        "rid" -> Set("1", "2", "unknown"),
        "state" -> Set("NEW", "USED", "unknown")
      )
      var filterRequestOpt = Option.empty[RawFilterRequest]
      val feature = mock[Feature[Boolean]]
      when(feature.value).thenReturn(true)
      when(featureManager.catalogBreadcrumbs).thenReturn(feature)
      when(catalogClient.filter(?, ?)(?)).thenAnswer((invocation: InvocationOnMock) => {
        val _ :: (r: RawFilterRequest) :: _ = invocation.getArguments.toList: @unchecked
        filterRequestOpt = Some(r)
        Future.successful(RawCatalog.getDefaultInstance)
      })
      manager.breadcrumbs(CategorySelector.Cars, filter).futureValue
      val filterRequest = filterRequestOpt.get

      shouldContainFilter(filterRequest, Some("MERCEDES"), Some("GL_KLASSE"), Some("4986814"), Some("4986815"))

      shouldContainFilter(filterRequest, Some("BMW"), Some("1ER"), Some("7707451"))
      shouldContainFilter(filterRequest, Some("FORD"), Some("FOCUS"))
      shouldContainFilter(filterRequest, Some("AUDI"))
      filterRequest.getReturnModeCase shouldBe ReturnModeCase.BREADCRUMBS
      filterRequest.getBreadcrumbs.getRegionIdList.asScala should contain theSameElementsAs List(1L, 2L)
      filterRequest.getBreadcrumbs.getStateList.asScala should contain theSameElementsAs List(State.NEW, State.USED)
    }

    "suggest marks" in new mocks {
      val mark = MarkCard.newBuilder().setEntity(ModelGenerators.entityGen().next).build()
      when(dictionariesManager.getDictionary("v1", Cars, "body_type"))
        .thenReturn(
          DictionaryResponse.newBuilder().setDictionaryV1(DictionaryV1.newBuilder().setVersion("").build()).build()
        )
      when(catalogClient.filter(?, ?)(?))
        .thenReturnF(RawCatalog.newBuilder().putMark(mark.getEntity.getId, mark).build())
      val res = manager.getSuggestForCreate(Cars, CarSuggestParams.empty).futureValue
      res.hasCarSuggest shouldBe true
      res.getCarSuggest.getMarksCount shouldBe 1
    }

    "suggest models and return all marks" in new mocks {
      val marks = ModelGenerators.generateMarkCards().next
      when(dictionariesManager.getDictionary("v1", Cars, "body_type"))
        .thenReturn(
          DictionaryResponse.newBuilder().setDictionaryV1(DictionaryV1.newBuilder().setVersion("").build()).build()
        )
      when(catalogClient.filter(?, ?)(?)).thenReturnF {
        RawCatalog.newBuilder().putAllMark(marks.map(m => m.getEntity.getId -> m).toMap.asJava).build()
      }
      val res =
        manager
          .getSuggestForCreate(Cars, CarSuggestParams.empty.copy(mark = Some(marks.head.getEntity.getId)))
          .futureValue

      res.hasCarSuggest shouldBe true

      res.getCarSuggest.getMarksList.asScala.map(_.getName).toSet shouldBe marks.map(_.getEntity.getName).toSet

      res.getCarSuggest.getModelsList.asScala.map(_.getName).toSet shouldBe marks.head.getModelMap.asScala.values
        .map(_.getEntity.getName)
        .toSet
    }
  }

  "CatalogCardsWithoutOffers test" should {
    "return all mark-model-etc filtets with None" in new mocks {

      val category = CategorySelector.Cars
      val tagList = Seq("new4new", "sometag")

      val supergen = "test"
      val markEntries = MarkFiltersEntryMessage
        .newBuilder()
        .addModels(
          ModelFiltersEntryMessage
            .newBuilder()
            .setModelCode("someCar")
            .addSupergens(SupergenFiltersEntryMessage.newBuilder().setSupergenId(supergen).build)
            .build()
        )
        .build()

      when(searcherClient.getMarkModelFilters(eqq(category), ?)(?)).thenReturn(
        Future.successful(
          MarkModelFiltersResultMessage
            .newBuilder()
            .addMarkEntries(markEntries)
            .build()
        )
      )
      val catalogByTagRequest = CatalogByTagRequest
        .newBuilder()
        .addAllTags(tagList.asJava)
        .addExcludeSuperGen(supergen)
        .build()
      val catalogByTagResponse = CatalogByTagResult.newBuilder().build()
      when(
        catalogClient.getCardsWithoutOffers(
          eqq(category),
          eqq(catalogByTagRequest),
          eqq(None),
          eqq(None),
          eqq(None),
          eqq(None)
        )(?)
      ).thenReturn(
        Future.successful(catalogByTagResponse)
      )
      val result = manager
        .getCatalogCardsWithoutOffers(
          CategorySelector.Cars,
          CatalogCardsWithoutOffersRequest
            .newBuilder()
            .addAllTags(tagList.asJava)
            .build()
        )
        .futureValue

      assert(result.getDataCount == 0)
      verify(searcherClient).getMarkModelFilters(eqq(category), ?)(?)
      verify(catalogClient).getCardsWithoutOffers(
        eqq(category),
        eqq(catalogByTagRequest),
        eqq(None),
        eqq(None),
        eqq(None),
        eqq(None)
      )(?)

    }
    "return all mark-model-etc filtets without None" in new mocks {

      val category = CategorySelector.Cars
      val tagList = Seq("new4new", "sometag")
      val supergen = "test"
      val markEntries = MarkFiltersEntryMessage
        .newBuilder()
        .addModels(
          ModelFiltersEntryMessage
            .newBuilder()
            .setModelCode("someCar")
            .addSupergens(SupergenFiltersEntryMessage.newBuilder().setSupergenId(supergen).build)
            .build()
        )
        .build()
      when(searcherClient.getMarkModelFilters(eqq(category), ?)(?)).thenReturn(
        Future.successful(
          MarkModelFiltersResultMessage
            .newBuilder()
            .addMarkEntries(markEntries)
            .build()
        )
      )
      val catalogByTagRequest = CatalogByTagRequest
        .newBuilder()
        .addAllTags(tagList.asJava)
        .addExcludeSuperGen(supergen)
        .build()
      val catalogByTagResponse = CatalogByTagResult
        .newBuilder()
        .addCatalogByTag(
          CatalogByTag.newBuilder().build()
        )
        .build()

      when(
        catalogClient.getCardsWithoutOffers(
          eqq(category),
          eqq(catalogByTagRequest),
          eqq(Some("mark")),
          eqq(Some("model")),
          eqq(Some("superGen")),
          eqq(Some("configuration"))
        )(?)
      ).thenReturn(
        Future.successful(catalogByTagResponse)
      )
      val result = manager
        .getCatalogCardsWithoutOffers(
          CategorySelector.Cars,
          CatalogCardsWithoutOffersRequest
            .newBuilder()
            .addAllTags(tagList.asJava)
            .build(),
          Some("mark"),
          Some("model"),
          Some("superGen"),
          Some("configuration")
        )
        .futureValue
      assert(result.getDataCount == 1)
      verify(searcherClient).getMarkModelFilters(eqq(category), ?)(?)
      verify(catalogClient).getCardsWithoutOffers(
        eqq(category),
        eqq(catalogByTagRequest),
        eqq(Some("mark")),
        eqq(Some("model")),
        eqq(Some("superGen")),
        eqq(Some("configuration"))
      )(?)

    }
  }

  private def shouldContainFilter(rawFilterRequest: RawFilterRequest,
                                  mark: Option[String] = None,
                                  model: Option[String] = None,
                                  superGen: Option[String] = None,
                                  configuration: Option[String] = None,
                                  techParam: Option[String] = None): Unit = {
    val candidates = rawFilterRequest.getFiltersList.asScala
    val byMark = mark.fold(candidates)(x => candidates.filter(_.getMark == x))
    val byModel = model.fold(byMark)(x => byMark.filter(_.getModel == x))
    val bySuperGen = superGen.fold(byModel)(x => byModel.filter(_.getSuperGen == x))
    val byConfiguration = configuration.fold(bySuperGen)(x => bySuperGen.filter(_.getConfiguration == x))
    val result = techParam.fold(byConfiguration)(x => byConfiguration.filter(_.getTechParam == x))
    result.size shouldBe 1
  }

}
