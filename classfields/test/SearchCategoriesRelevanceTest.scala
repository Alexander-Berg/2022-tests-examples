package ru.yandex.vertis.general.search.logic.test

import common.geobase.model.RegionIds
import common.zio.logging.Logging
import general.bonsai.category_model.Category
import general.search.vasgen.vasgen_model.{FacetResponse, SearchCountResponse, SearchOffersResponse}
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.globe.testkit.{TestDistrictManager, TestGeoService, TestMetroManager}
import ru.yandex.vertis.general.personal.testkit.TestPersonalBigBService
import ru.yandex.vertis.general.search.logic.CategoryPredictor.PredictedCategory
import ru.yandex.vertis.general.search.logic.Vasgen.RelevanceFilter
import ru.yandex.vertis.general.search.logic._
import ru.yandex.vertis.general.search.logic.validation.SearchRequestValidator
import ru.yandex.vertis.general.search.model._
import ru.yandex.vertis.general.search.testkit._
import vertis.vasgen.common.EmbeddedVector
import vertis.vasgen.query.{Filter, Grouping, Text}
import zio.random.Random
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation
import zio.{Has, IO, Ref, Task, UIO, ZIO, ZLayer, ZRef}

object SearchCategoriesRelevanceTest extends DefaultRunnableSpec {

  private def searcherLayer = {
    val vasgenMock = ZLayer.requires[Has[Vasgen.Service]]
    val lastBannedOffers = TestLastBannedOffers.empty
    val categoryPredictorMock = ZLayer.requires[Has[CategoryPredictor.Service]]
    val queryCleaner = TestQueryCleaner.CleanQuery(anything, Expectation.valueF(identity))
    val factorLogger = TestFactorLogger.empty
    val experimentsExtractor =
      TestExperimentsExtractor.GetSoftnessFlag(Expectation.value(Option.empty[Int])) &&
        TestExperimentsExtractor.GetForceCategoryChangeThreshold(Expectation.value(Option.empty[Float]))
    val globe = TestGeoService.layer
    val personal = TestPersonalBigBService.layer
    val bigBLogger = BigBUserInfoLogger.noop
    val validator = TestDistrictManager.empty ++ TestMetroManager.empty >>> SearchRequestValidator.live
    val testBonsaiSnapshot = BonsaiSnapshot(Seq.empty, Seq.empty)
    val vasgenFilterMapper =
      Logging.live ++ Ref
        .make(testBonsaiSnapshot)
        .toLayer ++ TestGeoService.layer >>> VasgenFilterMapper.live
    val factorsManager = TestSearchFactorsManager.empty
    val searcherInputLayers =
      Logging.live ++
        Random.live ++
        vasgenMock ++
        lastBannedOffers ++
        categoryPredictorMock ++
        queryCleaner ++
        factorLogger ++
        globe ++
        personal ++
        bigBLogger ++
        SearchEmbedder.noop ++
        validator ++
        TestTracingEmpty.pseudoEmpty ++
        TestSpellcheckerClientEmpty.empty ++
        TestFilterCreator.empty ++
        vasgenFilterMapper ++
        factorsManager ++
        experimentsExtractor ++
        RegionParser.noop ++
        RelatedSearchManager.noop ++
        ZRef.make(bonsaiSnaphot).toLayer

    searcherInputLayers >>> Searcher.live
  }

  val bonsaiSnaphot = BonsaiSnapshot(
    Seq(
      Category("root1"),
      Category("child1", parentId = "root1"),
      Category("leaf1", parentId = "child1"), // probability = 0.1f
      Category("leaf2", parentId = "child1"), // probability = 0.2f
      Category("child2", parentId = "root1"),
      Category("leaf3", parentId = "child2"),
      Category("root2"),
      Category("child3", parentId = "root2"),
      Category("leaf4", parentId = "child3"), // probability = 0.35f
      Category("root3"),
      Category("child4", parentId = "root3"), // probability = 0.35f
      Category("leaf5", parentId = "child4"),
      Category("leaf6", parentId = "child4")
    ),
    Seq.empty
  )

  val vasgenResponse = FacetResponse(
    valueRange = Seq.empty,
    facet = Seq(
      FacetResponse.Sample(value = "leaf1", 100),
      FacetResponse.Sample(value = "leaf2", 200),
      FacetResponse.Sample(value = "leaf3", 300),
      FacetResponse.Sample(value = "leaf4", 400),
      FacetResponse.Sample(value = "leaf5", 500)
    )
  )

  val predictedCategories = List(
    PredictedCategory("leaf1", probability = 0.1f),
    PredictedCategory("leaf2", probability = 0.2f),
    PredictedCategory("leaf4", probability = 0.35f),
    PredictedCategory("child4", probability = 0.35f)
  )

