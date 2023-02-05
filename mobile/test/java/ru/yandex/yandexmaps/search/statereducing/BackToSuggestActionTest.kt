package ru.yandex.yandexmaps.search.statereducing

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.yandexmaps.search.internal.redux.BackToSuggest
import ru.yandex.yandexmaps.search.internal.redux.SearchState
import ru.yandex.yandexmaps.search.internal.redux.SuggestAndCategories
import ru.yandex.yandexmaps.search.internal.redux.SuggestInput
import ru.yandex.yandexmaps.search.internal.redux.reduce
import ru.yandex.yandexmaps.search.statereducing.utils.fillOpenedCardFromSuggest
import ru.yandex.yandexmaps.search.statereducing.utils.fillResults
import ru.yandex.yandexmaps.search.statereducing.utils.fillSuggest
import ru.yandex.yandexmaps.search.statereducing.utils.initialSearchState
import ru.yandex.yandexmaps.search.statereducing.utils.makeSuggestEmpty

@RunWith(Parameterized::class)
class BackToSuggestActionTest(private val withPolyline: Boolean) {

    private val initialState: SearchState
        get() = initialSearchState(withPolyline)

    @Test
    fun `BackToSuggest on offline explanation screen`() {
        val beginState = initialState
            .fillSuggest()
            .fillResults(displayText = "displayText")
        val expectedState = beginState.copy(
            mainScreensStack = listOf(SuggestAndCategories),
            suggest = beginState.suggest?.copy(input = SuggestInput(showKeyboard = true, text = beginState.results!!.query.displayText)),
            results = null,
            isSearchSessionCombined = true
        )

        val reducedState = reduce(beginState, BackToSuggest)

        Assert.assertEquals(expectedState, reducedState)
    }

    @Test
    fun `BackToSuggest when MT card is opened from suggest`() {
        val beginState = initialState
            .fillOpenedCardFromSuggest()

        val expectedState = beginState
            .makeSuggestEmpty()

        val reducedState = reduce(beginState, BackToSuggest)

        Assert.assertEquals(expectedState, reducedState)
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
