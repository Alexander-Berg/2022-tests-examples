package com.edadeal.android.model.calibrator

import com.edadeal.android.dto.ScannerConfig.Strategy
import com.edadeal.android.model.barcode.ScannerConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.Buffer
import okio.source
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScannerConfigTest {

    @Test
    fun `scanner config should read correct handling params`() {
        val expected = listOf(
            "egais" to "{\"content\":{\"body\":{\"format\":\"text\"},\"designTemplate\":\"sheet_large\"},\"count\":1}",
            "cards" to "{\"name\":\"@edadeal/lompakko\"}",
            "promo" to "{\"name\":\"@edadeal/promo\"}"
        )

        val actual = parseConfig().strategies.flatMap { strategy ->
            strategy.handlers.mapNotNull { handler ->
                handler.paramsJson.takeUnless { it.isEmpty() }
                    ?.let { handler.handler.slug to it }
            }
        }

        assertEquals(expected, actual)
    }

    @Test
    fun `scanner config should read correct fallback handling`() {
        val actual = parseConfig().strategies.mapNotNull { it.fallbackHandler }

        assertEquals(1, actual.size)
        with(actual.first()) {
            assertTrue(paramsJson.isEmpty())
            assertTrue(handler.regexp.isEmpty())
            assertTrue(handler.params.isEmpty())
            assertTrue(handler.barcodeType.isEmpty())
            assertEquals("fallback", handler.slug)
            assertEquals("fns", handler.handler)
        }
    }

    private fun parseConfig(): ScannerConfig {
        val buffer = Buffer()
        javaClass.getResourceAsStream("/scannerStrategies.json")!!.source()
            .use { buffer.writeAll(it) }
        val strategiesType = Types.newParameterizedType(List::class.java, Strategy::class.java)
        val strategiesAdapter = Moshi.Builder().build().adapter<List<Strategy>>(strategiesType)

        val strategiesJson = buffer.peek().readUtf8()
        val strategies = strategiesAdapter.fromJson(buffer)
        return ScannerConfig.from(strategies!!, strategiesJson)
    }
}
