package com.edadeal.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ResUrlConstantsTest(private val expected: String, private val actual: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<String>> = listOf(
            arrayOf("https://higgs.edadeal.ru/v3.1/", Res.CONTENT_URL),
            arrayOf("https://teleport.edadeal.yandex.ru/api", Res.COUPONS_URL),
            arrayOf("https://api.edadeal.ru/analytics/", Res.ANALYTICS_URL),
            arrayOf("https://ads.edadeal.ru/v1.1/", Res.ADS_URL),
            arrayOf("https://usr.edadeal.ru/auth/v1/", Res.USR_URL),
            arrayOf("https://cb.edadeal.ru/v2/", Res.CB_URL)
        )
    }

    @Test
    fun `assert constant value is correct`() {
        assertEquals(expected, actual)
    }
}
