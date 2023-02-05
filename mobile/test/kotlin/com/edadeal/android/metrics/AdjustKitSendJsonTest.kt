package com.edadeal.android.metrics

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AdjustKitSendJsonTest(
    private val eventName: String,
    private val json: String,
    private val expectedTokens: List<String>
) : BaseAdjustKitTest() {

    @Test
    fun `should send correct token from json args`() {
        adjustKit.sendEvent(eventName, json)
        when (expectedTokens.isEmpty()) {
            true -> verify(adjustDelegate, never()).trackEvent(any())
            else -> expectedTokens.forEach { verify(adjustDelegate).trackEvent(it) }
        }
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "ButtonClick",
                """{
                    "slug":"offerEcomLink",
                    "BlockPosition":1
                    }
                """.trimIndent(),
                listOf("wnsbj5")
            ),
            arrayOf(
                "ButtonClick",
                """{
                    "slug":"showCouponCode",
                    "BlockPosition":1,
                    "HasBadge": true
                    }
                """.trimIndent(),
                listOf("4o4x3k")
            ),
            arrayOf(
                "ButtonClick",
                """{
                    "slug":"miniOfferEcomLink",
                    "screenName":"MainScreen",
                    "HasBadge":false
                    }
                """.trimIndent(),
                listOf("b5qziu")
            ),
            arrayOf(
                "CouponCTAButtonClick",
                """{
                    "slug":"miniOfferEcomLink",
                    "screenName":"MainScreen",
                    "HasBadge":false
                   }
                """.trimIndent(),
                listOf("lwytsn")
            ),
            arrayOf(
                "ButtonClick",
                """
                    {
                     "slug":"FnsAddPhone",
                     "BlockPosition":1,
                     "HasBadge":false
                    }
                """.trimIndent(),
                listOf("ocqffa", "dqmsye")
            ),
            arrayOf(
                "BannerAppear",
                """
                    {
                     "slug":"FnsAddPhone",
                     "BlockPosition":1,
                     "HasBadge":false
                    }
                """.trimIndent(),
                emptyList<String>()
            ),
            arrayOf(
                "MyCardScreenAppear",
                """
                    {}
                """.trimIndent(),
                listOf("sk5ljy")
            ),
            arrayOf(
                "AddToShoppingListClick",
                """
                    {
                     "slug":"FnsAddPhone",
                     "EventSources":["webapp", "native", "div"]
                    }
                """.trimIndent(),
                listOf("1du9yf")
            )
        )
    }
}
