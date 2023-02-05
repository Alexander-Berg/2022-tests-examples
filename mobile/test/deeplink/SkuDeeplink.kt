package ru.yandex.market.test.deeplink

import android.net.Uri
import ru.yandex.market.mocks.generateStringId

data class SkuDeeplink(
    private val skuId: String,
    private val hid: String
) : TestDeeplink() {

    override fun getURI(): Uri {
        val uri = Uri.Builder()
            .scheme("beru")
            .authority("product")
            .path("$hid/$skuId")
        return uri.build()
    }

    companion object {

        fun withoutHid(skuId: String) = SkuDeeplink(skuId = skuId, hid = generateStringId())

    }
}