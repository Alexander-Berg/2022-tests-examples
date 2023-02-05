package ru.yandex.yandexmaps.launch.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.WrongPatternEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenModeEvent

class OpenModeEventTest : UriParserBaseTest() {
    @Test
    fun `when uri mode=navigator then parsed event is OpenModeEvent with correct mode`() {
        val uri = parseUri("yandexmaps://maps.yandex.ru/?mode=navigator")
        val event = OpenModeEvent.Parser.parse(uri)

        assertTrue(event is OpenModeEvent)
        assertEquals((event as OpenModeEvent).mode, OpenModeEvent.Mode.NAVIGATOR)
    }

    @Test
    fun `when uri mode is unknown then parsed event is WrongPatternEvent`() {
        val uri = parseUri("yandexmaps://maps.yandex.ru/?mode=xxxOaksnanqjnwkeqkjwnekqjkaWJBEBAWBwbwbqnqbwajNN")
        val event = OpenModeEvent.Parser.parse(uri)

        assertTrue(event is WrongPatternEvent)
    }
}
