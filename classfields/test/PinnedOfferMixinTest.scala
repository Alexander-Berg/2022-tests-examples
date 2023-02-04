package ru.yandex.vertis.general.search.logic.test

import com.google.protobuf.ByteString
import common.geobase.model.RegionIds.RegionId
import common.zio.testkit.protogen.ProtoGen
import general.bonsai.category_model.Category
import general.common.address_model.{GeoPoint, RegionInfo, SellingAddress}
import general.common.pagination.PageStatistics
import general.common.seller_model.SellerId
import general.hammer.export_model.LastBannedOffers
import general.search.vasgen.vasgen_model.{SearchOffersResponse, SellingAddressWrapper, VasgenSearchSnippet}
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.common.cache.{Cache, RequestCacher}
import ru.yandex.vertis.general.common.resources.banned_offers.BannedOffersSnapshot
import ru.yandex.vertis.general.globe.testkit.{TestDistrictManager, TestGeoService, TestMetroManager}
import ru.yandex.vertis.general.personal.testkit.TestPersonalBigBService
import ru.yandex.vertis.general.search.constants.Constants
import ru.yandex.vertis.general.search.logic.CategoryPredictor.PredictedCategory
import ru.yandex.vertis.general.search.logic.Searcher.Searcher
import ru.yandex.vertis.general.search.logic.Vasgen.UserFactors
import ru.yandex.vertis.general.search.logic.validation.SearchRequestValidator
import ru.yandex.vertis.general.search.logic._
import ru.yandex.vertis.general.search.model._
import ru.yandex.vertis.general.search.testkit._
import common.zio.logging.Logging
import vertis.vasgen.common.{RawValue => VasgenRawValue}
import vertis.vasgen.query.{Eq, Filter}
import zio.random.{Random => ZRandom}
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test.magnolia.DeriveGen
import zio.test.mock.Expectation
import zio.test.mock.Expectation.{toLayer, value}
import zio.test.{DefaultRunnableSpec, _}
import zio.{Has, Ref, ZIO, ZLayer, ZRef}

object PinnedOfferMixinTest extends DefaultRunnableSpec {

  implicit private lazy val genByteString = DeriveGen.instance(Gen.alphaNumericString.map(ByteString.copyFromUtf8))

  private lazy val genSearchOffersResponse = ProtoGen[SearchOffersResponse].noShrink
    .map(_.copy(pageStatistics = Some(PageStatistics(1000, 10))))

  private lazy val sellerIdUser = Gen.alphaNumericStringBounded(10, 20).map(SellerId.SellerId.StoreId)

  private lazy val sellerIdStore = Gen.anyLong.map(SellerId.SellerId.UserId)

  private lazy val genVasgenSearchSnippet = ProtoGen[VasgenSearchSnippet].noShrink
    .map(
      _.copy(addresses =
        Seq(
          SellingAddressWrapper(
            address = Some(
              SellingAddress(
                geopoint = Some(GeoPoint(latitude = 61.698653, longitude = 99.505405)),
                region = Some(RegionInfo(id = 225, isEnriched = true, name = "Россия"))
              )
            )
          )
        )
      )
    )
    .flatMap(snippet =>
      for {
        id <- Gen.alphaNumericStringBounded(10, 20)
        sellerId <- Gen.oneOf(sellerIdUser, sellerIdStore).map(id => SellerId.defaultInstance.copy(sellerId = id))
      } yield snippet.copy(offerId = id, sellerId = Some(sellerId))
    )

