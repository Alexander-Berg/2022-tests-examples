package com.edadeal.android.data.room

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ConverterLongListToBytesTest(
    private val expected: List<Long>?
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<List<Long>?> = listOf(
            listOf(0x123456789abcdefL, 0x74726f6c6f6c6f3L, -524430722953823987L, Long.MIN_VALUE, Long.MAX_VALUE),
            listOf(9223372036854775806L),
            emptyList(),
            null
        )
    }

    @Test
    fun `conversion should keep data unchanged`() {
        val converter = Converter()
        val converted = converter.longListToBytes(expected)
        assertEquals(expected, converter.bytesToLongList(converted))
    }
}
