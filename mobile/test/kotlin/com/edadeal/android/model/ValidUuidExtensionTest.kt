package com.edadeal.android.model

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ValidUuidExtensionTest(private val uuid: String, private val expected: Boolean) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): List<Any> = listOf(
            arrayOf("94d98382-42c3-11e6-9419-52540010b608", true),
            arrayOf("94d98382c3-11e6-9419-52540010b608", false),
            arrayOf("", false),
            arrayOf("00000000-0000-0000-0000-000000000000", true)
        )
    }

    @Test
    fun `assert that string is valid uuid`() {
        assertEquals(expected, uuid.isValidUuid())
    }
}
