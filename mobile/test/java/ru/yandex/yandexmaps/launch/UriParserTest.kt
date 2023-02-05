package ru.yandex.yandexmaps.launch

import android.os.Build
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.yandexmaps.launch.parsers.UriParser
import ru.yandex.yandexmaps.multiplatform.core.geometry.Point
import ru.yandex.yandexmaps.multiplatform.core.geometry.isIdentical
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.WrongPatternEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.FeedbackEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenCoronaEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenMapWithCenterEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenPoiUriEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.ShowPointOnMapEvent
import strikt.api.expectThat
import strikt.assertions.isA

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
class UriParserTest : UriParserBaseTest() {

    private val uriParser = UriParser.default()

    private val point = Point(37.258114, 55.655247)
    private val zoom = 15

    @Test
    fun whatsHereWithMultiplePoints_parsedCorrect() {
        whatsHereWithMultiplePoints_parsedCorrect(PATH_BEGINNING_MAPS)
        whatsHereWithMultiplePoints_parsedCorrect(PATH_BEGINNING_NAVI)
    }

    fun whatsHereWithMultiplePoints_parsedCorrect(pathBeginning: String) {
        val uri = parseUri("https://yandex.ru/$pathBeginning/?ll=${point.lon},${point.lat}&pt=${point.lon},${point.lat}&z=$zoom")
        val parsedEvent = uriParser.process(uri).event
        assert(parsedEvent is ShowPointOnMapEvent)

        val event = parsedEvent as ShowPointOnMapEvent
        assert(event.point.isIdentical(point))
        assertEquals(event.zoom!!.toInt(), zoom)
    }

    @Test
    fun whatsHereHaritaWithMultiplePoints_parsedCorrect() {
        val uri = parseUri("https://yandex.com.tr/harita?whatshere[point]=${point.lon},${point.lat}&whatshere[zoom]=$zoom&ll=${point.lon},${point.lat}&z=$zoom")
        val parsedEvent = uriParser.process(uri).event
        assert(parsedEvent is ShowPointOnMapEvent)

        val event = parsedEvent as ShowPointOnMapEvent
        assert(event.point.isIdentical(point))
        assertEquals(event.zoom!!.toInt(), zoom)
    }

    @Test
    fun poiUriAndLl_openPoiUri() {
        poiUriAndLl_openPoiUri(PATH_BEGINNING_MAPS)
        poiUriAndLl_openPoiUri(PATH_BEGINNING_NAVI)
    }

    fun poiUriAndLl_openPoiUri(pathBeginning: String) {
        val uri = parseUri("yandexmaps://yandex.ru/$pathBeginning/44/izhevsk/?ll=53.218547,56.851111&z=15.5&mode=poi&poi[uri]=ymapsbm1://org?oid=1063255794&poi[point]=53.206701,56.850702")
        val parsedEvent = uriParser.process(uri).event
        assertThat(parsedEvent).isInstanceOf(OpenPoiUriEvent::class.java)
    }

    @Test
    fun llAndTrafficMapType_moveMapAndApplyType() {
        val uri = parseUri("yandexmaps://maps.yandex.ru/?ll=73.368212,54.989342&l=map,trf&z=12")
        val parsedEvent = uriParser.process(uri).event
        assertThat(parsedEvent).isInstanceOf(OpenMapWithCenterEvent::class.java)

        val openMapEvent = parsedEvent as OpenMapWithCenterEvent

        assertThat(openMapEvent.mapChangingParams.layersConfig.traffic).isTrue
    }

    @Test
    fun `'feedback' param opens feedback screen, not placecard`() {
        `'feedback' param opens feedback screen, not placecard`(PATH_BEGINNING_MAPS)
        `'feedback' param opens feedback screen, not placecard`(PATH_BEGINNING_NAVI)
    }

    fun `'feedback' param opens feedback screen, not placecard`(pathBeginning: String) {
        val uri = parseUri("yandexmaps://yandex.ru/$pathBeginning/?feedback=edit-organization&oid=1018907821")
        val parsedEvent = uriParser.process(uri).event
        expectThat(parsedEvent).isA<FeedbackEvent.EditOrganization>()
    }

    @Test
    fun `on non-hierarchical URI should return WrongPatternEvent`() {
        `on non-hierarchical URI should return WrongPatternEvent`(PATH_BEGINNING_MAPS)
        `on non-hierarchical URI should return WrongPatternEvent`(PATH_BEGINNING_NAVI)
    }

    fun `on non-hierarchical URI should return WrongPatternEvent`(pathBeginning: String) {
        val uri = parseUri("yandexmaps:yandex.ru/$pathBeginning/?feedback=edit-organization&oid=1018907821")
        val parsedEvent = uriParser.process(uri).event
        expectThat(parsedEvent).isA<WrongPatternEvent>()
    }

    @Test
    fun `'corona' opens webcard screen`() {
        `'corona' opens webcard screen`(PATH_BEGINNING_MAPS)
        `'corona' opens webcard screen`(PATH_BEGINNING_NAVI)
    }

    fun `'corona' opens webcard screen`(pathBeginning: String) {
        val uri = parseUri("yandexmaps://yandex.ru/$pathBeginning/corona")
        val parsedEvent = uriParser.process(uri).event
        expectThat(parsedEvent).isA<OpenCoronaEvent>()
    }

    companion object {
        private const val PATH_BEGINNING_MAPS = "maps"
        private const val PATH_BEGINNING_NAVI = "navi"
    }
}
