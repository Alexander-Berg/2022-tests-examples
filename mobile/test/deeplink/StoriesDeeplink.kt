package ru.yandex.market.test.deeplink

import android.net.Uri

class StoriesDeeplink(
    private val pageId: String,
) : TestDeeplink() {

    override fun getURI(): Uri {
        val uri = Uri.Builder()
            .scheme("yamarket")
            .authority("stories")
            .path(pageId)
        return uri.build()
    }
}