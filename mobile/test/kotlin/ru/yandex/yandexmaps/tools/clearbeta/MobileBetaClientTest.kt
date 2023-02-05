package ru.yandex.yandexmaps.tools.clearbeta

import kotlin.test.Test
import kotlin.test.assertEquals

internal class MobileBetaClientTest {

    @Test
    fun quote() {
        val tests = listOf(
            "" to "",
            "dev" to "dev",
            "branch with space" to "branch%20with%20space",
            "hello\n" to "hello%0A",
        )

        for (test in tests) {
            val value = test.first
            val expected = test.second
            val result = MobileBetaClient.quote(value)
            assertEquals(expected, result, "Failed test for '$value'")
        }
    }
}
