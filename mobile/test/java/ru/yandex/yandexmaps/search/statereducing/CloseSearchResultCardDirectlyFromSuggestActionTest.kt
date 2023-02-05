package ru.yandex.yandexmaps.search.statereducing

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.yandex.yandexmaps.search.internal.engine.CloseSearchResultCard
import ru.yandex.yandexmaps.search.internal.redux.reduce
import ru.yandex.yandexmaps.search.statereducing.utils.closeEverything
import ru.yandex.yandexmaps.search.statereducing.utils.fillOpenedCardFromSuggest
import ru.yandex.yandexmaps.search.statereducing.utils.initialSearchState

internal class CloseSearchResultCardDirectlyFromSuggestActionTest {

    @Test
    fun `CloseSearchResultCard when direct MT card from suggest is closed`() {
        val beginState = initialSearchState()
            .fillOpenedCardFromSuggest()

        val expectedState = beginState
            .closeEverything()

        val reducedState = reduce(beginState, CloseSearchResultCard)

        assertEquals(expectedState, reducedState)
    }
}