  private def searcherLayer = {
    val searchMock = ZLayer.requires[Has[Vasgen.Service]]
    val lastBannedOffers =
      TestLastBannedOffers.Snapshot(returns = value(BannedOffersSnapshot(LastBannedOffers(Seq.empty[String]))))
    val categoryPredictor =
      TestCategoryPredictor.PredictCategory(anything, value(List(PredictedCategory("categoryId", 1.5f))))
    val queryCleaner = TestQueryCleaner.CleanQuery(anything, Expectation.valueF(identity))
    val factorLogger = TestFactorLogger.LogFactorsAsync(anything)
    val globe = TestGeoService.layer
    val personal = TestPersonalBigBService.layer
    val bigBLogger = BigBUserInfoLogger.noop
    val validator = TestDistrictManager.empty ++ TestMetroManager.empty >>> SearchRequestValidator.live
    val testBonsaiSnapshot = BonsaiSnapshot(Seq(Category(id = "test_category", name = "test category")), Seq.empty)
    val experimentsExtractor =
      TestExperimentsExtractor.GetSoftnessFlag(Expectation.value(Option.empty[Int])) &&
        TestExperimentsExtractor.GetFulltextRelevanceFormulaName(Expectation.value(Option.empty[String])) &&
        TestExperimentsExtractor.GetVasesInTopNumber(Expectation.value(Option.empty[Int])) &&
        TestExperimentsExtractor.GetShowRaiseVasInCategoryListing(Expectation.value(Option.empty[Boolean])) &&
        TestExperimentsExtractor.GetForceCategoryChangeThreshold(Expectation.value(Option.empty[Float])) &&
        TestExperimentsExtractor.GetReverseSearchPageFlag(Expectation.value(false))
    val vasgenFilterMapper =
      Logging.live ++ Ref
        .make(testBonsaiSnapshot)
        .toLayer ++ TestGeoService.layer >>> VasgenFilterMapper.live
    val factorsManager = TestSearchFactorsManager.CreateRefineFactors(anything, value(List.empty)) ++
      TestSearchFactorsManager.CreateUserFactors(anything, value(UserFactors(Nil, Nil)))

    val searcherInputLayers =
      Logging.live ++
        ZRandom.live ++
        searchMock ++
        lastBannedOffers ++
        categoryPredictor ++
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
        ZRef.make(BonsaiSnapshot(List.empty, List.empty)).toLayer

    searcherInputLayers >>> Searcher.live
  }

  private def makeSearchRequest(
      pinnedOfferId: Option[String],
      pageNum: Int,
      discardedOfferIds: List[String] = List.empty): ZIO[Searcher, SearchError, Searcher.SearchResult] = {
    Searcher.searchOffers(
      text = "двигатель",
      categoryIds = Seq.empty,
      pinnedOfferId = pinnedOfferId,
      area = Toponyms(region = RegionId(225), metro = Seq.empty, districts = Seq.empty),
      parameters = Seq.empty,
      pageToken =
        PageToken(pageNum = pageNum, pageSize = 70, discardedOfferIds = discardedOfferIds, expansionLevel = 0, None),
      sort = SearchSort.ByRelevance,
      requester = None,
      searchContext = SearchContext.Unset,
      lockedFields = LockedFields(Set(LockedField.Text))
    )
  }

