package ru.yandex.yandexmaps.launch.parsers

import org.junit.Assert.assertTrue
import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.core.geometry.Point
import ru.yandex.yandexmaps.multiplatform.core.geometry.isIdentical
import ru.yandex.yandexmaps.multiplatform.core.utils.extensions.closeTo
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.WrongPatternEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenUserLocationEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.SearchEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.ShowPointOnMapEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.parsers.events.GeoUriParser

class GeoUriParserTest : UriParserBaseTest() {

    @Test
    fun nonGeoScheme_parsedFailed() {
        val uri = parseUri("any_other_scheme:47.6,-122.3?z=15")
        val event = geoUriParser().parse(uri)
        assertTrue(event is WrongPatternEvent)
    }

    @Test
    fun geoWithCoordinates_parsedCorrect() {
        val uri = parseUri("geo:47.6,-122.3?z=15")
        val event = geoUriParser().parse(uri)
        assertTrue(event is ShowPointOnMapEvent)

        val openMapEvent = event as ShowPointOnMapEvent
        assertTrue(openMapEvent.point.isIdentical(Point(47.6, -122.3)))
        assertTrue(openMapEvent.zoom!!.closeTo(15f))
    }

    @Test
    fun geoWithZeroCoordinates_parsedCorrect() {
        val uri = parseUri("geo:0,0")
        val event = geoUriParser().parse(uri)
        assertTrue(event is OpenUserLocationEvent)
    }

    @Test
    fun geoWithCoordinatesAndLabel_parsedCorrect() {
        val uri = parseUri("geo:0,0?q=34.99,-106.61(Treasure)")
        val event = geoUriParser().parse(uri)
        assertTrue(event is ShowPointOnMapEvent) // yeah, we ignore label

        val searchEvent = event as ShowPointOnMapEvent
        assertTrue(searchEvent.point.isIdentical(Point(34.99, -106.61)))
    }

    @Test
    fun geoWithQuery_parsedCorrect() {
        val uri = parseUri("geo:0,0?q=1600+Amphitheatre+Parkway%2C+CA")
        val event = geoUriParser().parse(uri)
        assertTrue(event is SearchEvent)

        val searchEvent = event as SearchEvent
        assertTrue(searchEvent.searchText == "1600 Amphitheatre Parkway, CA")
    }

    @Test
    fun testFromSergeyKrayushkin_parsedCorrect() {
        val uri = parseUri("geo:0,0?q=H%26M")
        val event = geoUriParser().parse(uri)
        assertTrue(event is SearchEvent)

        val searchEvent = event as SearchEvent
        assertTrue(searchEvent.searchText == "H&M")
    }

    @Test
    fun geoCoordinatesAndQuery_parsedCorrect() {
        val uri = parseUri("geo:37.7749,-122.4194?q=101+main+street")
        val event = geoUriParser().parse(uri)
        assertTrue(event is SearchEvent)

        val searchEvent = event as SearchEvent
        assertTrue(searchEvent.searchPoint!!.isIdentical(Point(37.7749, -122.4194)))
        assertTrue(searchEvent.searchText == "101 main street")
    }

    private fun geoUriParser() = GeoUriParser(0f)
}
