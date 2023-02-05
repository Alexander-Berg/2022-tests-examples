package ru.yandex.yandexmaps.launch.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.WrongPatternEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenMtCardEvent

class OpenMtCardParserTest : UriParserBaseTest() {
    @Test
    fun openStopCard_parsedCorrect() {
        val stopId = "stop_id_1234567890"
        val uri = parseUri("yandexmaps://maps.yandex.ru/?masstransit[stopId]=$stopId")
        val event = OpenMtCardEvent.Parser.parse(uri)

        assertTrue(event is OpenMtCardEvent.Stop)
        val parsedEvent = event as OpenMtCardEvent.Stop
        assertEquals(parsedEvent.stopId, stopId)
    }

    @Test
    fun openThreadCard_parsedCorrect() {
        val lineId = "line_id_1234567890"
        val threadId = "thread_id_1234567890"
        val uri = parseUri("yandexmaps://maps.yandex.ru/?masstransit[routeId]=$lineId&masstransit[threadId]=$threadId")
        val event = OpenMtCardEvent.Parser.parse(uri)

        assertTrue(event is OpenMtCardEvent.Thread)
        val parsedEvent = event as OpenMtCardEvent.Thread
        assertEquals(parsedEvent.lineId, lineId)
        assertEquals(parsedEvent.threadId, threadId)
    }

    @Test
    fun openThreadCardWithOneLostParam_parsedCorrect() {
        val lineId = "line_id_1234567890"
        val uri = parseUri("yandexmaps://maps.yandex.ru/?masstransit[routeId]=$lineId")
        val event = OpenMtCardEvent.Parser.parse(uri)

        assertTrue(event is OpenMtCardEvent.Thread)
        val parsedEvent = event as OpenMtCardEvent.Thread
        assertEquals(parsedEvent.lineId, lineId)
        assertEquals(parsedEvent.threadId, null)
    }

    @Test
    fun openThreadCardWithAnotherLostParam_parsedWrong() {
        val threadId = "thread_id_1234567890"
        val uri = parseUri("yandexmaps://maps.yandex.ru/?masstransit[threadId]=$threadId")
        val event = OpenMtCardEvent.Parser.parse(uri)

        assertTrue(event is WrongPatternEvent)
    }
}
