package com.edadeal.android.data.room

import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Random
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ConverterBytesToByteStringListTest(
    private val original: List<ByteString>
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<List<ByteString>> = listOf(
            emptyList(),
            listOf(s0),
            listOf(ByteString.EMPTY),
            listOf(s0, s1, ByteString.EMPTY, s2, getRandomByteString(0), getRandomByteString(256), getRandomByteString(1023))
        )

        private fun getRandomByteString(length: Int): ByteString {
            val bytes = ByteArray(length)
            Random().nextBytes(bytes)
            return bytes.toByteString(0, bytes.size)
        }
    }

    @Test
    fun `assert that conversion does not change data`() {
        val converter = Converter()
        val converted = converter.byteStringListToBytes(original)
        val deConverted = converter.bytesToByteStringList(converted)
        assertEquals(original, deConverted)
    }
}
