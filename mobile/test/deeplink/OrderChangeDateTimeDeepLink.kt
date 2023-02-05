package ru.yandex.market.test.deeplink

import android.net.Uri

data class OrderChangeDateTimeDeepLink(val orderId: Long) : TestDeeplink() {

    override fun getURI(): Uri {
        val uri = Uri.Builder()
            .scheme("yamarket")
            .authority("my")
            .encodedPath("order/$orderId#changeDateTime")
        return uri.build()
    }
}