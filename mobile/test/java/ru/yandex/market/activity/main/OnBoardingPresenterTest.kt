package ru.yandex.market.activity.main

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.common.startapp.StartAppMetricaSender
import ru.yandex.market.analytics.facades.OnboardingAnalytics
import ru.yandex.market.clean.domain.model.AutoDetectedRegion
import ru.yandex.market.domain.onboarding.model.OnboardingState
import ru.yandex.market.clean.domain.model.lavka2.LavkaOnboardingInfo
import ru.yandex.market.clean.domain.model.lavka2.ResultTryStartPopup
import ru.yandex.market.clean.presentation.feature.onboarding.flow.OnBoardingFlowTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.common.experiments.manager.ExperimentManager
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.safe.Safe

class OnBoardingPresenterTest {

    private val useCases = mock<OnBoardingUseCases>() {
        on { getFakeStoriesPageLink() } doReturn Maybe.empty()
        on { getAutoDetectedRegion() } doReturn Single.just(AutoDetectedRegion())
        on { enablePushNotifications() } doReturn Completable.complete()
        on { isFeatureConfigsLoaded() } doReturn Single.just(true)
        on { setOnboardingWasShown() } doReturn Completable.complete()
        on { isRegionConfirmed() } doReturn Single.just(false)
    }

    private val schedulers = presentationSchedulersMock()

    private val router = mock<Router>()

    private val startAppMetricaSender = mock<StartAppMetricaSender>()

    private val experimentManager = mock<ExperimentManager>()

    private val onboardingAnalytics = mock<OnboardingAnalytics>()

    private val presenter = OnBoardingPresenter(
        useCases = useCases,
        schedulers = schedulers,
        router = router,
        startAppMetricaSender = startAppMetricaSender,
        experimentManager = experimentManager,
        onboardingAnalytics = onboardingAnalytics,
    )

    private val view = mock<OnBoardingView>()
    private val lavkaOnboardingInfo = mock<Safe<LavkaOnboardingInfo>>()
    private val resultTryStartPopup = ResultTryStartPopup.GetStoriesPageAndStartOnBoarding

    @Test
    fun `Onboarding enabled and was shown`() {
        whenever(useCases.getOnboardingState()) doReturn Single.just(
            OnboardingState.OnboardingWasShownOrSkipped(
                isFirstLaunch = false,
                wasShown = true
            )
        )
        whenever(useCases.getLavkaStartOnboarding()) doReturn Single.just(lavkaOnboardingInfo)
        whenever(useCases.getLavkaStartPopupIfLoggedIn()) doReturn Single.just(resultTryStartPopup)

        presenter.attachView(view)
        presenter.onStartOnBoarding()

        verify(view, times(1)).finishOnBoarding()
    }

    @Test
    fun `Onboarding enabled and was not shown`() {
        whenever(useCases.getOnboardingState()) doReturn Single.just(
            OnboardingState.ShowWelcomeOnboarding(
                isFirstLaunch = false,
            )
        )
        whenever(useCases.getLavkaStartOnboarding()) doReturn Single.just(lavkaOnboardingInfo)
        whenever(useCases.getLavkaStartPopupIfLoggedIn()) doReturn Single.just(resultTryStartPopup)

        presenter.attachView(view)
        presenter.onStartOnBoarding()

        verify(router, times(1)).navigateTo(isA<OnBoardingFlowTargetScreen>())
    }

    @Test
    fun `Onboarding disabled`() {
        whenever(useCases.getOnboardingState()) doReturn Single.just(
            OnboardingState.OnboardingWasShownOrSkipped(
                isFirstLaunch = false,
                wasShown = false
            )
        )
        whenever(useCases.getLavkaStartOnboarding()) doReturn Single.just(lavkaOnboardingInfo)
        whenever(useCases.getLavkaStartPopupIfLoggedIn()) doReturn Single.just(resultTryStartPopup)

        presenter.attachView(view)
        presenter.onStartOnBoarding()
        verify(view, times(1)).finishOnBoarding()
    }

    @Test
    fun `Finishes on boarding if failed to check region confirmation`() {
        whenever(useCases.getOnboardingState()) doReturn Single.just(
            OnboardingState.OnboardingWasShownOrSkipped(
                isFirstLaunch = false,
                wasShown = false
            )
        )
        whenever(useCases.isRegionConfirmed()) doReturn Single.error(RuntimeException())

        presenter.attachView(view)
        presenter.onStartOnBoarding()

        verify(view, times(1)).finishOnBoarding()
    }

    @Test
    fun `Presenter calls 'onSplashDetached' is region not confirmed`() {
        whenever(useCases.getOnboardingState()) doReturn Single.just(
            OnboardingState.ShowWelcomeOnboarding(
                isFirstLaunch = false,
            )
        )

        presenter.attachView(view)
        presenter.onStartOnBoarding()

        verify(startAppMetricaSender, times(1)).onOnboardingStarted()
    }

}
