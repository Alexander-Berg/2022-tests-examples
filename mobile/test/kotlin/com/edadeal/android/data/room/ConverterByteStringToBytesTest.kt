package com.edadeal.android.data.room

import okio.ByteString
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ConverterByteStringToBytesTest(
    private val original: ByteString
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<ByteString> = listOf(s0, s1, s2)
    }

    @Test
    fun `assert that conversion does not change data`() {
        val converter = Converter()
        val converted = converter.byteStringToBytes(original)
        val deConverted = converter.bytesToByteString(converted)
        assertEquals(original, deConverted)
    }
}
