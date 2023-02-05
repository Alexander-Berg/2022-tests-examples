package ru.yandex.market.test.deeplink

import android.net.Uri

data class QuestionTestDeeplink(
    private val productId: String
) : TestDeeplink() {
    override fun getURI(): Uri {
        val uri = Uri.Builder()
            .scheme("beru")
            .authority("questions")
            .path(productId)
        return uri.build()
    }
}