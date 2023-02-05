package ru.yandex.market

import android.app.Application
import ru.yandex.market.application.MarketApplicationDelegate
import ru.yandex.market.common.toxin.app.scopes.coreScope
import ru.yandex.market.di.toxin.setupToxinAppScope
import ru.yandex.market.pipeline.PipelineBuilder

class NoOpMarketApplicationDelegate(application: Application) : MarketApplicationDelegate(application) {

    override fun setupDi() {
        setupToxinAppScope(
            coreScope = coreScope,
            overridesModule = robolectricTestOverridesModule()
        )
    }

    override fun setupCrashlytics(pipeline: PipelineBuilder<Scenario>) {
        // no-op
    }

    override fun setupYandexMetrica(pipeline: PipelineBuilder<Scenario>) {
        // no-op
    }

    override fun setupAdjust(pipeline: PipelineBuilder<Scenario>) {
        // no-op
    }

    override fun setupTimber(pipeline: PipelineBuilder<Scenario>) {
        // no-op
    }

    override fun setupExpressEntryPoint(pipeline: PipelineBuilder<Scenario>) {
        // no-op
    }

    override fun setupHyperlocal(pipeline: PipelineBuilder<Scenario>) {
        // no-op
    }

}
