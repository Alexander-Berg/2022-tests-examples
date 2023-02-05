package ru.yandex.market.di

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import dagger.Module
import dagger.Provides
import dagger.Reusable
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.common.android.ResourcesManagerImpl

@Module
class TestAndroidModule(private val application: Application) {

    @Provides
    fun context(): Context = application

    @Provides
    fun application(): Application = application

    @Provides
    fun resources(): Resources = application.resources

    @Provides
    fun assets(): AssetManager = application.assets

    @Provides
    @Reusable
    fun provideResourcesManager(resources: Resources): ResourcesManager {
        return ResourcesManagerImpl(resources)
    }
}