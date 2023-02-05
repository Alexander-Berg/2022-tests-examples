package ru.yandex.yandexmaps.launch.events

import android.app.Application
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.core.geometry.createPoint
import ru.yandex.yandexmaps.multiplatform.core.geometry.isIdentical
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.SearchEvent

class SearchEventParserTest : UriParserBaseTest() {

    @Mock
    lateinit var app: Application

    @Test
    fun onlyCorrectPath_parsedCorrect() {
        val uri = parseUri("yandexmaps://yandex.ru/maps/search")
        val event = parser().parse(uri)

        assert(event is SearchEvent)
    }

    @Test
    fun correctPathAndCorrectArgs_parsedCorrect() {
        val searchText = "cafe"
        val displayText = "food"

        val point = createPoint(37.617671, 55.755768)
        val span = createPoint(1.730347, 0.738038)

        val uri = parseUri("yandexmaps://maps.yandex.ru/?ll=${point.lon},${point.lat}&sspn=${span.lon},${span.lat}&search_text=$searchText&display-text=$displayText")
        val event = parser().parse(uri)

        assert(event is SearchEvent)

        val searchEvent = event as SearchEvent
        assert(searchEvent.searchText!! == searchText)
        assert(searchEvent.displayText!! == displayText)
        assert(searchEvent.searchPoint!!.isIdentical(point))
        assert(searchEvent.spanPoint!!.isIdentical(span))
    }

    @Test
    fun correctPathAndOnlyTextArg_parsedCorrect() {
        val searchText = "cafe"

        val uri = parseUri("yandexmaps://maps.yandex.ru/?text=$searchText")
        val event = parser().parse(uri)

        assert(event is SearchEvent)

        val searchEvent = event as SearchEvent
        assert(searchEvent.searchText!! == searchText)
    }

    private fun parser() = SearchEvent.Parser
}
