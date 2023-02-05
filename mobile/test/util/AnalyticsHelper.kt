package ru.yandex.market.test.util

import ru.yandex.market.Dependencies
import ru.yandex.market.analitycs.events.AnalyticsEvent
import ru.yandex.market.analitycs.events.health.HealthEvent
import ru.yandex.market.analytics.health.HealthName
import ru.yandex.market.analytics.health.HealthLevel
import ru.yandex.market.analytics.health.MetricaTransport
import ru.yandex.market.analytics.logger.TransportLogger
import ru.yandex.market.common.toxin.app.scopes.appScope
import ru.yandex.market.di.TestAnalyticsService
import ru.yandex.market.di.TestMetricaTransport
import ru.yandex.market.di.TestTransportLogger
import toxin.Component

object AnalyticsHelper {

    fun checkHealthEvent(
        name: HealthName
    ) = checkHealthEvent(name.name)

    fun checkHealthEvent(
        name: String
    ) {
        val transport = HelperComponent().getMetricaTransport() as TestMetricaTransport
        if (!transport.events.contains(name)) {
            throw RuntimeException("Didn't find events with name $name")
        }
    }

    fun checkNoHealthEvent(
        name: HealthName,
        level: HealthLevel = HealthLevel.ERROR
    ) {
        checkNoEvent<HealthEvent> {
            it.name == name &&
                    it.level == level
        }
    }

    fun checkImportantMetric(eventName: String) {
        (HelperComponent().getTransportLogger() as TestTransportLogger).checkEvent(eventName)
    }

    inline fun <reified T : AnalyticsEvent> checkAnalyticsEvent(predicate: (T) -> Boolean) {
        val service = Dependencies.getAnalyticsService() as TestAnalyticsService
        service.events.filterIsInstance<T>()
            .find { predicate.invoke(it) }
            ?: throw RuntimeException("Didn't find events with type ${T::class.java} for predicate")
    }

    inline fun <reified T : AnalyticsEvent> checkAnalyticsEvent(
        filter: (T) -> Boolean,
        predicate: (T) -> Boolean
    ) {
        val service = Dependencies.getAnalyticsService() as TestAnalyticsService
        service.events.filterIsInstance<T>()
            .filter { filter.invoke(it) }
            .find { predicate.invoke(it) }
            ?: throw RuntimeException("Didn't find events with type ${T::class.java} for predicate")
    }

    fun assertImportantAnalyticsChecked() {
        (HelperComponent().getTransportLogger() as TestTransportLogger).assertAllImportantEventsChecked()
    }

    private inline fun <reified R> checkNoEvent(predicate: (R) -> Boolean) {
        val service = Dependencies.getAnalyticsService() as TestAnalyticsService
        val event = service.events.filterIsInstance<R>()
            .find { predicate.invoke(it) }
        if (event != null) {
            throw RuntimeException("Found event with type ${R::class.java} for predicate")
        }
    }

    private class HelperComponent : Component(appScope) {

        fun getMetricaTransport(): MetricaTransport = auto()
        fun getTransportLogger(): TransportLogger = auto()
    }
}