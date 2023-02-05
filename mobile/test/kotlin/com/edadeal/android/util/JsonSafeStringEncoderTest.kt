package com.edadeal.android.util

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class JsonSafeStringEncoderTest(
    private val input: String,
    private val output: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<String>> = listOf(
            arrayOf("", ""),
            arrayOf("\t", "\\t"),
            arrayOf("\"", "\\\""),
            arrayOf("text", "text"),
            arrayOf("\\{}", "\\\\{}"),
            arrayOf("some \"text\"", "some \\\"text\\\"")
        )

        private val encoder = JsonSafeStringEncoder()
    }

    @Test
    fun `should return expected output`() {
        assertEquals(output, encoder.encode(input))
    }
}
