package ru.yandex.market.test

import com.google.gson.Gson
import dagger.BindsInstance
import dagger.Component
import ru.yandex.market.di.FeatureTestConfigurationKotlinModule
import ru.yandex.market.di.module.MockFrontApiModule
import ru.yandex.market.di.module.MockGooglePayAvailabilityModule
import ru.yandex.market.di.module.MockHttpClientModule
import ru.yandex.market.di.module.MockMessengerModule
import ru.yandex.market.di.module.MockPostamateClientModule
import ru.yandex.market.di.module.MockSystemModule
import ru.yandex.market.di.module.MockVideoTusModule
import ru.yandex.market.di.module.MockYandexPlayerModule
import ru.yandex.market.di.module.TestAnalyticsModule
import ru.yandex.market.di.module.TestInstalledApplicationManagerModule
import ru.yandex.market.di.module.TestLocationModule
import ru.yandex.market.di.module.TestNativePaymentModule
import ru.yandex.market.di.module.TestPlusHomeModule
import ru.yandex.market.mocks.StateFacade
import javax.inject.Singleton

@Singleton
@Component(
    dependencies = [
        TestOverridesBridge::class
    ]
)
interface TestModuleComponent {

    fun getMockPostamateClientModule(): MockPostamateClientModule

    fun getMockGooglePayAvailabilityModule(): MockGooglePayAvailabilityModule

    fun getTestAnalyticsModule(): TestAnalyticsModule

    fun getMockHttpClientModule(): MockHttpClientModule

    fun getMockFrontApiModule(): MockFrontApiModule

    fun getTestLocationModule(): TestLocationModule

    fun getStateFacade(): StateFacade

    fun getMockPlusHomeModule(): TestPlusHomeModule

    fun getMockInstalledApplicationManagerModule(): TestInstalledApplicationManagerModule

    fun getMockYandexPlayerModule(): MockYandexPlayerModule

    fun getMockMessengerModule(): MockMessengerModule

    fun getMockVideoTusModule(): MockVideoTusModule

    fun getMockSystemModule(): MockSystemModule

    fun getMockNativePaymentModule(): TestNativePaymentModule

    fun getFeatureTestConfigurationKotlinModule(): FeatureTestConfigurationKotlinModule

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun gson(gson: Gson): Builder

        fun testOverridesBridge(bridge: TestOverridesBridge): Builder

        fun build(): TestModuleComponent
    }

}