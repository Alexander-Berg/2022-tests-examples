package ru.yandex.market.activity.main

import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Observable
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.activity.ForceUpdateTargetScreen
import ru.yandex.market.common.startapp.StartAppMetricaSender
import ru.yandex.market.analytics.facades.VacanciesAnalytics
import ru.yandex.market.clean.presentation.feature.cms.HomeTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.presentationSchedulersMock

class ApplicationConductorPresenterTest {

    private val useCases = mock<ApplicationConductorUseCases> {
        on { sendThirdPartyAppsAnalytics() } doReturn Completable.complete()
        on { sendPushNotificationsSubscriptionState() } doReturn Completable.complete()
    }

    private val schedulers = presentationSchedulersMock()

    private val startAppMetricaSender = mock<StartAppMetricaSender>()

    private val router = mock<Router>()

    private val vacanciesAnalytics = mock<Lazy<VacanciesAnalytics>>()


    private val presenter = ApplicationConductorPresenter(
        useCases = useCases,
        schedulers = schedulers,
        startAppMetricaSender = startAppMetricaSender,
        router = router,
        vacanciesAnalytics = vacanciesAnalytics
    )

    private val view = mock<ApplicationConductorView>()

    @Test
    fun `Presenter starts on boarding on attach`() {
        whenever(useCases.waitForExperiments()) doReturn Completable.complete()
        whenever(useCases.getForceUpdateStream()) doReturn Observable.just(false)

        presenter.attachView(view)
        presenter.onFinishOnBoarding()

        verify(view, times(1)).startOnBoarding()
    }

    @Test
    fun `Presenter starts on boarding on first attach only`() {
        whenever(useCases.waitForExperiments()) doReturn Completable.complete()
        whenever(useCases.getForceUpdateStream()) doReturn Observable.just(false)

        presenter.attachView(view)
        presenter.detachView(view)
        presenter.attachView(view)
        presenter.onFinishOnBoarding()

        verify(view, times(1)).startOnBoarding()
    }

    @Test
    fun `Presenter navigates to force update screen if need`() {
        whenever(useCases.waitForExperiments()) doReturn Completable.complete()
        whenever(useCases.getForceUpdateStream()) doReturn Observable.just(true)

        presenter.attachView(view)

        verify(router, times(1)).navigateTo(ForceUpdateTargetScreen())
    }

    @Test
    fun `Presenter calls 'view performInitialNavigation' method when on boarding finishes`() {
        whenever(useCases.waitForExperiments()) doReturn Completable.complete()
        whenever(useCases.getForceUpdateStream()) doReturn Observable.just(true)

        presenter.attachView(view)
        presenter.onFinishOnBoarding()

        verify(view, times(1)).performInitialNavigation()
    }

    @Test
    fun `Presenter navigates to home screen when requested`() {
        whenever(useCases.waitForExperiments()) doReturn Completable.complete()
        whenever(useCases.getForceUpdateStream()) doReturn Observable.just(true)

        presenter.attachView(view)
        presenter.openMainScreen()

        verify(router, times(1)).navigateTo(HomeTargetScreen())
    }

    @Test
    fun `Presenter calls 'onSplashDetached' if update is required`() {
        whenever(useCases.waitForExperiments()) doReturn Completable.complete()
        whenever(useCases.getForceUpdateStream()) doReturn Observable.just(true)

        presenter.attachView(view)

        verify(startAppMetricaSender, times(1)).onOnboardingStarted()
    }

}
