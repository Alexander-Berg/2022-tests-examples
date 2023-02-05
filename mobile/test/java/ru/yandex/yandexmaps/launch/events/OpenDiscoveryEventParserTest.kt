package ru.yandex.yandexmaps.launch.events

import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenDiscoveryEvent

class OpenDiscoveryEventParserTest : UriParserBaseTest() {

    @Test
    fun correctUri_parsedCorrect() {
        val discovery = "winter2017"

        val uri = parseUri("yandexmaps://yandex.ru/maps/discovery/$discovery")

        val event = OpenDiscoveryEvent.Parser.parse(uri)
        assert(event is OpenDiscoveryEvent)

        val discoveryEvent = event as OpenDiscoveryEvent
        assert(discoveryEvent.segmentId == discovery)
    }
}
