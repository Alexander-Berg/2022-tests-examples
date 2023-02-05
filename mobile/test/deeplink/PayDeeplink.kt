package ru.yandex.market.test.deeplink

import android.net.Uri

data class PayDeeplink(val orderId: Long) : TestDeeplink() {

    override fun getURI(): Uri {
        val uri = Uri.Builder()
            .scheme("beru")
            .authority("my")
            .path("orders/$orderId/pay")
        return uri.build()
    }
}