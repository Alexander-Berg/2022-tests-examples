package ru.yandex.market.test.deeplink

import android.net.Uri

class SmartCoinsDeepLink : TestDeeplink() {

    override fun getURI(): Uri {
        val uri = Uri.Builder()
            .scheme("beru")
            .authority("bonus")
        return uri.build()
    }
}