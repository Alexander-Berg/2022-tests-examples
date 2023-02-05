package ru.yandex.market.di

import com.adjust.sdk.AdjustEvent
import ru.yandex.market.analytics.MetricTransport
import ru.yandex.market.analytics.adjust.AdjustTransport
import ru.yandex.market.analytics.logger.TransportLogger

class TestAdjustTransport(
    private val logger: TransportLogger
) : AdjustTransport {

    override fun sendEvent(eventName: String, params: Map<String, *>, valueToSum: Double?) {
        logger.log(MetricTransport.ADJUST, "", eventName)
    }

    override fun sendEvent(event: AdjustEvent, eventName: String, params: Map<String, *>, valueToSum: Double?) {
        logger.log(MetricTransport.ADJUST, "", eventName)
    }

    override fun init() = Unit

    override fun setCustomerIdIntoCriteoEvents(customerId: String) = Unit

    override fun setHashedEmailIntoCriteoEvents(hashedEmail: String) = Unit

    override fun sendPushToken(token: String) = Unit

    override fun sendOnResume() = Unit

    override fun sendOnPause() = Unit
}