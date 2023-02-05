package ru.yandex.yandexmaps.launch.events

import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.WrongPatternEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenStoryEvent

class OpenStoryEventParserTest : UriParserBaseTest() {

    @Test
    fun storyId_parsedCorrectly() {
        val uri = parseUri("yandexmaps://maps.yandex.ru/stories/32275d70-f469-46cd-a2c1-95247a9d2fb4")
        val event = OpenStoryEvent.Parser.parse(uri)

        assert(event is OpenStoryEvent)

        val casted = event as OpenStoryEvent
        assert(casted.id == "32275d70-f469-46cd-a2c1-95247a9d2fb4")
        assert(!casted.fullscreenShowcase)
    }

    @Test
    fun storyIdWithBackgroundMode_parsedCorrectly() {
        val uri = parseUri("yandexmaps://maps.yandex.ru/stories/32275d70-f469-46cd-a2c1-95247a9d2fb4?background=showcase")

        val event = OpenStoryEvent.Parser.parse(uri)

        assert(event is OpenStoryEvent)

        val casted = event as OpenStoryEvent
        assert(casted.id == "32275d70-f469-46cd-a2c1-95247a9d2fb4")
        assert(casted.fullscreenShowcase)
    }

    @Test
    fun wrongPathSegment_wrongPattern() {
        val uri = parseUri("yandexmaps://maps.yandex.ru/wrong_segment/32275d70-f469-46cd-a2c1-95247a9d2fb4")

        val event = OpenStoryEvent.Parser.parse(uri)

        assert(event is WrongPatternEvent)
    }

    @Test
    fun noStoryId_wrongPattern() {
        val uri = parseUri("yandexmaps://maps.yandex.ru/stories/")

        val event = OpenStoryEvent.Parser.parse(uri)

        assert(event is WrongPatternEvent)
    }
}
