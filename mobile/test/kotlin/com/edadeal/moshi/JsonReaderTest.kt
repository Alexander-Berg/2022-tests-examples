package com.edadeal.moshi

import com.squareup.moshi.JsonReader
import okio.Buffer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class JsonReaderTest(
    private val json: String,
    private val rawValue: String?
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<String?>> = listOf(
            "{\"a\": { \"p\": \"text\" } }" to "{ \"p\": \"text\" }",
            "[{\"p\":1} ]" to "{\"p\":1}",
            "{}" to null
        ).map { (json, rawValue) -> arrayOf(json, rawValue) }
    }

    @Test
    fun `readRawValue should return correct value`() {
        val source = Buffer().apply { writeUtf8(json) }
        val reader = JsonReader.of(source)

        val isArray = reader.peek() == JsonReader.Token.BEGIN_ARRAY
        when (isArray) {
            true -> reader.beginArray()
            else -> reader.beginObject()
        }
        var actual: String? = null
        if (reader.hasNext()) {
            if (!isArray) {
                reader.nextName()
            }
            actual = reader.readRawValue(source)
        }

        assertEquals(rawValue, actual)
    }
}
