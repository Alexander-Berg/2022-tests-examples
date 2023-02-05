package ru.yandex.yandexmaps.launch.events

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.core.uri.Uri
import ru.yandex.yandexmaps.multiplatform.core.utils.extensions.urlEncode
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.WrongContentException
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.WrongPatternEvent
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OpenUrlEvent

class OpenUrlEventParserTest : UriParserBaseTest() {

    @Mock
    lateinit var app: Application

    private val simple = "https://yandex.ru"

    private val scheme = "https"
    private val host = "яндекс.рф".urlEncode()
    private val path = "service/find"
    private val valueText = "Display text, кириллик %)".urlEncode()
    private val valueUrl = "https://test.yandex.ru/request?query_param=value".urlEncode()
    private val query = "text=$valueText&url=$valueUrl"
    private val complex = "$scheme://$host:8080/$path?$query"

    private fun makeOpenUrlString(url: String): String {
        val encodedUrl = url.urlEncode()
        return "yandexmaps://open_url?url=$encodedUrl"
    }

    @Test
    fun correctUri_simpleUrlParsedCorrect() {
        val openSimple = makeOpenUrlString(simple)
        val uri = parseUri(openSimple)
        val event = OpenUrlEvent.Parser.parse(uri)

        assert(event is OpenUrlEvent)

        val openUrlEvent = event as OpenUrlEvent
        assertEquals(simple, openUrlEvent.url.toString())
    }

    @Test
    fun correctUri_complexUrlParsedCorrect() {
        val openComplex = makeOpenUrlString(complex)
        val uri = parseUri(openComplex)
        val event = OpenUrlEvent.Parser.parse(uri)

        assert(event is OpenUrlEvent)

        val openUrlEvent = event as OpenUrlEvent
        assertEquals(complex, openUrlEvent.url.toString())
    }

    @Test
    fun correctUri_recursiveUrlParsedCorrect() {
        val openComplex = makeOpenUrlString(complex)
        val openRecursive = makeOpenUrlString(openComplex)

        // openComplex == "yandexmaps://open_url?
        // url=https%3A%2F%2F%25D1%258F%25D0%25BD%25D0%25B4%25D0%25B5%25D0%25BA%25D1%2581.%25D1%2580%25D1%2584%3A8080%2Fservice%2Ffind%3F
        // text%3DDisplay%2Btext%252C%2B%25D0%25BA%25D0%25B8%25D1%2580%25D0%25B8%25D0%25BB%25D0%25BB%25D0%25B8%25D0%25BA%2B%2525%2529%26
        // url%3Dhttps%253A%252F%252Ftest.yandex.ru%252Frequest%253Fquery_param%253Dvalue"

        // openRecursuve == "yandexmaps://open_url?url=yandexmaps%3A%2F%2Fopen_url%3F
        // url%3Dhttps%253A%252F%252F%2525D1%25258F%2525D0%2525BD%2525D0%2525B4%2525D0%2525B5%2525D0%2525BA%2525D1%252581.%2525D1%252580%2525D1%252584%253A8080%252Fservice%252Ffind%253F
        // text%253DDisplay%252Btext%25252C%252B%2525D0%2525BA%2525D0%2525BE%2525D1%252582%2525D0%2525BE%2525D1%252580%2525D1%25258B%2525D0%2525B9%252B%2525D0%2525BF%2525D1%252580
        // %2525D0%2525BE%2525D1%252581%2525D1%252582%2525D0%2525BE%252B%2525D1%252581%2525D1%252582%2525D1%252580%2525D0%2525BE%2525D0%2525BA%2525D0%2525B0%252B%252525%252529%2526
        // url%253Dhttps%25253A%25252F%25252Ftest.yandex.ru%25252Frequest%25253Fquery_param%25253Dvalue"

        val firstLevelUri = parseUri(openRecursive)
        val firstLevelEvent = OpenUrlEvent.Parser.parse(firstLevelUri)

        assert(firstLevelEvent is OpenUrlEvent)

        val firstLevelOpenUrlEvent = firstLevelEvent as OpenUrlEvent
        assertEquals(openComplex, firstLevelOpenUrlEvent.url.toString())

        val secondLevelEvent = OpenUrlEvent.Parser.parse(firstLevelOpenUrlEvent.url)

        assert(secondLevelEvent is OpenUrlEvent)
        val secondLevelOpenUrlEvent = secondLevelEvent as OpenUrlEvent
        assertEquals(complex, secondLevelOpenUrlEvent.url.toString())
    }

    @Test
    fun wrongAuthority_wrongPattern() {
        val encoded = simple.urlEncode()
        val uri = parseUri("yandexmaps://wrong_authority?url=$encoded")
        val event = OpenUrlEvent.Parser.parse(uri)

        assert(event is WrongPatternEvent)
    }

    @Test(expected = WrongContentException::class)
    fun wrongUrl_wrongContent() {
        val uri = parseUri("yandexmaps://open_url?")
        OpenUrlEvent.Parser.parse(uri)
    }
}

private fun Uri.toDecodedString(): String? {
    return Uri.decode(toString())
}
