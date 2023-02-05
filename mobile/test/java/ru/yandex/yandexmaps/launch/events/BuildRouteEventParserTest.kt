package ru.yandex.yandexmaps.launch.events

import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.core.geometry.createPoint
import ru.yandex.yandexmaps.multiplatform.core.geometry.isIdentical
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.WrongContentException
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.WrongPatternEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.BuildRouteEvent

class BuildRouteEventParserTest : UriParserBaseTest() {

    private val pointFrom = createPoint(59.967870, 30.242658)
    private val pointTo = createPoint(59.898495, 30.299559)

    @Test
    fun bothPoint_parsedCorrect() {
        val uri = parseUri("yandexmaps://build_route_on_map/?lat_from=${pointFrom.lat}&lon_from=${pointFrom.lon}&lat_to=${pointTo.lat}&lon_to=${pointTo.lon}")
        val event = BuildRouteEvent.Parser.parse(uri)
        assert(event is BuildRouteEvent)

        val routeEvent = event as BuildRouteEvent
        assert(routeEvent.pointTo!!.point.isIdentical(pointTo))
        assert(routeEvent.pointFrom!!.point.isIdentical(pointFrom))
    }

    @Test
    fun noPoints_parsedCorrect() {
        val uri = parseUri("yandexmaps://build_route_on_map")
        val event = BuildRouteEvent.Parser.parse(uri)
        assert(event is BuildRouteEvent)

        val routeEvent = event as BuildRouteEvent
        assert(routeEvent.pointFrom == null && routeEvent.pointTo == null && routeEvent.viaPoints.isEmpty())
    }

    @Test()
    fun noPoints_wrongContent() {
        val uri = parseUri("yandexmaps://maps.yandex.ru/build_route_on_map")
        val event = BuildRouteEvent.Parser.parse(uri)
        assert(event is WrongPatternEvent)
    }

    @Test
    fun startPoint_parsedCorrect() {
        val uri = parseUri("yandexmaps://build_route_on_map/?lat_from=${pointFrom.lat}&lon_from=${pointFrom.lon}")
        val event = BuildRouteEvent.Parser.parse(uri)
        assert(event is BuildRouteEvent)

        val routeEvent = event as BuildRouteEvent
        assert(routeEvent.pointFrom!!.point.isIdentical(pointFrom))
    }

    @Test
    fun endPoint_parsedCorrect() {
        val uri = parseUri("yandexmaps://build_route_on_map/?lat_to=${pointTo.lat}&lon_to=${pointTo.lon}")
        val event = BuildRouteEvent.Parser.parse(uri)
        assert(event is BuildRouteEvent)

        val routeEvent = event as BuildRouteEvent
        assert(routeEvent.pointTo!!.point.isIdentical(pointTo))
    }

    @Test
    fun noTargetPointsButManyVia_parsedCorrect() {
        val uri = parseUri("yandexmaps://yandex.ru/maps?rtext=${pointFrom.lat},${pointFrom.lon}~55.76009,37.648801~55.76009,37.548801~${pointTo.lat},${pointTo.lon}")
        val event = BuildRouteEvent.Parser.parse(uri)
        assert(event is BuildRouteEvent)

        val routeEvent = event as BuildRouteEvent
        assert(routeEvent.pointTo!!.point.isIdentical(pointTo))
        assert(routeEvent.pointFrom!!.point.isIdentical(pointFrom))
    }

    @Test
    fun noTargetPointsButManyViaWithDifferentOrder_parsedCorrect() {
        val uri = parseUri("yandexmaps://maps.yandex.ru/?rt=${pointFrom.lon},${pointFrom.lat}~${pointTo.lon},${pointTo.lat}")
        val event = BuildRouteEvent.Parser.parse(uri)
        assert(event is BuildRouteEvent)

        val routeEvent = event as BuildRouteEvent
        assert(routeEvent.pointTo!!.point.isIdentical(pointTo))
        assert(routeEvent.pointFrom!!.point.isIdentical(pointFrom))
    }
}
