package ru.yandex.yandexmaps.launch.events

import android.app.Application
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenUserLocationEvent

class OpenUserLocationEventParserTest : UriParserBaseTest() {

    @Mock
    lateinit var app: Application

    @Test
    fun userLocation_parsedCorrect() {
        val uri = parseUri("yandexmaps://?ll=user_location")
        val event = OpenUserLocationEvent.Parser.parse(uri)

        assert(event is OpenUserLocationEvent)
    }
}
