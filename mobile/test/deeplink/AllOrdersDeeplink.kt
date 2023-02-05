package ru.yandex.market.test.deeplink

import android.net.Uri

class AllOrdersDeeplink : TestDeeplink() {

    override fun getURI(): Uri {
        val uri = Uri.Builder()
            .scheme("yamarket")
            .authority("my")
            .encodedPath("orders")
        return uri.build()
    }
}