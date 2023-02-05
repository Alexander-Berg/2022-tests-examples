package ru.yandex.yandexmaps.search.statereducing

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.yandexmaps.search.internal.redux.GoBack
import ru.yandex.yandexmaps.search.internal.redux.SearchState
import ru.yandex.yandexmaps.search.internal.redux.reduce
import ru.yandex.yandexmaps.search.statereducing.utils.closeEverything
import ru.yandex.yandexmaps.search.statereducing.utils.fillOpenedCardFromSuggest
import ru.yandex.yandexmaps.search.statereducing.utils.fillResults
import ru.yandex.yandexmaps.search.statereducing.utils.fillSuggest
import ru.yandex.yandexmaps.search.statereducing.utils.initialSearchState

@RunWith(Parameterized::class)
class GoBackActionTest(private val withPolyline: Boolean) {

    private val initialState: SearchState
        get() = initialSearchState(withPolyline)

    @Test
    fun `GoBack Results shown`() {
        val beginState = initialState
            .fillResults()
        val expectedState = beginState.copy(
            mainScreensStack = emptyList(),
            isSearchSessionCombined = false
        )

        val reducedState = reduce(beginState, GoBack)

        Assert.assertEquals(expectedState, reducedState)
    }

    @Test
    fun `GoBack filters screen shown`() {
        val beginState = initialState
            .fillResults(withFiltersScreen = !withPolyline)
        val expectedState = beginState.copy(
            mainScreensStack = beginState.mainScreensStack.dropLast(1)
        )

        val reducedState = reduce(beginState, GoBack)

        Assert.assertEquals(expectedState, reducedState)
    }

    @Test
    fun `GoBack Result card screen shown`() {
        val beginState = initialState
            .fillResults(withResultCard = true)
        val expectedState = beginState
            .fillResults(withResultCard = false)

        val reducedState = reduce(beginState, GoBack)

        Assert.assertEquals(expectedState, reducedState)
    }

    @Test
    fun `GoBack when MT card from suggest shown`() {
        val expectedState = initialState

        val beginState = expectedState
            .fillOpenedCardFromSuggest()

        val reducedState = reduce(beginState, GoBack)
        Assert.assertEquals(expectedState, reducedState)
    }

    @Test
    fun `GoBack Suggest shown`() {
        val beginState = initialState
            .fillSuggest()
        val expectedState = beginState
            .closeEverything(isSearchSessionCombined = false)

        val reducedState = reduce(beginState, GoBack)

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