  val predictor = UIO(new CategoryPredictor.Service {

    override def predictCategory(query: String, limit: Option[Int]): Task[List[PredictedCategory]] =
      ZIO.succeed(predictedCategories)
  }).toLayer

  val vasgen = UIO(new Vasgen.Service {

    override def search(
        filter: Filter,
        text: Option[Text],
        page: Page,
        sort: SearchSort,
        extraParams: Vasgen.ExtraParams,
        rankingVectors: List[EmbeddedVector],
        grouping: Option[Grouping],
        searchRequestId: String): IO[VasgenCommunicationError, SearchOffersResponse] = ???

    override def count(
        filter: Filter,
        text: Option[Text],
        relevanceFilter: Option[RelevanceFilter],
        softness: Option[Int],
        experiments: List[String]): IO[VasgenCommunicationError, SearchCountResponse] = ???

    override def facet(
        filter: Filter,
        text: Option[Text],
        facet: Option[Vasgen.Facet],
        rangeFields: Seq[String],
        relevanceFilter: Option[RelevanceFilter],
        softness: Option[Int],
        experiments: List[String]): IO[VasgenCommunicationError, FacetResponse] = ZIO.succeed(vasgenResponse)

    override def findObject(offerId: String): IO[VasgenCommunicationError, SearchOffersResponse] = ???
  }).toLayer

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("SearchCategoriesRelevanceTest")(
      testM("сортировать категории с учетом их релевантности") {
        val res =
          for {
            categories <- Searcher.searchCategories(
              text = "текст",
              categoryIds = Seq.empty,
              area = Toponyms(region = RegionIds.Russia, Seq.empty, Seq.empty),
              parameters = Seq.empty,
              orderByRelevance = true,
              lockedFields = LockedFields(Set(LockedField.Text, LockedField.Category)),
              searchContext = SearchContext.Unset
            )
          } yield assert(categories.leafCategories)(
            equalTo(
              Seq(
                SearchCategory("leaf4", 400),
                SearchCategory("leaf2", 200),
                SearchCategory("leaf1", 100),
                SearchCategory("leaf5", 500),
                SearchCategory("leaf3", 300)
              )
            )
          ) && assert(categories.adjacentCategories)(
            equalTo(
              Seq(
                SearchCategory("root3", 500),
                SearchCategory("root2", 400),
                SearchCategory("root1", 600)
              )
            )
          )
        res.provideLayer(vasgen ++ predictor >>> searcherLayer)
      },
      testM("сортировать категории без учета их релевантности") {
        val res =
          for {
            categories <- Searcher.searchCategories(
              text = "текст",
              categoryIds = Seq.empty,
              area = Toponyms(region = RegionIds.Russia, Seq.empty, Seq.empty),
              parameters = Seq.empty,
              orderByRelevance = false,
              lockedFields = LockedFields(Set(LockedField.Text, LockedField.Category)),
              searchContext = SearchContext.Unset
            )
          } yield assert(categories.leafCategories)(
            equalTo(
              Seq(
                SearchCategory("leaf5", 500),
                SearchCategory("leaf4", 400),
                SearchCategory("leaf3", 300),
                SearchCategory("leaf2", 200),
                SearchCategory("leaf1", 100)
              )
            )
          ) && assert(categories.adjacentCategories)(
            equalTo(
              Seq(
                SearchCategory("root1", 600),
                SearchCategory("root3", 500),
                SearchCategory("root2", 400)
              )
            )
          )
        res.provideLayer(vasgen ++ predictor >>> searcherLayer)
      },
      testM("приходят примыкающие категории дочерние от выбранной") {
        val res =
          for {
            categories <- Searcher.searchCategories(
              text = "текст",
              categoryIds = Seq("root1"),
              area = Toponyms(region = RegionIds.Russia, Seq.empty, Seq.empty),
              parameters = Seq.empty,
              orderByRelevance = true,
              lockedFields = LockedFields(Set(LockedField.Text)),
              searchContext = SearchContext.Unset
            )
          } yield assert(categories.adjacentCategories)(
            equalTo(
              Seq(
                SearchCategory("child1", 300),
                SearchCategory("child2", 300)
              )
            )
          )
        res.provideLayer(vasgen ++ predictor >>> searcherLayer)
      },
      testM("при выбранной листовой категории приходит пустой список примыкающих категорий") {
        val res =
          for {
            categories <- Searcher.searchCategories(
              text = "текст",
              categoryIds = Seq("leaf4"),
              area = Toponyms(region = RegionIds.Russia, Seq.empty, Seq.empty),
              parameters = Seq.empty,
              orderByRelevance = true,
              lockedFields = LockedFields(Set(LockedField.Text)),
              searchContext = SearchContext.Unset
            )
          } yield assert(categories.adjacentCategories)(isEmpty)
        res.provideLayer(vasgen ++ predictor >>> searcherLayer)
      }
    )
  }
}
