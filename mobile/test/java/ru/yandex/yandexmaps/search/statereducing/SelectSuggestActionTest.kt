package ru.yandex.yandexmaps.search.statereducing

import com.yandex.mapkit.search.SuggestItem
import com.yandex.mapkit.search.SuggestItem.Action.SEARCH
import com.yandex.mapkit.search.SuggestItem.Action.SUBSTITUTE
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.yandexmaps.search.internal.redux.SearchState
import ru.yandex.yandexmaps.search.internal.redux.defaultOrigin
import ru.yandex.yandexmaps.search.internal.redux.reduce
import ru.yandex.yandexmaps.search.statereducing.utils.fillOpenedCardFromSuggest
import ru.yandex.yandexmaps.search.statereducing.utils.fillResults
import ru.yandex.yandexmaps.search.statereducing.utils.fillSuggest
import ru.yandex.yandexmaps.search.statereducing.utils.initialSearchState
import ru.yandex.yandexmaps.search.statereducing.utils.stub
import ru.yandex.yandexmaps.search.statereducing.utils.syncSessionId
import ru.yandex.yandexmaps.search.statereducing.utils.toQuery
import ru.yandex.yandexmaps.search.statereducing.utils.withoutChanges
import ru.yandex.yandexmaps.suggest.redux.SelectSuggest
import ru.yandex.yandexmaps.suggest.redux.SuggestElement
import ru.yandex.yandexmaps.suggest.redux.reduce

@RunWith(Parameterized::class)
class SelectSuggestActionTest(
    private val withPolyline: Boolean,
) {

    private val initialState: SearchState
        get() = initialSearchState(withPolyline)

    @Test
    fun `SelectSuggest with search`() {
        val beginState = initialState
        val suggestElement = suggestElement(actionSearch = true)
        val expectedState = beginState
            .fillResults(query = suggestElement.toQuery(beginState.defaultOrigin))

        val reducedState = reduce(beginState, SelectSuggest(suggestElement))
            .syncSessionId(expectedState)

        assertEquals(expectedState, reducedState)
    }

    @Test
    fun `SelectSuggest with substitute when suggest open`() {
        val beginState = initialState
            .fillSuggest()
        val suggestElement = suggestElement(actionSearch = false)
        val selectSuggest = SelectSuggest(suggestElement)
        val expectedState = beginState
            .copy(suggest = beginState.suggest!!.let { suggest -> suggest.copy(state = suggest.state.reduce(selectSuggest)) })

        val reducedState = reduce(beginState, selectSuggest)

        assertEquals(expectedState, reducedState)
    }

    @Test
    fun `SelectSuggest with substitute when results open`() {
        val beginState = initialState
            .fillResults()
        val suggestElement = suggestElement(actionSearch = false)
        val selectSuggest = SelectSuggest(suggestElement)
        val expectedState = beginState
            .withoutChanges()

        val reducedState = reduce(beginState, selectSuggest)

        assertEquals(expectedState, reducedState)
    }

    @Test
    fun `SelectSuggest opens MT card directly`() {
        val beginState = initialState
            .fillSuggest(emptySuggest = false)

        val suggestElement = suggestElementToOpenMtCardDirectly()

        val expectedState = beginState
            .fillOpenedCardFromSuggest(suggestElement.uri!!, suggestElement.logId)

        val reducedState = reduce(beginState, SelectSuggest(suggestElement))

        assertEquals(expectedState, reducedState)
    }

    private fun suggestElement(actionSearch: Boolean) = SuggestElement(
        title = stub(),
        subtitle = stub(),
        searchText = "searchText",
        tags = emptyList(),
        personal = false,
        action = if (actionSearch) SEARCH else SUBSTITUTE,
        uri = null,
        distance = null,
        type = stub(),
        position = 0,
        responseTime = 0,
        logId = "logId",
        offline = false,
        isWordSuggest = false,
        displayText = "displayText",
        avatarUrlTemplate = "avatarUrlTemplate"
    )

    private fun suggestElementToOpenMtCardDirectly(): SuggestElement {
        return suggestElement(true)
            .copy(
                type = SuggestItem.Type.TRANSIT,
                uri = "transport://someId"
            )
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
