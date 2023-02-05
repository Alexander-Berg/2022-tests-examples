package com.edadeal.android.metrics

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AdjustKitSendArgsTest(
    private val eventName: String,
    private val args: MutableMap<String, Any>,
    private val expectedTokens: List<String>
) : BaseAdjustKitTest() {

    @Test
    fun `should send correct token from map args`() {
        adjustKit.sendEvent(eventName, args)
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
                mapOf(
                    "slug" to "offerEcomLink",
                    "BlockPosition" to 1
                ),
                listOf("wnsbj5")
            ),
            arrayOf(
                "ButtonClick",
                mapOf(
                    "slug" to "showCouponCode",
                    "BlockPosition" to 1,
                    "HasBadge" to true
                ),
                listOf("4o4x3k")
            ),
            arrayOf(
                "ButtonClick",
                mapOf(
                    "slug" to "miniOfferEcomLink",
                    "screenName" to ScreenName.main,
                    "HasBadge" to false
                ),
                listOf("b5qziu")
            ),
            arrayOf(
                "CouponCTAButtonClick",
                mapOf(
                    "slug" to "miniOfferEcomLink",
                    "screenName" to ScreenName.main,
                    "HasBadge" to false
                ),
                listOf("lwytsn")
            ),
            arrayOf(
                "ButtonClick",
                mapOf(
                    "slug" to "FnsAddPhone",
                    "BlockPosition" to 1,
                    "HasBadge" to false
                ),
                listOf("ocqffa", "dqmsye")
            ),
            arrayOf(
                "BannerAppear",
                mapOf(
                    "slug" to "FnsAddPhone",
                    "BlockPosition" to 1,
                    "HasBadge" to false
                ),
                emptyList<String>()
            )
        )
    }
}
