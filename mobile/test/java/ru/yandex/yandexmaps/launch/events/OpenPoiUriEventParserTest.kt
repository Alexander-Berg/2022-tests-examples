package ru.yandex.yandexmaps.launch.events

import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenPoiUriEvent

class OpenPoiUriEventParserTest : UriParserBaseTest() {

    private val uriToOpen = "ymapsbm1://org?oid=117966646317"

    @Test
    fun correctUri_parsedCorrect() {
        val uri = parseUri("yandexmaps://?poi[uri]=$uriToOpen")
        val event = OpenPoiUriEvent.Parser.parse(uri)

        assert(event is OpenPoiUriEvent)

        val openUriEvent = event as OpenPoiUriEvent
        assert(openUriEvent.uri == parseUri(uriToOpen))
    }

    @Test
    fun correctUri2_parsedCorrect() {
        val uri = parseUri("yandexmaps://?ouri=$uriToOpen")
        val event = OpenPoiUriEvent.Parser.parse(uri)

        assert(event is OpenPoiUriEvent)

        val openUriEvent = event as OpenPoiUriEvent
        assert(openUriEvent.uri == parseUri(uriToOpen))
    }
}
