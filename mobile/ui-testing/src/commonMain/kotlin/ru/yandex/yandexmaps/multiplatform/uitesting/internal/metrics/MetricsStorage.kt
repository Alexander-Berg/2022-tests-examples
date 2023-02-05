package ru.yandex.yandexmaps.multiplatform.uitesting.internal.metrics

import ru.yandex.yandexmaps.multiplatform.uitesting.api.MetricsEvent

internal class MetricsStorage {

    private val events = mutableListOf<MetricsEvent>()

    fun logEvent(event: MetricsEvent) {
        events += event
    }

    fun logEvent(name: String, parameters: Map<String, Any?>) = logEvent(MetricsEvent(name, parameters))

    fun logEvent(name: String, vararg parameters: Pair<String, Any?>) = logEvent(name, parameters.toMap())

    fun getMetricsEvents(): List<MetricsEvent> = events.toList()
}
