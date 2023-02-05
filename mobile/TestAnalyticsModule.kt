package ru.yandex.market.di.module

import dagger.Lazy
import ru.yandex.market.analitycs.adjust.AdjustAnalyticsService
import ru.yandex.market.analitycs.adwords.AdWordsAnalyticsService
import ru.yandex.market.analitycs.appmetrica.AppMetricaAnalyticsService
import ru.yandex.market.analitycs.firebase.FirebaseAnalyticsService
import ru.yandex.market.analitycs.health.HealthAnalyticsService
import ru.yandex.market.analytics.AnalyticsDispatcher
import ru.yandex.market.di.TestAnalyticsService
import ru.yandex.market.di.module.common.AnalyticsModule
import javax.inject.Inject

class TestAnalyticsModule @Inject constructor() : AnalyticsModule() {

    override fun provideAnalyticsService(
        analyticsDispatcher: AnalyticsDispatcher,
        appMetricaAnalyticsService: Lazy<AppMetricaAnalyticsService>,
        adWordsAnalyticsService: Lazy<AdWordsAnalyticsService>,
        healthAnalyticsService: Lazy<HealthAnalyticsService>,
        adjustAnalyticsService: Lazy<AdjustAnalyticsService>,
        firebaseAnalyticsService: Lazy<FirebaseAnalyticsService>
    ): @Suppress("DEPRECATION") ru.yandex.market.analitycs.AnalyticsService {
        return TestAnalyticsService(
            listOf(adjustAnalyticsService.get(), firebaseAnalyticsService.get())
        )
    }
}
