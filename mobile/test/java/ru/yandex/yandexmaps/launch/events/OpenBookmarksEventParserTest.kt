package ru.yandex.yandexmaps.launch.events

import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenBookmarksEvent

class OpenBookmarksEventParserTest : UriParserBaseTest() {

    @Test
    fun correctUri_parsedCorrect() {
        val uri = parseUri("yandexmaps://yandex.ru/maps/bookmarks")
        val event = OpenBookmarksEvent.Parser.parse(uri)

        assert(event is OpenBookmarksEvent)
    }
}
