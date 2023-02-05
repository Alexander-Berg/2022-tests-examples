package ru.yandex.yandexmaps.launch.events

import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.core.geometry.createPoint
import ru.yandex.yandexmaps.multiplatform.core.geometry.isIdentical
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.ShowPointOnMapEvent

class ShowPointOnMapEventParserTest : UriParserBaseTest() {

    private val point = createPoint(37.617671, 55.755768)

    @Test
    fun whatsHerePointSet_parsedCorrect() {
        val uri = parseUri("yandexmaps://?whatshere[point]=${point.lon},${point.lat}")
        val event = ShowPointOnMapEvent.Parser.parse(uri)

        assert(event is ShowPointOnMapEvent)

        val whatsHereEvent = event as ShowPointOnMapEvent
        assert(whatsHereEvent.point.isIdentical(point))
    }

    @Test
    fun pointSet_parsedCorrect() {
        val uri = parseUri("yandexmaps://?pt=${point.lon},${point.lat}")
        val event = ShowPointOnMapEvent.Parser.parse(uri)

        assert(event is ShowPointOnMapEvent)

        val whatsHereEvent = event as ShowPointOnMapEvent
        assert(whatsHereEvent.point.isIdentical(point))
    }
}
