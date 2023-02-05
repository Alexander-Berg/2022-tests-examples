package ru.yandex.yandexmaps.launch.events

import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.ExternalEvent

class ExternalEventParserTest : UriParserBaseTest() {

    @Test
    fun correctUri_parsedCorrect() {
        val uri = parseUri("yandexmaps://?um=whatever")
        val event = ExternalEvent.Parser.parse(uri)

        assert(event is ExternalEvent)
        val externalEvent = event as ExternalEvent

        assert(externalEvent.uri == uri)
    }
}
