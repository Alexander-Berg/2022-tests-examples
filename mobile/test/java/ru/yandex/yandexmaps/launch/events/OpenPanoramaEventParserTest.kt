package ru.yandex.yandexmaps.launch.events

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.core.geometry.createPoint
import ru.yandex.yandexmaps.multiplatform.core.geometry.isIdentical
import ru.yandex.yandexmaps.multiplatform.core.utils.extensions.closeTo
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenPanoramaEvent

@RunWith(RobolectricTestRunner::class)
class OpenPanoramaEventParserTest : UriParserBaseTest() {

    private val point = createPoint(55.773317, 37.412339)
    private val span = createPoint(130.000000, 71.919192)
    private val azimuth = 228.970000
    private val tilt = 6.060547
    private val historicalId = "1298045253_672967335_23_1381663150"

    @Test
    fun uriWithoutHistorical_parsedCorrect() {
        val uri = parseUri("yandexmaps://?panorama[point]=${point.lon},${point.lat}&panorama[direction]=$azimuth,$tilt")
        val event = OpenPanoramaEvent.Parser.parse(uri)

        assert(event is OpenPanoramaEvent)

        val panoramaEvent = event as OpenPanoramaEvent
        val mapState = panoramaEvent.mapState
        val panoramaState = panoramaEvent.panoramaState

        assert(panoramaState == null)
        assert(mapState.azimuth.closeTo(azimuth))
        assert(mapState.point.isIdentical(point))
    }

    @Test
    fun uriWithHistorical_parsedCorrect() {
        val uri = parseUri("yandexmaps://?panorama[point]=${point.lon},${point.lat}&panorama[direction]=$azimuth,$tilt&panorama[span]=${span.lon},${span.lat}&panorama[id]=$historicalId")
        val event = OpenPanoramaEvent.Parser.parse(uri)

        assert(event is OpenPanoramaEvent)

        val panoramaEvent = event as OpenPanoramaEvent
        val mapState = panoramaEvent.mapState
        val panoramaState = panoramaEvent.panoramaState!!

        assert(mapState.azimuth.closeTo(azimuth))
        assert(mapState.point.isIdentical(point))
        assert(panoramaState.azimuth.closeTo(azimuth))
        assert(panoramaState.tilt.closeTo(tilt))
        assert(panoramaState.span!!.horizontalAngle.closeTo(span.lon))
        assert(panoramaState.span!!.verticalAngle.closeTo(span.lat))
        assert(panoramaState.id == historicalId)
    }

    @Test
    fun uriWithoutSpan_parsedCorrect() {
        val uri = parseUri("yandexmaps://?panorama[point]=${point.lon},${point.lat}&panorama[direction]=$azimuth,$tilt&panorama[span]=,&panorama[id]=$historicalId")
        val event = OpenPanoramaEvent.Parser.parse(uri)

        assert(event is OpenPanoramaEvent)

        val panoramaEvent = event as OpenPanoramaEvent
        val mapState = panoramaEvent.mapState
        val panoramaState = panoramaEvent.panoramaState!!

        assert(mapState.azimuth.closeTo(azimuth))
        assert(mapState.point.isIdentical(point))
        assert(panoramaState.azimuth.closeTo(azimuth))
        assert(panoramaState.tilt.closeTo(tilt))
        assert(panoramaState.span == null)
        assert(panoramaState.id == historicalId)
    }
}
