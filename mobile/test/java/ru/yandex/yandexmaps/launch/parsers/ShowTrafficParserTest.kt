package ru.yandex.yandexmaps.launch.parsers

import android.app.Application
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.launch.parsers.events.traffic.City
import ru.yandex.yandexmaps.launch.parsers.events.traffic.ShortNameCities
import ru.yandex.yandexmaps.launch.parsers.events.traffic.ShowTrafficParser
import ru.yandex.yandexmaps.multiplatform.core.geometry.Point
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.WrongPatternEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenMapWithCenterEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.models.ParsedBoundingBox
import utils.shouldReturn

class ShowTrafficParserTest : UriParserBaseTest() {

    @Mock
    lateinit var app: Application

    @Mock
    lateinit var citiesList: ShortNameCities

    private val fullCityName = "moscow"
    private val shortCityName = "msk"
    private val city = City(fullCityName, shortCityName, ParsedBoundingBox(Point(59.939095, 30.315868), Point(0.0, 0.0)))

    override fun setUp() {
        super.setUp()
        citiesList[shortCityName].shouldReturn(city)
    }

    @Test
    fun correctPath_parsedCorrect() {
        val uri = parseUri("yandexmaps://traffic/${shortCityName}_traffic") // todo: what should be instead of first "traffic"?

        val event = ShowTrafficParser(citiesList).parse(uri)
        assert(event is OpenMapWithCenterEvent)

        val searchEvent = event as OpenMapWithCenterEvent
        assert(searchEvent.boundingBox != null)
    }

    @Test
    fun incorrectPath_incorrectPattern() {
        val uri = parseUri("yandexmaps://traffic/${shortCityName}_ciffart")
        val event = ShowTrafficParser(citiesList).parse(uri)
        assert(event is WrongPatternEvent)
    }
}
