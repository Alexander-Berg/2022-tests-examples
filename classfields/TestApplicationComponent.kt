package com.yandex.mobile.realty.dagger

import com.yandex.mobile.realty.application.ApplicationComponent
import com.yandex.mobile.realty.core.metrica.TestMetricaEventReporter
import com.yandex.mobile.realty.core.webserver.ConnectionConfiguration
import com.yandex.mobile.realty.data.repository.TestSessionStartupService
import com.yandex.mobile.realty.deeplink.DeepLinkModule
import com.yandex.mobile.realty.domain.startup.repository.InstallReferrerRepository
import com.yandex.mobile.realty.module.BuildTypeModule
import com.yandex.mobile.realty.module.BuildVariantModule
import com.yandex.mobile.realty.module.ProductFlavorModule
import dagger.Component
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * @author sorokinandrei on 5/31/21.
 */
@Component(
    modules = [
        BuildTypeModule::class,
        BuildVariantModule::class,
        DeepLinkModule::class,
        FakeApplicationModule::class,
        FakeBindingModule::class,
        FakeGlideConfigurationModule::class,
        FakeNetworkModule::class,
        FakeProvisionModule::class,
        FakeSingletonsModule::class,
        ProductFlavorModule::class,
        FakeVendorModule::class,
    ]
)
@Singleton
interface TestApplicationComponent : ApplicationComponent {

    fun getOkHttpClient(): OkHttpClient

    fun getConnectionConfiguration(): ConnectionConfiguration

    fun getTestSessionStartupService(): TestSessionStartupService

    fun getInstallReferrerRepository(): InstallReferrerRepository

    fun getTestMetricaEventReporter(): TestMetricaEventReporter
}
