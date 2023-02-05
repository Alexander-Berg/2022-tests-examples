package ru.yandex.yandexmaps.search.statereducing.utils

import com.nhaarman.mockito_kotlin.mock
import ru.yandex.yandexmaps.multiplatform.core.search.SearchOrigin
import ru.yandex.yandexmaps.multiplatform.core.utils.extensions.optionalCast
import ru.yandex.yandexmaps.multiplatform.core.utils.grabIf
import ru.yandex.yandexmaps.search.api.controller.CardFromSuggestData
import ru.yandex.yandexmaps.search.api.controller.SearchCategoriesContentMode
import ru.yandex.yandexmaps.search.api.controller.SearchQuery
import ru.yandex.yandexmaps.search.api.controller.SearchResultsScreenConfig
import ru.yandex.yandexmaps.search.api.dependencies.AddObjectInSuggestMode
import ru.yandex.yandexmaps.search.api.dependencies.Categories
import ru.yandex.yandexmaps.search.api.dependencies.SearchScreenType
import ru.yandex.yandexmaps.search.internal.engine.SearchEngineState
import ru.yandex.yandexmaps.search.internal.redux.AllFiltersScreen
import ru.yandex.yandexmaps.search.internal.redux.ResultCard
import ru.yandex.yandexmaps.search.internal.redux.SearchResultsState
import ru.yandex.yandexmaps.search.internal.redux.SearchResultsState.CommonSearchResultsState
import ru.yandex.yandexmaps.search.internal.redux.SearchResultsState.RouteSearchResultsState
import ru.yandex.yandexmaps.search.internal.redux.SearchScreen
import ru.yandex.yandexmaps.search.internal.redux.SearchState
import ru.yandex.yandexmaps.search.internal.redux.Serp
import ru.yandex.yandexmaps.search.internal.redux.Suggest
import ru.yandex.yandexmaps.search.internal.redux.SuggestAndCategories
import ru.yandex.yandexmaps.search.internal.redux.SuggestInput
import ru.yandex.yandexmaps.search.internal.results.filters.state.BooleanFilter
import ru.yandex.yandexmaps.search.internal.results.filters.state.CompositeFilter
import ru.yandex.yandexmaps.search.internal.results.filters.state.EnumFilter
import ru.yandex.yandexmaps.search.internal.results.filters.state.FiltersState
import ru.yandex.yandexmaps.search.internal.results.filters.state.ImageEnumFilter
import ru.yandex.yandexmaps.suggest.redux.SuggestElement
import ru.yandex.yandexmaps.suggest.redux.SuggestState

internal inline fun <reified T : Any> stub() = mock<T>(stubOnly = true)

internal fun initialSearchState(withPolyline: Boolean = false) =
    SearchState(
        mainScreensStack = emptyList(),
        suggest = null,
        results = null,
        polyline = grabIf(withPolyline) { stub() },
        searchOpenedFrom = stub(),
        isSearchSessionCombined = false,
        categoriesMode = stub(),
        searchBannersConfig = stub(),
        categoriesContentMode = SearchCategoriesContentMode.SHOWCASE,
        isSearchHidden = false,
        isInDriveMode = true,
        searchScreenTypeExperiment = SearchScreenType.OLD,
        addObjectInSuggest = AddObjectInSuggestMode.DISABLED,
        resultsScreenConfig = SearchResultsScreenConfig(),
    )

internal fun SearchState.fillSuggest(emptySuggest: Boolean = false): SearchState {
    fun input() = when (emptySuggest) {
        false -> SuggestInput(showKeyboard = true, text = "suggest text")
        true -> SuggestInput(showKeyboard = true, text = "")
    }

    return copy(
        mainScreensStack = listOf(SuggestAndCategories),
        suggest = Suggest(
            input = input(),
            mainCategories = Categories(stub()),
            historyCategories = stub(),
            showcaseData = stub(),
            categoriesColoredBackground = false,
            preserveCategoriesOrder = false,
            historyItems = stub(),
            categoriesContentMode = categoriesContentMode,
        )
    )
}

internal fun SearchState.makeSuggestEmpty(): SearchState {
    return copy(
        mainScreensStack = listOf(SuggestAndCategories),
        isSearchSessionCombined = true,
        suggest = suggest?.copy(state = SuggestState.Empty, input = suggest.input.copy(showKeyboard = false))
    )
}

