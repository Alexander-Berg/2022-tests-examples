package com.yandex.mobile.realty.requests

import com.yandex.mobile.realty.network.SecurityInterceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author misha-kozlov on 13.05.2020
 */
class SecurityInterceptorTest {

    @Test
    @Suppress("MaxLineLength")
    fun testSecurityParameterGenerationGetRequest() {
        val request = Request.Builder()
            .url("https://api.realty.yandex.net/1.0/cardWithViews.json?category=HOUSE&currency=RUR&id=9105050433078320640&objectType=OFFER&priceMax=11000000&priceType=PER_OFFER&showOnMobile=YES&type=SELL")
            .build()
        val parameter = SecurityInterceptor.calculateParameter(
            request = request,
            uuid = UUID,
            extra = EXTRA,
            fallback = { FALLBACK_PARAM }
        )
        assertEquals(
            "category=housecurrency=rurid=9105050433078320640objecttype=offerpricemax=11000000pricetype=per_offershowonmobile=yestype=sella308ae2137ac4bdf90fa9e3c39174257$EXTRA",
            parameter
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun testSecurityParameterGenerationPostRequest() {
        val request = Request.Builder()
            .url("https://api.realty.yandex.net/1.0/cardWithViews.json?category=HOUSE&currency=RUR&id=9105050433078320640&objectType=OFFER&priceMax=11000000&priceType=PER_OFFER&showOnMobile=YES&type=SELL")
            .post("".toRequestBody(null))
            .build()
        val parameter = SecurityInterceptor.calculateParameter(
            request = request,
            uuid = UUID,
            extra = EXTRA,
            fallback = { FALLBACK_PARAM }
        )
        assertEquals(
            FALLBACK_PARAM,
            parameter
        )
    }

    @Test
    fun testSecurityParameterGenerationEmptyQuery() {
        val request = Request.Builder()
            .url("https://api.realty.yandex.net/1.0/cardWithViews.json")
            .build()
        val parameter = SecurityInterceptor.calculateParameter(
            request = request,
            uuid = UUID,
            extra = EXTRA,
            fallback = { FALLBACK_PARAM }
        )
        assertEquals(
            "a308ae2137ac4bdf90fa9e3c39174257$EXTRA",
            parameter
        )
    }

    companion object {

        private const val UUID = "A308ae2137ac4bdf90fa9e3c39174257"
        private const val EXTRA = "Extra"
        private const val FALLBACK_PARAM = "FallbackParam"
    }
}
