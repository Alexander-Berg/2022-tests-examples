package com.edadeal.android.model

import okio.ByteString
import okio.ByteString.Companion.decodeHex
import org.junit.Test
import kotlin.test.assertEquals

class ByteStringExtensionsTest {

    @Test
    fun `toUuidString should return zeroes for empty or incorrect ByteString`() {
        val emptyUUID = "00000000-0000-0000-0000-000000000000"
        assertEquals(ByteString.EMPTY.toUuidString(), emptyUUID)
        assertEquals("0123456789abcdef".decodeHex().toUuidString(), emptyUUID)
    }
}
