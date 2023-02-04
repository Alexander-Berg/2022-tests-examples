package ru.auto.api.util.search

import org.scalacheck.Gen
import org.scalatest.OptionValues
import ru.auto.api.BaseSpec
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.features.AppsFeaturesManager
import ru.auto.api.managers.searcher.SearchRequestContext.Type._
import ru.auto.api.model.{Paging, RequestParams, Sorting, SortingByField}
import ru.auto.api.model.CategorySelector._
import ru.auto.api.model.searcher.{ApiSearchRequest, GroupBy, SearcherRequest}
import ru.auto.api.search.SearchModel.SearchRequestParameters
import ru.auto.api.ui.UiModel.StateGroup
import ru.auto.api.util.RequestImpl
import ru.auto.api.util.search.SearcherRequestMapper._
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport._
import ru.yandex.vertis.tracing.Traced

class SearcherRequestMapperSpec extends BaseSpec with OptionValues {
  implicit val paging: Paging = Paging.Default
  implicit val sorting: Sorting = topSortings.head
  implicit val trace: Traced = Traced.empty

  implicit val request: RequestImpl = {
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

  private val appsFeaturesManager = mock[AppsFeaturesManager]
  private val searchRequestMapper = new SearcherRequestMapper(appsFeaturesManager, featureManager)

  "add top count to filter" in {
    val original = ApiSearchRequest(
      Cars,
      SearchRequestParameters
        .newBuilder()
        .build()
    )

    when(appsFeaturesManager.isSearchNoPremiumAdsInNewListingSupported(?)).thenReturnF(false)
    when(appsFeaturesManager.isCarsUsedGroupingSupported(?)).thenReturnF(false)

    val withoutVas = searchMappings.fromApiToSearcher(original, None)
    val result = searchRequestMapper.addVasControl(withoutVas, Listing, GroupBy.NoGrouping).futureValue

    result.params(AutoruTopCountParam) shouldEqual Set(DefaultTopCount.toString)
    result.params shouldNot contain(TopExpectedCountParam)
  }

  "don't add top_count on context=group_card" in {
    val original = ApiSearchRequest(
      Cars,
      SearchRequestParameters
        .newBuilder()
        .build()
    )

    when(appsFeaturesManager.isSearchNoPremiumAdsInNewListingSupported(?)).thenReturnF(false)
    when(appsFeaturesManager.isCarsUsedGroupingSupported(?)).thenReturnF(false)

    val withoutVas = searchMappings.fromApiToSearcher(original, None)
    val result =
      searchRequestMapper
        .addVasControl(withoutVas, GroupCard, GroupBy.NoGrouping)(sorting, Paging(1, 10), request)
        .futureValue

    result.params(AutoruTopCountParam) shouldEqual Set(GroupCardTopCount.toString)
    result.params shouldNot contain(TopExpectedCountParam)
  }

  "add default top_count on context=listing for MOTO category" in {
    val original = ApiSearchRequest(
      Moto,
      SearchRequestParameters
        .newBuilder()
        .build()
    )

    val withoutVas = searchMappings.fromApiToSearcher(original, None)
    val result =
      searchRequestMapper
        .addVasControl(withoutVas, Listing, GroupBy.NoGrouping)(sorting, Paging(1, 10), request)
        .futureValue

    result.params(AutoruTopCountParam) shouldEqual Set(DefaultTopCount.toString)
    result.params shouldNot contain(TopExpectedCountParam)
  }

  "don't add top_count on context=subscription for NEW CARS (AUTORUAPI-5625)" in {
    val original = ApiSearchRequest(
      Cars,
      SearchRequestParameters
        .newBuilder()
        .setStateGroup(StateGroup.NEW)
        .build()
    )

    val withoutVas = searchMappings.fromApiToSearcher(original, None)
    val result =
      searchRequestMapper
        .addVasControl(withoutVas, Subscription, GroupBy.NoGrouping)(sorting, Paging(1, 10), request)
        .futureValue

    result.params(AutoruTopCountParam) shouldEqual Set(SeparatedTopCount.toString)
    result.params shouldNot contain(TopExpectedCountParam)
  }

  "do add top_count on context=subscription for NEW TRUCKS OR MOTO (AUTORUAPI-5625)" in {
    val original = ApiSearchRequest(
      Gen.oneOf(Moto, Trucks).next,
      SearchRequestParameters
        .newBuilder()
        .setStateGroup(StateGroup.NEW)
        .build()
    )

    val withoutVas = searchMappings.fromApiToSearcher(original, None)
    val result =
      searchRequestMapper
        .addVasControl(withoutVas, Subscription, GroupBy.NoGrouping)(sorting, Paging(1, 10), request)
        .futureValue

    result.params(AutoruTopCountParam) shouldEqual Set(SubscriptionsTopCount.toString)
    result.params shouldNot contain(TopExpectedCountParam)
  }

  "do add top_count on context=subscription" in {
    val original = ApiSearchRequest(
      Cars,
      SearchRequestParameters
        .newBuilder()
        .build()
    )

    val withoutVas = searchMappings.fromApiToSearcher(original, None)
    val result =
      searchRequestMapper
        .addVasControl(withoutVas, Subscription, GroupBy.NoGrouping)(sorting, Paging(1, 10), request)
        .futureValue

    result.params(AutoruTopCountParam) shouldEqual Set(SubscriptionsTopCount.toString)
    result.params shouldNot contain(TopExpectedCountParam)
  }

  "don't add top_count on new listing for web" in {
    val original = ApiSearchRequest(
      Cars,
      SearchRequestParameters
        .newBuilder()
        .setStateGroup(StateGroup.NEW)
        .build()
    )

    when(appsFeaturesManager.isSearchNoPremiumAdsInNewListingSupported(?)).thenReturnF(true)
    when(appsFeaturesManager.isCarsUsedGroupingSupported(?)).thenReturnF(false)

    val withoutVas = searchMappings.fromApiToSearcher(original, None)
    val result =
      searchRequestMapper
        .addVasControl(withoutVas, Listing, GroupBy.NoGrouping)(sorting, Paging(1, 10), request)
        .futureValue

    result.params(AutoruTopCountParam) shouldEqual Set(SeparatedTopCount.toString)
    result.params shouldNot contain(TopExpectedCountParam)
  }

  "don't add top_count on new listing for iOS app with feature" in {
    val original = ApiSearchRequest(
      Cars,
      SearchRequestParameters
        .newBuilder()
        .setStateGroup(StateGroup.NEW)
        .build()
    )

    val requestIos = {
      val req = new RequestImpl
      req.setApplication(Application.iosApp)
      req.setTrace(trace)
      req.setRequestParams(RequestParams.empty)
      req
    }

    when(appsFeaturesManager.isSearchNoPremiumAdsInNewListingSupported(?)).thenReturnF(true)
    when(appsFeaturesManager.isCarsUsedGroupingSupported(?)).thenReturnF(false)

    val withoutVas = searchMappings.fromApiToSearcher(original, None)
    val result =
      searchRequestMapper
        .addVasControl(withoutVas, Listing, GroupBy.NoGrouping)(sorting, Paging(1, 10), requestIos)
        .futureValue

    result.params(AutoruTopCountParam) shouldEqual Set(SeparatedTopCount.toString)
    result.params shouldNot contain(TopExpectedCountParam)
  }

  "don't add top_count on new listing for Android app with feature" in {
    val original = ApiSearchRequest(
      Cars,
      SearchRequestParameters
        .newBuilder()
        .setStateGroup(StateGroup.NEW)
        .build()
    )

    val requestIos = {
      val req = new RequestImpl
      req.setApplication(Application.androidApp)
      req.setTrace(trace)
      req.setRequestParams(RequestParams.empty)
      req
    }

    when(appsFeaturesManager.isSearchNoPremiumAdsInNewListingSupported(?)).thenReturnF(true)
    when(appsFeaturesManager.isCarsUsedGroupingSupported(?)).thenReturnF(false)

    val withoutVas = searchMappings.fromApiToSearcher(original, None)
    val result =
      searchRequestMapper
        .addVasControl(withoutVas, Listing, GroupBy.NoGrouping)(sorting, Paging(1, 10), requestIos)
        .futureValue

    result.params("autoru_top_count") shouldEqual Set(SeparatedTopCount.toString)
    result.params shouldNot contain(TopExpectedCountParam)
  }

  "add top_count on USED listing for Android app with feature" in {
    val original = ApiSearchRequest(
      Cars,
      SearchRequestParameters
        .newBuilder()
        .setStateGroup(StateGroup.USED)
        .build()
    )

    val requestIos = {
      val req = new RequestImpl
      req.setApplication(Application.androidApp)
      req.setTrace(trace)
      req.setRequestParams(RequestParams.empty)
      req
    }

    when(appsFeaturesManager.isSearchNoPremiumAdsInNewListingSupported(?)).thenReturnF(true)
    when(appsFeaturesManager.isCarsUsedGroupingSupported(?)).thenReturnF(false)

    val withoutVas = searchMappings.fromApiToSearcher(original, None)
    val result =
      searchRequestMapper
        .addVasControl(withoutVas, Listing, GroupBy.NoGrouping)(sorting, Paging(1, 10), requestIos)
        .futureValue

    result.params("autoru_top_count") shouldEqual Set(DefaultTopCount.toString)
    result.params shouldNot contain(TopExpectedCountParam)
  }

  "CARS listing for Android app with CARS_USED_GROUPING feature" in {
    val original = ApiSearchRequest(
      Cars,
      SearchRequestParameters
        .newBuilder()
        .build()
    )

    val requestIos = {
      val req = new RequestImpl
      req.setApplication(Application.androidApp)
      req.setTrace(trace)
      req.setRequestParams(RequestParams.empty)
      req
    }

    when(appsFeaturesManager.isCarsUsedGroupingSupported(?)).thenReturnF(true)
    when(appsFeaturesManager.isSearchNoPremiumAdsInNewListingSupported(?)).thenReturnF(false)

    val withoutVas = searchMappings.fromApiToSearcher(original, None)
    val resultWithGrouping =
      searchRequestMapper
        .addVasControl(withoutVas, Listing, GroupBy.Configuration :: Nil)(sorting, Paging(1, 10), requestIos)
        .futureValue

    resultWithGrouping.params("autoru_top_count") shouldEqual Set(ZeroTopCount.toString)

    val resultWithoutGrouping =
      searchRequestMapper
        .addVasControl(withoutVas, Listing, GroupBy.NoGrouping)(sorting, Paging(1, 10), requestIos)
        .futureValue

    resultWithoutGrouping.params("autoru_top_count") shouldEqual Set(DefaultTopCount.toString)
  }

  "don't add top_count on second page" in {
    val original = ApiSearchRequest(
      Cars,
      SearchRequestParameters
        .newBuilder()
        .build()
    )

    val withoutVas = searchMappings.fromApiToSearcher(original, None)
    val result =
      searchRequestMapper
        .addVasControl(withoutVas, Listing, GroupBy.NoGrouping)(sorting, Paging(2, 10), request)
        .futureValue
        .params

    result shouldNot contain(AutoruTopCountParam)
    result(TopExpectedCountParam) shouldEqual Set(DefaultTopCount.toString)
  }

  "don't add top_count on specific sorting" in {
    val original = ApiSearchRequest(
      Cars,
      SearchRequestParameters
        .newBuilder()
        .build()
    )
    val sort = SortingByField("a", desc = false)

    val withoutVas = searchMappings.fromApiToSearcher(original, None)
    val result =
      searchRequestMapper
        .addVasControl(withoutVas, Listing, GroupBy.NoGrouping)(sort, paging, request)
        .futureValue
        .params

    result shouldNot contain(AutoruTopCountParam)
    result shouldNot contain(TopExpectedCountParam)
  }

  "append all of the valid techparams from header" in {
    val r = {
      val req = new RequestImpl
      req.setApplication(Application.desktop)
      req.setTrace(trace)
      req.setRequestParams(RequestParams.empty.copy(xLastViewedTechParamIds = Set(1L, 2L, 3L)))
      req
    }

    val filter =
      searchMappings.fromApiToSearcher(ApiSearchRequest(Cars, SearchRequestParameters.newBuilder().build()), None)

    val result = searchRequestMapper.addSeenTechParams(filter)(r).params

    result(SeenTechParamParamName).flatMap(_.split(",")) shouldEqual Set("1", "2", "3")
  }

  "dropMatchApplicationsTag" when {
    "has no match_application tag" should {
      "do nothing" in {
        val params = Map("search_tag" -> Set("other_tag"))

        when(featureManager.matchApplicationNoSearch).thenReturn(new Feature[Boolean] {
          override def name: String = "match_application_no_search"
          override def value: Boolean = true
        })

        val result = searchRequestMapper.dropMatchApplicationsTag(SearcherRequest(Cars, params))

        assert(result.params.get("search_tag").value.contains("other_tag"))
        assert(!result.params.contains("state"))
        assert(!result.params.contains("state_group"))
      }
    }
  }

  for (on <- List(true, false)) {
    val not = if (on) "" else "not "
    s"should ${not}drop search tag 'match_applications' when feature ${not}enabled" in {
      val params = SearchRequestParameters
        .newBuilder()
        .addSearchTag("match_applications")
        .addSearchTag("other_tag")
        .build()

      when(featureManager.matchApplicationNoSearch).thenReturn(new Feature[Boolean] {
        override def name: String = "match_application_no_search"

        override def value: Boolean = on
      })

      val apiRequest = searchMappings.fromApiToSearcher(ApiSearchRequest(Cars, params), None)
      val result = searchRequestMapper.dropMatchApplicationsTag(apiRequest)

      assert(result.params.get("search_tag").value.contains("other_tag"))

      assert {
        result.params.exists {
          case (SearcherFieldNames.SEARCH_TAG, tags) => tags("match_applications")
          case (SearcherFieldNames.STATE_GROUP, tags) => !tags("NEW")
          case (SearcherFieldNames.STATE, tags) => !tags("NEW")
          case _ => false
        } != on
      }
    }
  }
}
