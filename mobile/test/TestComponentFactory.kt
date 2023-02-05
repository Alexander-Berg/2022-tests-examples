package ru.yandex.market.test

import ru.yandex.market.clean.presentation.feature.nativepayment.NativePaymentModule
import ru.yandex.market.di.ComponentFactory
import ru.yandex.market.di.module.common.AnalyticsModule
import ru.yandex.market.di.module.common.FrontApiModule
import ru.yandex.market.di.module.common.GooglePayAvailabilityModule
import ru.yandex.market.di.module.common.HttpClientModule
import ru.yandex.market.di.module.common.LocationModule
import ru.yandex.market.di.module.common.MessengerModule
import ru.yandex.market.di.module.common.PostamateClientModule
import ru.yandex.market.di.module.common.SystemModule
import ru.yandex.market.di.module.common.VideoTusModule
import ru.yandex.market.di.module.common.YandexPlayerModule
import ru.yandex.market.di.module.feature.FeatureConfigurationKotlinModule
import ru.yandex.market.di.module.feature.InstalledApplicationManagerModule
import ru.yandex.market.di.module.feature.PlusHomeModule
import ru.yandex.market.gson.GsonFactory
import ru.yandex.market.mocks.State

class TestComponentFactory(
    val states: List<State>
) : ComponentFactory() {

    val moduleComponent by lazy {
        DaggerTestModuleComponent.builder()
            .testOverridesBridge(TestOverridesBridgeComponent())
            .gson(GsonFactory.get())
            .build()
    }

    override fun createPostamateClientModule(): PostamateClientModule {
        return moduleComponent.getMockPostamateClientModule()
    }

    override fun createGooglePayAvailabilityModule(): GooglePayAvailabilityModule {
        return moduleComponent.getMockGooglePayAvailabilityModule()
    }

    override fun createAnalyticsModule(): AnalyticsModule {
        return moduleComponent.getTestAnalyticsModule()
    }

    override fun createHttpClientModule(): HttpClientModule {
        return moduleComponent.getMockHttpClientModule()
    }

    override fun createLocationModule(): LocationModule {
        return moduleComponent.getTestLocationModule()
    }

    override fun createFrontApiModule(): FrontApiModule {
        return moduleComponent.getMockFrontApiModule()
    }

    override fun createPlusHomeModule(): PlusHomeModule {
        return moduleComponent.getMockPlusHomeModule()
    }

    override fun createYandexPlayerModule(): YandexPlayerModule {
        return moduleComponent.getMockYandexPlayerModule()
    }

    override fun createMessengerModule(): MessengerModule {
        return moduleComponent.getMockMessengerModule()
    }

    override fun createVideoTusModule(): VideoTusModule {
        return moduleComponent.getMockVideoTusModule()
    }

    override fun createSystemModule(): SystemModule {
        return moduleComponent.getMockSystemModule()
    }

    override fun createNativePaymentModule(): NativePaymentModule {
        return moduleComponent.getMockNativePaymentModule()
    }

    override fun createFeatureConfigurationKotlinModule(): FeatureConfigurationKotlinModule {
        return moduleComponent.getFeatureTestConfigurationKotlinModule()
    }

    override fun createInstalledApplicationManager(): InstalledApplicationManagerModule {
        return moduleComponent.getMockInstalledApplicationManagerModule()
    }
}