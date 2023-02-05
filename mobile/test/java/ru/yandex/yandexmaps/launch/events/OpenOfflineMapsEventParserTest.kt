package ru.yandex.yandexmaps.launch.events

import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenOfflineMapsEvent

class OpenOfflineMapsEventParserTest : UriParserBaseTest() {

    @Test
    fun correctUri_parsedCorrect() {
        val uri = parseUri("yandexmaps://yandex.ru/maps/offline-maps")

        val event = OpenOfflineMapsEvent.Parser.parse(uri)
        assert(event is OpenOfflineMapsEvent)
    }
}
