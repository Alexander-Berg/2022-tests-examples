package com.edadeal.android.data.room

import com.edadeal.android.data.room.entity.JsonConverter
import org.junit.Test
import kotlin.test.assertEquals

class JsonConverterTest {
    @Test
    fun `fromStringToStringList presentation correct`() {
        val converter = JsonConverter()
        val dbValue = "[\"1\",\"2\"]"
        val clientValue = converter.fromStringToStringList(dbValue)
        assertEquals(listOf("1", "2"), clientValue)
    }

    @Test
    fun `stringListToString presentation correct`() {
        val converter = JsonConverter()
        val list = listOf("1", "2")
        val dbValue = converter.stringListToString(list)
        assertEquals("[\"1\",\"2\"]", dbValue)
    }

    @Test
    fun `converter saves and gets saved value`() {
        val converter = JsonConverter()
        val list = listOf("1", "2")
        val converted = converter.stringListToString(list)
        val value = converter.fromStringToStringList(converted)
        assertEquals(list, value)
    }

    @Test
    fun `converter saves empty list and gets empty list`() {
        val converter = JsonConverter()
        val list = listOf<String>()
        val converted = converter.stringListToString(list)
        val value = converter.fromStringToStringList(converted)
        assertEquals(list, value)
    }

    @Test
    fun `converter null equality test`() {
        val converter = JsonConverter()
        val value = converter.fromStringToStringList(null)
        assertEquals(null, value)
    }
}
