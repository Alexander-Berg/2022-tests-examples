package ru.yandex.supercheck.model.data.network.api.common

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.squareup.moshi.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import java.lang.reflect.Type

class FallbackJsonAdapterFactoryTest {

    private val mockAdapterFactory = mock<JsonAdapter.Factory>()

    private val type = mock<Type>()
    private val annotations = mock<MutableSet<out Annotation>>()
    private val moshi = mock<Moshi>()

    private val reader = mock<JsonReader>()


    @Test
    fun testWithThrowingExceptionAtParsing() {

        val testException = JsonDataException("Test exception")

        val throwingAdapter = object : JsonAdapter<String>() {
            override fun fromJson(reader: JsonReader?): String? {
                throw testException
            }

            override fun toJson(writer: JsonWriter, value: String?) = Unit
        }

        `when`(reader.peekJson()).thenReturn(reader)

        `when`(
            mockAdapterFactory.create(any(), any(), any())
        ).thenReturn(throwingAdapter)

        val fallbackObject = object {}

        val fallbackJsonAdapterFactory =
            FallbackJsonAdapterFactory(mockAdapterFactory, fallbackObject)

        val fallbackJsonAdapter = fallbackJsonAdapterFactory.create(type, annotations, moshi)!!

        assertEquals(fallbackObject, fallbackJsonAdapter.fromJson(reader))
    }

    @Test
    fun testWithNullMockJsonAdapter() {

        val nullAdapter = object : JsonAdapter<String>() {
            override fun fromJson(reader: JsonReader?): String? {
                return null
            }

            override fun toJson(writer: JsonWriter, value: String?) = Unit
        }

        `when`(reader.peekJson()).thenReturn(reader)

        `when`(
            mockAdapterFactory.create(any(), any(), any())
        ).thenReturn(nullAdapter)

        val fallbackJsonAdapterFactory =
            FallbackJsonAdapterFactory(mockAdapterFactory, object {})

        val fallbackJsonAdapter = fallbackJsonAdapterFactory.create(type, annotations, moshi)!!

        assertEquals(null, fallbackJsonAdapter.fromJson(reader))
    }

}