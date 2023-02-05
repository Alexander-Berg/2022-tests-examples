package ru.yandex.market.clean.data.request.access

import android.content.Context
import ru.yandex.market.net.ContentApiVersion
import ru.yandex.market.base.network.common.Method
import ru.yandex.market.net.Request
import ru.yandex.market.net.parsers.DevNull

class TestContentApiAccessRequest(
    context: Context
) : Request<Void>(
    context,
    DevNull(),
    PATH,
    ContentApiVersion.VERSION__2_X_X
) {

    init {
        mAppendUuid = true
        mCacheEnabled = false
        mAppendAuthParams = true
        mForceMarketUid = true
    }

    override fun getResponseClass() = Void::class.java

    override fun getRequestedMethod() = Method.GET

    companion object {
        private const val PATH = "user/subscriptions"
    }
}