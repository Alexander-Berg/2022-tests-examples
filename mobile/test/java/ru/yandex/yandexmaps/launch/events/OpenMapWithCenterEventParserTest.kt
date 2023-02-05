package ru.yandex.yandexmaps.launch.events

import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.core.geometry.createPoint
import ru.yandex.yandexmaps.multiplatform.core.geometry.isIdentical
import ru.yandex.yandexmaps.multiplatform.core.utils.extensions.closeTo
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.MapChangingParams
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenMapWithCenterEvent

class OpenMapWithCenterEventParserTest : UriParserBaseTest() {

    private val point = createPoint(59.951059, 30.310182)
    private val zoom = 18

    @Test
    fun openByPointWithMapStyles_parsedCorrect() {
        val uri = parseUri("yandexmaps://yandex.ru/maps?ll=${point.lon},${point.lat}&z=$zoom&l=map,trf")
        val event = OpenMapWithCenterEvent.Parser.parse(uri)
        assert(event is OpenMapWithCenterEvent)

        val openMapEvent = event as OpenMapWithCenterEvent
        assert(openMapEvent.mapCenter!!.isIdentical(point))

        val mapChangingParams = openMapEvent.mapChangingParams
        assert(mapChangingParams.mapAppearance == MapChangingParams.MapAppearance.VECTOR_MAP)
        assert(mapChangingParams.layersConfig.traffic ?: false)
    }

    @Test
    fun openMapWithSpan_parsedCorrect() {
        val uri = parseUri("yandexmaps://yandex.ru/maps?ll=${point.lon},${point.lat}&spn=1.730347,0.738038")
        val event = OpenMapWithCenterEvent.Parser.parse(uri)
        assert(event is OpenMapWithCenterEvent)

        val openMapEvent = event as OpenMapWithCenterEvent
        assert(openMapEvent.boundingBox != null)
    }

    @Test
    fun openMapFromSeparatedCoordsParams_parsedCorrect() {
        val uri = parseUri("yandexmaps://yandex.ru/maps?lon=${point.lon}&lat=${point.lat}&z=$zoom")
        val event = OpenMapWithCenterEvent.Parser.parse(uri)
        assert(event is OpenMapWithCenterEvent)

        val openMapEvent = event as OpenMapWithCenterEvent
        assert(openMapEvent.mapCenter!!.isIdentical(point))
        assert(openMapEvent.zoom!!.closeTo(zoom.toFloat()))
    }
}
