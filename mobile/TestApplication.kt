package ru.yandex.market.uikitapp

import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import timber.log.Timber

class TestApplication : DaggerApplication() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationComponent.create()
    }
}