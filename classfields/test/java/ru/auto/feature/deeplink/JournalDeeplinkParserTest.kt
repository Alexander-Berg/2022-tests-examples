package ru.auto.feature.deeplink

import android.net.Uri
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import ru.auto.ara.RobolectricTest
import ru.auto.ara.deeplink.parser.JournalDeeplinkParser
import ru.auto.ara.util.android.StringsProvider
import ru.auto.core_ui.util.Consts
import ru.auto.test.runner.AllureRobolectricRunner
import kotlin.test.assertEquals

@RunWith(AllureRobolectricRunner::class) class JournalDeeplinkParserTest : RobolectricTest() {

    private val stringsProvider: StringsProvider = Mockito.mock(StringsProvider::class.java, { MANUAL_TITLE })
    private val journalDeeplinkParser = JournalDeeplinkParser(stringsProvider)

    @Test
    fun `should parse manual https url correctly`() {
        testParse(Uri.parse(CORRECT_HTTPS_MANUAL_URL), MANUAL_TITLE)
    }

    @Test
    fun `should parse manual autoru scheme url correctly`() {
        testParse(Uri.parse(CORRECT_AUTORU_MANUAL_APPLINK), MANUAL_TITLE, Uri.parse(CORRECT_HTTPS_MANUAL_URL))
    }

    @Test
    fun `should parse journal https url correctly`() {
        testParse(Uri.parse(CORRECT_HTTPS_JOURNAL_URL), null)
    }

    @Test
    fun `should parse journal autoru scheme url correctly`() {
        testParse(Uri.parse(CORRECT_AUTORU_JOURNAL_APPLINK), null, Uri.parse(CORRECT_HTTPS_JOURNAL_URL))
    }

    @Test
    fun `should parse url with web in path correctly`() {
        testParse(Uri.parse(CORRECT_JOURNAL_URL_WITH_WEB_IN_SEGMENTS), null, Uri.parse(PARSED_JOURNAL_URL_WITH_WEB_IN_SEGMENTS))
    }

    private fun testParse(uri: Uri, title: String?, parsedUri: Uri = uri) {
        if (journalDeeplinkParser.checkPrecondition(uri)) {
            val result = journalDeeplinkParser.parseAfterCheck(uri)
            val correctResultUri = parsedUri.buildUpon().toString()
            assertEquals(result.getValue(Consts.EXTRA_URL), correctResultUri)
            assertEquals(result.getValue(Consts.EXTRA_TITLE), title)
        }
    }
}

private const val MANUAL_TITLE = "Учебник"

private const val CORRECT_HTTPS_MANUAL_URL = "https://mag.auto.ru/theme/uchebnik/?utm_content=main_menu"
private const val CORRECT_AUTORU_MANUAL_APPLINK = "autoru://web/mag.auto.ru/theme/uchebnik/?utm_content=main_menu"
private const val CORRECT_HTTPS_JOURNAL_URL = "https://mag.auto.ru/article/pollaboutautomanuans/"
private const val CORRECT_AUTORU_JOURNAL_APPLINK = "autoru://web/mag.auto.ru/article/pollaboutautomanuans/"
private const val CORRECT_JOURNAL_URL_WITH_WEB_IN_SEGMENTS = "autoru://web/mag.auto.ru/article/pollaboutautomanuans_in_web/"
private const val PARSED_JOURNAL_URL_WITH_WEB_IN_SEGMENTS = "https://mag.auto.ru/article/pollaboutautomanuans_in_web/"
