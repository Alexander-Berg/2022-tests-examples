package com.edadeal.android.ui.common

import com.edadeal.android.dto.AuthMethod
import com.edadeal.android.dto.Promo
import com.edadeal.android.model.ads.AdContent
import com.edadeal.android.model.ads.AdImpl
import com.edadeal.android.model.ads.AdItem
import com.edadeal.android.model.ads.AdPosition
import com.edadeal.android.model.entity.CartItem
import com.edadeal.android.model.entity.MetaOffer
import com.edadeal.android.model.entity.OfferEntity
import com.edadeal.android.model.entity.PriceRange
import com.edadeal.android.ui.common.metrics.HasMetricsTrackingTraits
import com.edadeal.android.ui.home.AuthViewCardBinding
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class HasMetricsTrackingTraitsTest(
    private val minVisibleAreaPercentage: Int,
    private val viewTimeout: Int,
    private val item: HasMetricsTrackingTraits
) {

    companion object {
        private val adPosition = AdPosition.Instance(Promo.Screen.main, false, 0)
        private val adContent = object : AdContent {}

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(1, 2000, OfferEntity.EMPTY),
            arrayOf(1, 2000, CartItem("", OfferEntity.EMPTY, 0L)),
            arrayOf(1, 2000, MetaOffer(OfferEntity.EMPTY, PriceRange.EMPTY)),
            arrayOf(1, 2000, AuthViewCardBinding.Item(0L, AuthMethod.LoginSDK, "", "", "")),
            arrayOf(50, 0, AdImpl(AdItem(adContent, adPosition, "", Promo.Banner()), adContent, 0, -1))
        )
    }

    @Test
    fun `assert minVisibleAreaPercentageToAttach and viewTimeout values for adapter items`() {
        val traits = item.getMetricsTrackingTraits()
        val visibilityDurationMillis = traits.visibilityActions
            ?.firstOrNull()
            ?.getVisibilityDurationMillis(item)

        assertEquals(minVisibleAreaPercentage, traits.minVisibleAreaPercentage)
        assertEquals(viewTimeout, visibilityDurationMillis)
    }
}
