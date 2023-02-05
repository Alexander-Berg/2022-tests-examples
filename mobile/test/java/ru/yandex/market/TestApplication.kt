package ru.yandex.market

import android.content.Context
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import ru.yandex.market.di.TestAndroidModule
import ru.yandex.market.di.TestPreferencesModule
import ru.yandex.market.utils.withLocale
import ru.yandex.market.utils.LocaleUtils

class TestApplication : DaggerApplication() {

    var currentDateTime
        get() = component.dateTimeProvider().currentDateTime
        set(value) {
            (component.dateTimeProvider() as UnitTestDateTimeProvider).currentDate = value
        }

    lateinit var component: TestComponent

    override fun applicationInjector(): AndroidInjector<out DaggerApplication>? {
        component = DaggerTestComponent.builder()
            .testAndroidModule(TestAndroidModule(this))
            .testPreferencesModule(TestPreferencesModule(this))
            .build()
        return component
    }

    override fun onCreate() {
        instance = this
        super.onCreate()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withLocale(LocaleUtils.russian()))
    }

    companion object {

        @JvmStatic
        lateinit var instance: TestApplication
            private set
    }
}
