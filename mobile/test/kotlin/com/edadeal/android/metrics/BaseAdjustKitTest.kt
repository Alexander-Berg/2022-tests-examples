package com.edadeal.android.metrics

import com.edadeal.android.dto.Analytics
import com.nhaarman.mockito_kotlin.mock

open class BaseAdjustKitTest {

    protected val adjustDelegate = mock<AdjustDelegate>()
    protected val adjustKit = AdjustKit(adjustDelegate)

    init {
        adjustKit.updateEvents(events)
    }

    companion object {
        @JvmStatic
        private val events = listOf(
            Analytics.Event(
                name = "ButtonClick",
                adjustToken = "wnsbj5",
                requiredAttributes = mapOf("slug" to "offerEcomLink")
            ),
            Analytics.Event(
                name = "ButtonClick",
                adjustToken = "4o4x3k",
                requiredAttributes = mapOf("slug" to "showCouponCode")
            ),
            Analytics.Event(
                name = "ButtonClick",
                adjustToken = "ocqffa",
                requiredAttributes = mapOf("slug" to "FnsAddPhone")
            ),
            Analytics.Event(
                name = "ButtonClick",
                adjustToken = "b5qziu",
                requiredAttributes = mapOf(
                    "slug" to "miniOfferEcomLink",
                    "screenName" to ScreenName.main
                )
            ),
            Analytics.Event(
                name = "ButtonClick",
                adjustToken = "dqmsye",
                requiredAttributes = mapOf(
                    "slug" to "FnsAddPhone",
                    "HasBadge" to false
                )
            ),
            Analytics.Event(
                name = "CouponCTAButtonClick",
                adjustToken = "lwytsn",
                requiredAttributes = mapOf<String, Any>(
                    "HasBadge" to false
                )
            ),
            Analytics.Event(
                name = "MyCardScreenAppear",
                adjustToken = "sk5ljy",
                requiredAttributes = emptyMap()
            ),
            Analytics.Event(
                name = "AddToShoppingListClick",
                adjustToken = "1du9yf",
                requiredAttributes = mapOf(
                    "EventSources" to listOf("webapp", "native", "div")
                )
            )
        )
    }
}
