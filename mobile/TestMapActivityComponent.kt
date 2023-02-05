package ru.yandex.yandexmaps.app.di.components

import dagger.BindsInstance
import dagger.Subcomponent
import ru.yandex.yandexmaps.app.MapActivity
import ru.yandex.yandexmaps.app.di.modules.aggregation.MapActivityCommonModulesModule
import ru.yandex.yandexmaps.app.di.modules.aggregation.MapActivityMockedModulesModule
import ru.yandex.yandexmaps.app.di.modules.aggregation.MapActivityTestOnlyModulesModule
import ru.yandex.yandexmaps.app.di.scopes.MapActivityScope
import ru.yandex.yandexmaps.business.common.mapkit.entrances.EntrancesScope
import ru.yandex.yandexmaps.integrations.overlays.di.OverlaysScope
import ru.yandex.yandexmaps.intro.coordinator.lifecycle.ColdStartAwareImpl
import ru.yandex.yandexmaps.multiplatform.uitesting.api.AssertionProvider
import ru.yandex.yandexmaps.multiplatform.uitesting.reporter.api.TestRunnerReporter
import ru.yandex.yandexmaps.probator.model.TestingApplication
import ru.yandex.yandexmaps.reviews.create.di.CreateReviewScope
import ru.yandex.yandexmaps.routes.redux.RoutesActivityScope
import ru.yandex.yandexmaps.routes.redux.RoutesReduxModule
import ru.yandex.yandexmaps.utils.capture.ScreenshotCapturerImpl
import ru.yandex.yandexmaps.utils.capture.VideoCapturerImpl
import javax.inject.Provider

@MapActivityScope
@EntrancesScope
@RoutesActivityScope
@CreateReviewScope
@OverlaysScope
@Subcomponent(
    modules = [
        MapActivityCommonModulesModule::class,
        MapActivityMockedModulesModule::class,
        MapActivityTestOnlyModulesModule::class,
    ]
)
interface TestMapActivityComponent : MapActivityComponent {

    @Subcomponent.Factory
    interface Factory : MapActivityComponent.Factory {
        override fun create(
            routesReduxModule: RoutesReduxModule,
            @BindsInstance activity: MapActivity,
            @BindsInstance coldStartAwareImpl: ColdStartAwareImpl
        ): TestMapActivityComponent
    }

    val allureReporter: TestRunnerReporter
    val assertionProvider: AssertionProvider
    val screenshotCapturerImpl: ScreenshotCapturerImpl
    val videoCapturerImpl: VideoCapturerImpl
    val testingAppProvider: Provider<TestingApplication>
}
