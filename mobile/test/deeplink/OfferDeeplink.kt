package ru.yandex.market.test.deeplink

import android.net.Uri

data class OfferDeeplink(
    private val offerId: String,
) : TestDeeplink() {

    override fun getURI(): Uri {
        val uri = Uri.Builder()
            .scheme("yamarket")
            .authority("offer")
            .path(offerId)

        return uri.build()
    }
}