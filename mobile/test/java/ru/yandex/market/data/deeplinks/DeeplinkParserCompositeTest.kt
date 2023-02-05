package ru.yandex.market.data.deeplinks

import android.net.Uri
import android.os.Build
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.analitycs.events.health.HealthEvent
import ru.yandex.market.analytics.health.HealthName
import ru.yandex.market.data.deeplinks.links.debug.DebugDeeplink
import ru.yandex.market.deeplinks.DeeplinkSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class DeeplinkParserCompositeTest {

    @get:Rule
    var thrown: ExpectedException = ExpectedException.none()

    private val beruParser = mock<SimpleDeeplinkParser>()
    private val bluemarketParser = mock<SimpleDeeplinkParser>()
    private val yamarketParser = mock<SimpleDeeplinkParser>()
    private val pokupkiParser = mock<SimpleDeeplinkParser>()
    private val yaccParser = mock<YaccDeeplinkParser>()

    @Suppress("DEPRECATION")
    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()

    private val parserComposite =
        DeeplinkParserComposite(
            beruParser,
            bluemarketParser,
            yamarketParser,
            pokupkiParser,
            yaccParser,
            analyticsService
        )

    @Test
    fun `Send event if all parsers throw IllegalArgumentException and source is push`() {

        val uri = Uri.parse("some_scheme")
        val source = DeeplinkSource.PUSH_DEEPLINK
        whenever(beruParser.parse(eq(uri), eq(source))).thenThrow(IllegalArgumentException::class.java)
        whenever(yamarketParser.parse(eq(uri), eq(source))).thenThrow(IllegalArgumentException::class.java)
        whenever(bluemarketParser.parse(eq(uri), eq(source))).thenThrow(IllegalArgumentException::class.java)
        whenever(pokupkiParser.parse(eq(uri), eq(source))).thenThrow(IllegalArgumentException::class.java)
        whenever(yaccParser.parse(eq(uri), eq(source))).thenReturn(null)

        thrown.expect(IllegalArgumentException::class.java)
        val deeplink = parserComposite.parse(uri, source)

        verify(analyticsService).report(argThat<HealthEvent> { this.name == HealthName.PUSH_DEEPLINK_UNKNOWN })
    }

    @Test
    fun `Don't send event if all parsers throw IllegalArgumentException and source is not push`() {

        val uri = Uri.parse("some_scheme")
        val source = DeeplinkSource.SIMPLE_DEEPLINK
        whenever(beruParser.parse(eq(uri), eq(source))).thenThrow(IllegalArgumentException::class.java)
        whenever(yamarketParser.parse(eq(uri), eq(source))).thenThrow(IllegalArgumentException::class.java)
        whenever(bluemarketParser.parse(eq(uri), eq(source))).thenThrow(IllegalArgumentException::class.java)
        whenever(pokupkiParser.parse(eq(uri), eq(source))).thenThrow(IllegalArgumentException::class.java)
        whenever(yaccParser.parse(eq(uri), eq(source))).thenReturn(null)

        thrown.expect(IllegalArgumentException::class.java)
        parserComposite.parse(uri, source)

        verify(analyticsService, never()).report(any<HealthEvent>())
    }

    @Test
    fun `Don't send event if one parser throw IllegalArgumentException and source is push`() {

        val uri = Uri.parse("some_scheme")
        val source = DeeplinkSource.PUSH_DEEPLINK
        whenever(beruParser.parse(eq(uri), eq(source))).thenThrow(IllegalArgumentException::class.java)
        whenever(yamarketParser.parse(eq(uri), eq(source))).thenThrow(IllegalArgumentException::class.java)
        whenever(bluemarketParser.parse(eq(uri), eq(source))).thenReturn(DebugDeeplink(uri))
        whenever(pokupkiParser.parse(eq(uri), eq(source))).thenReturn(DebugDeeplink(uri))
        whenever(yaccParser.parse(eq(uri), eq(source))).thenReturn(null)

        parserComposite.parse(uri, source)

        verify(analyticsService, never()).report(any<HealthEvent>())
    }
}