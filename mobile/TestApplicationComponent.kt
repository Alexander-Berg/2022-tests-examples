package ru.yandex.yandexmaps.app.di.components

import dagger.BindsInstance
import dagger.Component
import okhttp3.OkHttpClient
import ru.yandex.maps.appkit.common.PreferenceStorage
import ru.yandex.yandexmaps.app.MapsApplication
import ru.yandex.yandexmaps.app.di.modules.CategoriesScope
import ru.yandex.yandexmaps.app.di.modules.InitialExperiments
import ru.yandex.yandexmaps.app.di.modules.aggregation.ApplicationCommonModulesModule
import ru.yandex.yandexmaps.app.di.modules.aggregation.ApplicationMockedModulesModule
import ru.yandex.yandexmaps.app.di.modules.aggregation.ApplicationTestOnlyModulesModule
import ru.yandex.yandexmaps.app.di.modules.network.RawOkHttpClient
import ru.yandex.yandexmaps.cabinet.CabinetRanksScope
import ru.yandex.yandexmaps.common.utils.rx.ImmediateMainThreadScheduler
import ru.yandex.yandexmaps.probator.runner.ProbatorRunStorage
import ru.yandex.yandexmaps.probator.runner.ProbatorTestRunner
import ru.yandex.yandexmaps.reviews.ugc.di.UgcReviewsScope
import javax.inject.Singleton

@Singleton
@UgcReviewsScope
@CabinetRanksScope
@ImmediateMainThreadScheduler.Scope
@CategoriesScope
@Component(
    modules = [
        ApplicationMockedModulesModule::class,
        ApplicationTestOnlyModulesModule::class,
        ApplicationCommonModulesModule::class,
    ]
)
interface TestApplicationComponent : ApplicationComponent {

    @Component.Builder
    interface Builder : ApplicationComponent.Builder {

        @BindsInstance
        override fun bindApplication(application: MapsApplication): Builder

        @BindsInstance
        override fun bindPreferences(preferences: PreferenceStorage): Builder

        fun initialExperiments(@BindsInstance @InitialExperiments initialExperiments: Map<String, String?>): Builder

        override fun build(): TestApplicationComponent
    }

    override fun mapActivityComponentFactory(): TestMapActivityComponent.Factory

    fun inject(probatorRunner: ProbatorTestRunner)

    @RawOkHttpClient
    fun rawOkHttpClient(): OkHttpClient

    val probatorRunStorage: ProbatorRunStorage
}
