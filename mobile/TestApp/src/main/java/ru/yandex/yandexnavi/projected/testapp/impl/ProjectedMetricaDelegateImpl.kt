package ru.yandex.yandexnavi.projected.testapp.impl

import ru.yandex.yandexnavi.analytics.Analytics
import ru.yandex.yandexnavi.projected.platformkit.domain.repo.metrica.ProjectedMetricaDelegate

class ProjectedMetricaDelegateImpl : ProjectedMetricaDelegate {

    override fun event(name: String, map: Map<String, Any?>?) {
        Analytics.report(name, map)
    }

    override fun error(name: String, e: Throwable?) {
        Analytics.report(name, mapOf(PARAM_ERROR to e))
    }
}

private const val PARAM_ERROR = "error"
