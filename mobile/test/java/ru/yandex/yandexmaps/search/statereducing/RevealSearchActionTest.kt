package ru.yandex.yandexmaps.search.statereducing

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.yandexmaps.search.internal.engine.RevealSearch
import ru.yandex.yandexmaps.search.internal.redux.SearchState
import ru.yandex.yandexmaps.search.internal.redux.reduce
import ru.yandex.yandexmaps.search.statereducing.utils.closeEverything
import ru.yandex.yandexmaps.search.statereducing.utils.fillResults
import ru.yandex.yandexmaps.search.statereducing.utils.fillSuggest
import ru.yandex.yandexmaps.search.statereducing.utils.initialSearchState
import ru.yandex.yandexmaps.search.statereducing.utils.withoutChanges

@RunWith(Parameterized::class)
class RevealSearchActionTest(private val withPolyline: Boolean) {

    private val initialState: SearchState
        get() = initialSearchState(withPolyline)

    @Test
    fun `HandleSlaveClosed when card opened`() {
        val beginState = initialState
            .fillSuggest()
            .fillResults(withResultCard = true)
        val expectedState = beginState
            .withoutChanges()

        val reducedState = reduce(beginState, RevealSearch)

        assertEquals(expectedState, reducedState)
    }

    @Test
    fun `HandleSlaveClosed when isHideSerp=false`() {
        val beginState = initialState
            .fillSuggest()
            .fillResults(isHideSerp = false)
        val expectedState = beginState
            .withoutChanges()

        val reducedState = reduce(beginState, RevealSearch)

        assertEquals(expectedState, reducedState)
    }

    @Test
    fun `HandleSlaveClosed when isHideSerp=true and no openCard`() {
        val beginState = initialState
            .fillSuggest()
            .fillResults(isHideSerp = true, withResultCard = false)
        val expectedState = beginState
            .closeEverything()

        val reducedState = reduce(beginState, RevealSearch)

        assertEquals(expectedState, reducedState)
    }

    @Test
    fun `HandleSlaveClosed when isHideSerp=true and with openCard`() {
        val beginState = initialState
            .fillSuggest()
            .fillResults(isHideSerp = true, withResultCard = true)
        val expectedState = beginState
            .withoutChanges()

        val reducedState = reduce(beginState, RevealSearch)

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
