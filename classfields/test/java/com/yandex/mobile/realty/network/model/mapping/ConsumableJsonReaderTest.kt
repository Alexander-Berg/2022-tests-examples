package com.yandex.mobile.realty.network.model.mapping

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.yandex.mobile.realty.data.mapping.EmptyDescriptor
import com.yandex.mobile.realty.data.model.publication.ConsumableJsonReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @author andrey-bgm on 19/02/2020.
 */
class ConsumableJsonReaderTest {

    @Test
    fun readValues() {
        val json = JsonObject().apply {
            addProperty("string", "string value")
            addProperty("bool", true)
            addProperty("int", 4)
            add(
                "list",
                JsonArray().apply {
                    add(5)
                    add(6)
                }
            )
        }
        val reader = createReader(json)

        assertEquals("string value", reader.readString("string"))
        assertEquals(true, reader.readBoolean("bool"))
        assertEquals(4, reader.readValue("int", Converters.intConverter))
        assertEquals(listOf(5, 6), reader.readValues("list", Converters.intConverter))
        assertTrue(json.keySet().isEmpty())
    }

    @Test
    fun readNullValues() {
        val json = JsonObject().apply {
            add("string", null)
            add("bool", null)
            add("int", null)
            add("list", null)
        }
        val reader = createReader(json)

        assertNull(reader.readString("string"))
        assertNull(reader.readBoolean("bool"))
        assertNull(reader.readValue("int", Converters.intConverter))
        assertNull(reader.readValues("list", Converters.intConverter))
        assertTrue(json.keySet().isEmpty())
    }

    @Test
    fun unreadFieldShouldStayUnconsumed() {
        val json = JsonObject().apply {
            addProperty("field1", "value1")
            addProperty("field2", "value2")
        }
        val reader = createReader(json)

        reader.readString("field1")

        assertEquals(setOf("field2"), json.keySet())
    }

    @Test(expected = JsonSyntaxException::class)
    fun shouldThrowWhenInvalidValue() {
        val json = JsonObject().apply {
            add("int", JsonArray())
        }
        val reader = createReader(json)

        reader.readValue("int", Converters.intConverter)
    }

    @Test(expected = IllegalStateException::class)
    fun shouldThrowWhenInvalidStringValue() {
        val json = JsonObject().apply {
            add("string", JsonArray())
        }
        val reader = createReader(json)

        reader.readString("string")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldThrowWhenInvalidBooleanValue() {
        val json = JsonObject().apply {
            add("bool", JsonArray())
        }
        val reader = createReader(json)

        reader.readBoolean("bool")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldThrowWhenAnotherPrimitiveInsteadBooleanValue() {
        val json = JsonObject().apply {
            addProperty("bool", "string value")
        }
        val reader = createReader(json)

        reader.readBoolean("bool")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldThrowWhenInvalidArrayValue() {
        val json = JsonObject().apply {
            addProperty("list", 14)
        }
        val reader = createReader(json)

        reader.readValues("list", Converters.intConverter)
    }

    @Test(expected = JsonSyntaxException::class)
    fun shouldThrowWhenInvalidArrayItemValue() {
        val json = JsonObject().apply {
            add(
                "list",
                JsonArray().apply {
                    add(JsonArray())
                }
            )
        }
        val reader = createReader(json)

        reader.readValues("list", Converters.intConverter)
    }

    private fun createReader(json: JsonObject): ConsumableJsonReader {
        return ConsumableJsonReader(Gson(), json, EmptyDescriptor)
    }
}
