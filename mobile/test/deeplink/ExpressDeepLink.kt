package ru.yandex.market.test.deeplink

import android.net.Uri

class ExpressDeepLink : TestDeeplink() {

    override fun getURI(): Uri {
        val uri = Uri.Builder()
            .scheme("yamarket")
            .authority("express")
        return uri.build()
    }
}