package ru.yandex.market.analytics.facades

import ru.yandex.market.analytics.AnalyticEvent
import ru.yandex.market.analytics.AnalyticFacade
import ru.yandex.market.analytics.appmetrica.AppMetricaAnalyticService
import ru.yandex.market.utils.Json
import javax.inject.Inject

@AnalyticFacade
class TestingFeaturesAnalyticsFacade @Inject constructor(
    private val appMetrica: AppMetricaAnalyticService,
) {

    @AnalyticEvent
    fun handleCommonDebug(message: String) {
        appMetrica.report("COMMON_DEBUG_EVENT") {
            Json.buildObject {
                "infoMessage" to message
            }
        }
    }

}