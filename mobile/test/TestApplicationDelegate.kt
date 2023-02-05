package ru.yandex.market.test

import android.app.Application
import ru.yandex.market.application.MarketApplicationDelegate
import ru.yandex.market.common.toxin.app.scopes.coreScope
import ru.yandex.market.di.ComponentFactory
import ru.yandex.market.di.toxin.setupToxinAppScope
import ru.yandex.market.di.toxin.setupToxinCoreScope
import ru.yandex.market.di.toxin.setupToxinLaunchScope
import ru.yandex.market.mocks.State
import ru.yandex.market.pipeline.PipelineBuilder
import ru.yandex.market.pipeline.task
import ru.yandex.market.utils.defensiveCopy

class TestApplicationDelegate(
    application: Application,
    initialStates: List<State>
) : MarketApplicationDelegate(application) {

    private val _initialStates = initialStates.defensiveCopy()
    private val componentFactory by lazy { TestComponentFactory(_initialStates) }
    private val testComponent by lazy { componentFactory.moduleComponent }

    override fun setupCoreDi() {
        setupToxinCoreScope(application = application)
        setupToxinLaunchScope(overridesModule = uiTestOverridesModule(_initialStates))
    }

    override fun setupDi() {
        setupToxinAppScope(
            coreScope = coreScope,
            overridesModule = uiTestOverridesModule(_initialStates)
        )
    }

    override fun createComponentFactory(): ComponentFactory {
        return componentFactory
    }

    override fun setupDebugOverlay(application: Application) {
        // no-op
    }

    override fun setupYandexMetrica(pipeline: PipelineBuilder<Scenario>) {
        // no-op
    }

    override fun setupAdjust(pipeline: PipelineBuilder<Scenario>) {
        // no-op
    }

    override fun setupAppUpdateCheck(pipeline: PipelineBuilder<Scenario>) {
        // no-op
    }

    //синхронное получение экспериментов из стейта
    override fun setupPrefetchExperiments(pipeline: PipelineBuilder<Scenario>) {
        pipeline.task(Scenario.FINALLY_LAUNCH_ASYNC, "get start config for test") {
            getApplicationComponent().experimentConfigServiceFactory()
                .experimentConfigServiceInstance.getConfigs()
        }
    }

    fun updateStates(states: List<State>) {
        testComponent.getStateFacade().updateStates(states)
    }
}
