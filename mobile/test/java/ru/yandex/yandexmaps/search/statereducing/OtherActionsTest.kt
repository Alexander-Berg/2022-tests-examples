package ru.yandex.yandexmaps.search.statereducing

import com.nhaarman.mockito_kotlin.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.yandexmaps.search.api.controller.SearchQuery
import ru.yandex.yandexmaps.search.api.dependencies.SearchBannersConfig
import ru.yandex.yandexmaps.search.internal.redux.PerformSearchByCurrentInput
import ru.yandex.yandexmaps.search.internal.redux.PerformSearchByVoiceInput
import ru.yandex.yandexmaps.search.internal.redux.PerformSearchWithoutMisspellCorrection
import ru.yandex.yandexmaps.search.internal.redux.RerunSearch
import ru.yandex.yandexmaps.search.internal.redux.SearchState
import ru.yandex.yandexmaps.search.internal.redux.defaultOrigin
import ru.yandex.yandexmaps.search.internal.redux.defaultVoiceOrigin
import ru.yandex.yandexmaps.search.internal.redux.reduce
import ru.yandex.yandexmaps.search.internal.suggest.LoadSearchBannersConfig
import ru.yandex.yandexmaps.search.internal.suggest.ResetSearchState
import ru.yandex.yandexmaps.search.internal.suggest.categoryandhistory.PerformSearchByCategory
import ru.yandex.yandexmaps.search.internal.suggest.categoryandhistory.PerformSearchByHistory
import ru.yandex.yandexmaps.search.statereducing.utils.fillResults
import ru.yandex.yandexmaps.search.statereducing.utils.fillSuggest
import ru.yandex.yandexmaps.search.statereducing.utils.initialSearchState
import ru.yandex.yandexmaps.search.statereducing.utils.stub
import ru.yandex.yandexmaps.search.statereducing.utils.syncSessionId
import ru.yandex.yandexmaps.search.statereducing.utils.withoutChanges

@RunWith(Parameterized::class)
class OtherActionsTest(
    private val withPolyline: Boolean,
) {

    private val initialState: SearchState
        get() = initialSearchState(withPolyline)

    @Test
    fun `ResetSearchState happened`() {
        val beginState = initialState
        val resetSearchState = ResetSearchState(newState = initialSearchState())
        val expectedState = resetSearchState.newState

        val reducedState = reduce(beginState, resetSearchState)
        assertEquals(expectedState, reducedState)
    }

    @Test
    fun `PerformSearchByCategory happened`() {
        val beginState = initialState
        val resetSearchState = PerformSearchByCategory(id = "id", title = "title", query = stub(), source = null, fromHistory = false)
        val expectedState = beginState
            .fillResults(query = resetSearchState.query)

        val reducedState = reduce(beginState, resetSearchState)
            .syncSessionId(expectedState)

        assertEquals(expectedState, reducedState)
    }

    @Test
    fun `PerformSearchByHistory happened`() {
        val beginState = initialState
        val performSearchByHistory = PerformSearchByHistory(query = stub())
        val expectedState = beginState
            .fillResults(query = performSearchByHistory.query)

        val reducedState = reduce(beginState, performSearchByHistory)
            .syncSessionId(expectedState)

        assertEquals(expectedState, reducedState)
    }

    @Test
    fun `RerunSearch happened`() {
        val beginState = initialState
        val rerunSearch = RerunSearch(query = stub())
        val expectedState = beginState
            .fillResults(query = rerunSearch.query)

        val reducedState = reduce(beginState, rerunSearch)
            .syncSessionId(expectedState)

        assertEquals(expectedState, reducedState)
    }

    @Test
    fun `PerformSearchByCurrentInput happened`() {
        val beginState = initialState
            .fillSuggest()

        val reducedState = reduce(beginState, PerformSearchByCurrentInput)
        assertNotNull(reducedState.results)
    }

    @Test
    fun `PerformSearchWithoutMisspellCorrection happened`() {
        val requestText = "requestText"
        val displayText = "displayText"
        val beginState = initialState
            .fillResults(requestText = requestText, displayText = displayText)
        val expectedState = beginState
            .fillResults(query = SearchQuery.fromCancelMisspellCorrection(requestText, beginState.defaultOrigin))

        val reducedState = reduce(beginState, PerformSearchWithoutMisspellCorrection).syncSessionId(expectedState)

        assertEquals(expectedState, reducedState)
    }

    @Test
    fun `PerformSearchByVoiceInput happened`() {
        val requestText = "requestText"
        val beginState = initialState
        val expectedState = beginState
            .fillResults(query = SearchQuery.fromText(requestText, beginState.defaultVoiceOrigin, SearchQuery.Source.VOICE))

        val reducedState = reduce(beginState, PerformSearchByVoiceInput(requestText)).syncSessionId(expectedState)
        assertEquals(expectedState, reducedState)
    }

    @Test
    fun `LoadSearchBannersConfig happened when bannerConfig not null`() {
        val beginState = initialState
        val bannerConfig = mock<SearchBannersConfig>()
        val expectedState = beginState.copy(
            searchBannersConfig = bannerConfig
        )

        val reducedState = reduce(beginState, LoadSearchBannersConfig(bannerConfig))

        assertEquals(expectedState, reducedState)
    }

    @Test
    fun `LoadSearchBannersConfig happened when bannerConfig null`() {
        val beginState = initialState
        val bannerConfig = null
        val expectedState = beginState
            .withoutChanges()

        val reducedState = reduce(beginState, LoadSearchBannersConfig(bannerConfig))

        assertEquals(expectedState, reducedState)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: withPolyline = {0}")
        fun parameters() = listOf(
            arrayOf(true),
            arrayOf(false)
        )
    }
}
