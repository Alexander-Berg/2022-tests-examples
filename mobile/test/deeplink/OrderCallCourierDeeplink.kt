package ru.yandex.market.test.deeplink

import android.net.Uri

class OrderCallCourierDeeplink(val orderId: String) : TestDeeplink() {

    override fun getURI(): Uri {
        val uri = Uri.Builder()
            .scheme("yamarket")
            .authority("my")
            .encodedPath("order/$orderId#callCourierAgit")
        return uri.build()
    }
}
