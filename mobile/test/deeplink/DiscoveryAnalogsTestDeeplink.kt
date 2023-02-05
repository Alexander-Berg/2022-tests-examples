package ru.yandex.market.test.deeplink

import android.net.Uri

class DiscoveryAnalogsTestDeeplink(
    private val modelId: String,
) : TestDeeplink() {

    override fun getURI(): Uri {
        val uri = Uri.Builder()
            .scheme("yamarket")
            .authority("product")
            .path("$modelId/pharma_analogs")
        return uri.build()
    }
}