  private def snippetToVertisAny(snippet: VasgenSearchSnippet): ru.yandex.vertis.sraas.any.Any = {
    val protobufAnySnippet = com.google.protobuf.any.Any.pack(snippet)
    ru.yandex.vertis.sraas.any.Any(
      typeUrl = protobufAnySnippet.typeUrl,
      value = protobufAnySnippet.value
    )
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val genSearchResponse = genSearchOffersResponse.filter(x => x.pageStatistics.isDefined && x.snippets.nonEmpty)
    val genVasgenSnippets = Gen
      .int(3, 10)
      .flatMap(Gen.listOfN(_)(genVasgenSearchSnippet.noShrink).noShrink)

    val generateResponseWithSnippets: Gen[ZRandom with Sized, (List[VasgenSearchSnippet], SearchOffersResponse)] = {
      for {
        response <- genSearchResponse
        snippets <- genVasgenSnippets
      } yield (snippets, response.copy(snippets = snippets.map(snippetToVertisAny)))
    }

    suite("PinnedOfferMixinTest")(
      testM("return default list if pinnedOfferId is undefined") {
        checkNM(1)(genSearchResponse, genVasgenSnippets) { (randomSearchResponse, randomVasgenSnippets) =>
          val sampleSearchResponse = randomSearchResponse.copy(snippets = randomVasgenSnippets.map(snippetToVertisAny))
          val vasgenLayer = TestVasgen.Search(anything, value(sampleSearchResponse))
          makeSearchRequest(pinnedOfferId = None, pageNum = 0)
            .provideLayer(vasgenLayer >>> searcherLayer)
            .map { searchResult =>
              assert(randomVasgenSnippets.map(_.offerId))(equalTo(searchResult.snippets.map(_.offerId))) &&
              assert(searchResult.pageStatistics.totalCount)(
                equalTo(sampleSearchResponse.pageStatistics.get.totalCount)
              )
            }
        }
      },
      testM("return default list with pinned offer if pinnedOfferId is defined") {
        checkNM(1)(generateResponseWithSnippets, generateResponseWithSnippets, Gen.alphaNumericString) {
          case ((defaultSearchSnippets, defaultSearchResponse), (pinnedSnippet, pinnedOfferResponse), pinnedOfferId) =>
            val vasgenLayer =
              TestVasgen.Search(anything, value(defaultSearchResponse)) &&
                TestVasgen.FindObject(equalTo(pinnedOfferId), value(pinnedOfferResponse))
            makeSearchRequest(pinnedOfferId = Some(pinnedOfferId), pageNum = 0)
              .provideLayer(vasgenLayer >>> searcherLayer)
              .map { actual =>
                val expectedOfferIds = (pinnedSnippet ++ defaultSearchSnippets).map(_.offerId)
                assert(actual.snippets.map(_.offerId))(equalTo(expectedOfferIds)) &&
                assert(actual.pageStatistics.totalCount)(equalTo(defaultSearchResponse.pageStatistics.get.totalCount))
              }
        }
      },
      testM(
        "if pinnedOfferId is defined and it is found on the first page of default listing, " +
          "it should be moved to the beginning of the response list"
      ) {
        checkNM(1)(generateResponseWithSnippets, genSearchResponse) {
          case ((defaultSearchSnippets, defaultSearchResponse), randomSearchResponse) =>
            val pinnedOffer = defaultSearchSnippets.last
            val pinnedSearchResponse = randomSearchResponse.copy(snippets = List(snippetToVertisAny(pinnedOffer)))
            val snippetsWithoutPinnedOffer = defaultSearchSnippets.filter(_.offerId != pinnedOffer.offerId)
            val responseWithoutPinnedOffer =
              defaultSearchResponse.copy(snippets = snippetsWithoutPinnedOffer.map(snippetToVertisAny))
            val vasgenLayer =
              TestVasgen.Search(
                anything,
                value(responseWithoutPinnedOffer)
              ) &&
                TestVasgen.FindObject(equalTo(pinnedOffer.offerId), value(pinnedSearchResponse))
            makeSearchRequest(pinnedOfferId = Some(pinnedOffer.offerId), pageNum = 0)
              .provideLayer(vasgenLayer >>> searcherLayer)
              .map { actual =>
                val expectedSnippets = pinnedOffer +: snippetsWithoutPinnedOffer
                assert(actual.snippets.map(_.offerId))(equalTo(expectedSnippets.map(_.offerId))) &&
                assert(actual.pageStatistics.totalCount)(
                  equalTo(responseWithoutPinnedOffer.pageStatistics.get.totalCount)
                ) &&
                assert(actual.nextPageToken)(
                  isSome(hasField("discardedOfferIds", _.discardedOfferIds, equalTo(List(pinnedOffer.offerId))))
                )
              }
        }
      },
      testM("return pinned offer page stats if default search list is empty but pinned offer is found") {
        checkNM(1)(generateResponseWithSnippets, generateResponseWithSnippets, Gen.alphaNumericString) {
          case ((_, defaultSearchResponse), (pinnedSnippets, pinnedOfferResponse), pinnedOfferId) =>
            val fixedPinnedOfferResponse = pinnedOfferResponse.copy(
              pageStatistics = Some(
                pinnedOfferResponse.pageStatistics.get
                  .copy(totalCount = pinnedOfferResponse.snippets.size, totalPageCount = 1)
              )
            )
            val emptyDefaultResponse =
              defaultSearchResponse.copy(snippets = Seq.empty, pageStatistics = Some(PageStatistics()))
            val vasgenLayer =
              TestVasgen.Search(anything, value(emptyDefaultResponse)) &&
                TestVasgen.FindObject(equalTo(pinnedOfferId), value(fixedPinnedOfferResponse))
            makeSearchRequest(pinnedOfferId = Some(pinnedOfferId), pageNum = 0)
              .provideLayer(vasgenLayer >>> searcherLayer)
              .map { actual =>
                assert(actual.snippets.map(_.offerId))(equalTo(pinnedSnippets.map(_.offerId))) &&
                assert(actual.pageStatistics)(equalTo(fixedPinnedOfferResponse.pageStatistics.get))
              }
        }
      },
      testM(
        "if pinnedOfferId is NOT defined but page token with discardedOfferIds is provided " +
          "do not search for pinned offer"
      ) {
        checkNM(1)(generateResponseWithSnippets) { case (defaultSearchSnippets, defaultSearchResponse) =>
          val pinnedOffer = defaultSearchSnippets.head
          val removePinnedOfferFilter = Filter(
            not = Some(
              Filter(
                op = Filter.Op.Eq(
                  Eq(
                    field = Constants.VasgenPrimaryKeyFieldName,
                    value = Some(VasgenRawValue(VasgenRawValue.ValueTypeOneof.String(pinnedOffer.offerId)))
                  )
                )
              )
            )
          )
          val responseWithoutPinnedOffer =
            defaultSearchResponse.copy(snippets = defaultSearchSnippets.tail.map(snippetToVertisAny))
          val vasgenLayer = TestVasgen.Search(
            hasField("and filters", _._1.and, hasSubset[Filter](List(removePinnedOfferFilter))),
            value(responseWithoutPinnedOffer)
          )
          makeSearchRequest(pinnedOfferId = None, pageNum = 2, discardedOfferIds = List(pinnedOffer.offerId))
            .provideLayer(vasgenLayer >>> searcherLayer)
            .map { actual =>
              assert(actual.snippets.map(_.offerId))(equalTo(defaultSearchSnippets.tail.map(_.offerId))) &&
              assert(actual.pageStatistics.totalCount)(
                equalTo(responseWithoutPinnedOffer.pageStatistics.get.totalCount)
              ) &&
              assert(actual.nextPageToken)(
                isSome(hasField("discardedOfferIds", _.discardedOfferIds, equalTo(List(pinnedOffer.offerId))))
              )
            }
        }
      },
      testM(
        "if pinnedOfferId is defined but response of both requests are empty" +
          "response list should contain PageStats(0,0) and empty snippets' list "
      ) {
        checkNM(1)(generateResponseWithSnippets, generateResponseWithSnippets, Gen.alphaNumericString) {
          case ((_, randomDefaultResponse), (_, randomPinnedResponse), pinnedOfferId) =>
            val defaultSearchResponse = randomDefaultResponse.copy(snippets = Seq.empty)
            val pinnedSearchResponse = randomPinnedResponse.copy(snippets = Seq.empty)
            val vasgenLayer = TestVasgen.Search(anything, value(defaultSearchResponse)) &&
              TestVasgen.FindObject(equalTo(pinnedOfferId), value(pinnedSearchResponse))
            makeSearchRequest(pinnedOfferId = Some(pinnedOfferId), pageNum = 0)
              .provideLayer(vasgenLayer >>> searcherLayer)
              .map { actual =>
                assert(actual.snippets.map(_.offerId))(equalTo(Seq.empty)) &&
                assert(actual.pageStatistics.totalCount)(equalTo(defaultSearchResponse.pageStatistics.get.totalCount))
              }
        }
      }
    )
  } @@ sequential
}
