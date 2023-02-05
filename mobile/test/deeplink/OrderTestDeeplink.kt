package ru.yandex.market.test.deeplink

import android.net.Uri

class OrderTestDeeplink(val orderId: String) : TestDeeplink() {

    override fun getURI(): Uri {
        val uri = Uri.Builder()
            .scheme("yamarket")
            .authority("my")
            .encodedPath("order/$orderId")
        return uri.build()
    }
}