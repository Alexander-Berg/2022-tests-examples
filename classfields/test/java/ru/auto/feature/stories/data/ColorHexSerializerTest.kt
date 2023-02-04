package ru.auto.feature.stories.data

import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.test.runner.AllureRobolectricRunner
import kotlin.test.assertEquals

/**
 * @author themishkun on 2019-06-18.
 */
@RunWith(AllureRobolectricRunner::class)
class ColorHexSerializerTest {

    @Test
    fun `should serialize and deserialize with alpha`() {
        val color = "\"#88ffaa00\""
        val deserialized = Json.decodeFromString(ColorHexSerializer, color)
        val serialized = Json.encodeToString(ColorHexSerializer, deserialized)
        assertEquals(color, serialized)
    }

    @Test
    fun `should deserialize without alpha and add FF to alpha channel`() {
        val argbColor = "\"#ffffaa00\""
        val rgbColor = "\"#ffaa00\""
        val deserializedArgb = Json.decodeFromString(ColorHexSerializer, argbColor)
        val deserializedRgb = Json.decodeFromString(ColorHexSerializer, rgbColor)
        assertEquals(deserializedArgb, deserializedRgb)
    }
}
