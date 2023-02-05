package ru.yandex.yandexnavi.projected.testapp.di

import dagger.Component
import ru.yandex.yandexnavi.projected.platformkit.ProjectedKit
import ru.yandex.yandexnavi.projected.testapp.MainApplication
import ru.yandex.yandexnavi.projected.testapp.ui.MainActivity
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        AppProjectedModule::class,
        AppNavikitModule::class
    ]
)
interface AppComponent : ProjectedKit.Dependencies {
    fun inject(app: MainApplication)
    fun inject(activity: MainActivity)
}