internal fun SearchState.withoutChanges() = this

internal fun SearchState.openSuggest() = copy(
    mainScreensStack = listOf(SuggestAndCategories),
    isSearchSessionCombined = true
)

internal fun SearchState.closeEverything(isSearchSessionCombined: Boolean? = null) = copy(
    mainScreensStack = emptyList(),
    isSearchSessionCombined = isSearchSessionCombined ?: this.isSearchSessionCombined
)

internal fun SearchState.syncSessionId(expectedState: SearchState) = copy(results = results!!.setSearchSessionId(expectedState.results!!.searchSessionId))

private fun SearchResultsState.setSearchSessionId(value: String): SearchResultsState = when (this) {
    is CommonSearchResultsState -> copy(searchSessionId = value)
    is RouteSearchResultsState -> copy(searchSessionId = value)
}

internal fun SearchState.fillResults(
    withResultCard: Boolean = false,
    withFiltersScreen: Boolean = false,
    isHideSerp: Boolean = false,
    isLoading: Boolean = false,
    query: SearchQuery? = null,
    requestText: String? = null,
    displayText: String? = null,
): SearchState {
    val withPolyline: Boolean = polyline != null

    fun SearchResultsState?.createOrCopyResultsScreen(): SearchResultsState {

        fun displayText() = displayText ?: ""
        fun data() = requestText?.let { SearchQuery.Data.Text(it) } ?: stub()

        fun query() = query
            ?: this?.query?.copy(displayText = displayText())
            ?: SearchQuery(displayText = displayText(), data = data(), origin = stub(), source = stub())

        fun engineState() = if (requestText != null && !isLoading) {
            this?.engineState
                ?.optionalCast<SearchEngineState.Results>()
                ?.copy(requestText = requestText)
                ?: SearchEngineState.Results(
                    results = stub(),
                    hasNextPage = false,
                    offline = false,
                    reqId = "reqId",
                    receivingTime = 0,
                    boundingBox = stub(),
                    displayType = stub(),
                    responseType = stub(),
                    responseSource = stub(),
                    requestText = requestText,
                    correctedRequestText = "correctedRequestText",
                    hasReversePoint = false,
                    unusualHoursType = stub(),
                    experimentaBannerNames = stub(),
                    closedForWithoutQr = false,
                    openMiniCardFromSerpExperiment = false,
                )
        } else {
            SearchEngineState.Loading
        }

        fun CommonSearchResultsState?.createOrCopyCommonResultsScreen(): CommonSearchResultsState {
            return this?.copy(
                query = query(),
                engineState = engineState()
            ) ?: CommonSearchResultsState(
                query = query(),
                engineState = engineState()
            )
        }

        fun RouteSearchResultsState?.createOrCopyRouteResultsScreen(): RouteSearchResultsState {
            return this?.copy(
                query = query(),
                engineState = engineState()
            ) ?: RouteSearchResultsState(
                query = query(),
                engineState = engineState(),
            )
        }

        return when (withPolyline) {
            true -> optionalCast<RouteSearchResultsState>().createOrCopyRouteResultsScreen()
            false -> optionalCast<CommonSearchResultsState>().createOrCopyCommonResultsScreen()
        }
    }
    return copy(
        mainScreensStack = listOfNotNull(
            grabIf(!isHideSerp) { Serp },
            grabIf(withResultCard) { ResultCard(stub()) },
            grabIf(withFiltersScreen) {
                AllFiltersScreen(
                    FiltersState(
                        "",
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        null,
                        null
                    ),
                    emptySet()
                )
            }
        ),
        results = results.createOrCopyResultsScreen()
    )
}

internal fun SearchState.fillOpenedCardFromSuggest(uri: String = "uri", logId: String? = null): SearchState {
    return copy(
        mainScreensStack = listOf(ResultCard(CardFromSuggestData.MtThreadCard(uri, logId))),
    )
}

internal fun SuggestElement.toQuery(origin: SearchOrigin) = SearchQuery(
    displayText = displayText,
    data = when (val uri = uri) {
        null -> SearchQuery.Data.Text(searchText)
        else -> SearchQuery.Data.Uri(uri)
    },
    origin = origin,
    source = SearchQuery.Source.SUGGEST
)
