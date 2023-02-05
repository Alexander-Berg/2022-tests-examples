package ru.yandex.yandexmaps.search.statereducing

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.yandexmaps.search.internal.engine.CloseSearchResultCard
import ru.yandex.yandexmaps.search.internal.redux.reduce
import ru.yandex.yandexmaps.search.statereducing.utils.fillResults
import ru.yandex.yandexmaps.search.statereducing.utils.initialSearchState

@RunWith(Parameterized::class)
internal class CloseSearchResultCardActionTest(
    private val withPolyline: Boolean,
) {

    @Test
    fun `CloseSearchResultCard without serp in Backstack`() {
        val beginState = initialSearchState(withPolyline)
            .fillResults(withResultCard = true, isHideSerp = true)

        val expectedState = beginState.copy(
            mainScreensStack = emptyList()
        )
        val reducedState = reduce(beginState, CloseSearchResultCard)

        assertEquals(expectedState, reducedState)
    }

    @Test
    fun `CloseSearchResultCard with serp in Backstack`() {
        val beginState = initialSearchState(withPolyline)
            .fillResults(withResultCard = true, isHideSerp = false)

        val expectedState = beginState.copy(
            mainScreensStack = beginState.mainScreensStack.dropLast(1)
        )
        val reducedState = reduce(beginState, CloseSearchResultCard)

        assertEquals(expectedState, reducedState)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun parameters() = listOf(
            arrayOf(false),
            arrayOf(true),
        )
    }
}
