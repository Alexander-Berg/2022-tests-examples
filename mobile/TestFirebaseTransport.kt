package ru.yandex.market.di

import android.os.Bundle
import ru.yandex.market.analytics.MetricTransport
import ru.yandex.market.analytics.firebase.FirebaseTransport
import ru.yandex.market.analytics.logger.TransportLogger

class TestFirebaseTransport(
    private val logger: TransportLogger
) : FirebaseTransport {

    override fun sendEvent(eventName: String, params: Bundle) {
        logger.log(MetricTransport.FIREBASE, "", eventName)
    }
}