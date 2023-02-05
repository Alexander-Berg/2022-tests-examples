package ru.yandex.market.test.deeplink

import android.net.Uri

class EfimDeepLink : TestDeeplink() {

    override fun getURI(): Uri {
        val uri = Uri.Builder()
            .scheme("beru")
            .authority("special")
            .path("bonusy")
        return uri.build()
    }
}