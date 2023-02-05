package ru.yandex.market.clean.presentation.feature.helpisnear

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import ru.yandex.market.activity.web.MarketWebActivityArguments
import ru.yandex.market.activity.web.MarketWebActivityTargetScreen
import ru.yandex.market.analytics.facades.HelpIsNearAnalytics
import ru.yandex.market.clean.domain.model.HelpIsNearSubscriptionStatus
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.presentationSchedulersMock

class HelpIsNearOnboardingPresenterTest {

    private val helpIsNearStatusStreamSubject = PublishSubject.create<HelpIsNearSubscriptionStatus>()
    private val landingLink = "https://help.yandex.ru/mobile-constructor?platform=market_android"

    private val useCases = mock<HelpIsNearOnboardingUseCases> {
        on { setHelpIsNearLandingWasShown() } doReturn Completable.complete()
        on { getHelpIsNearStatusStream() } doReturn helpIsNearStatusStreamSubject
        on { getHelpIsNearLandingLink(any()) } doReturn Single.just(landingLink)
    }

    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.HELP_IS_NEAR_ONBOARDING
    }
    private val analytics = mock<HelpIsNearAnalytics>()
    private val viewState = mock<HelpIsNearOnboardingView>()

    private val presenter = HelpIsNearOnboardingPresenter(
        presentationSchedulersMock(),
        useCases,
        router,
        analytics
    )

    @Before
    fun setup() {
        presenter.attachView(viewState)
    }

    @Test
    fun `send onboarding shown analytics for subscribed status`() {
        helpIsNearStatusStreamSubject.onNext(
            HelpIsNearSubscriptionStatus(
                isSubscribed = true,
                donatedTotal = Money.zeroRub()
            )
        )

        verify(analytics).onboardingShown(true)
    }

    @Test
    fun `send onboarding shown analytics for not subscribed status`() {
        helpIsNearStatusStreamSubject.onNext(HelpIsNearSubscriptionStatus.NOT_SUBSCRIBED)

        verify(analytics).onboardingShown(false)
    }

    @Test
    fun `send onboarding shown analytics for not subscribed status is was error on status receive`() {
        helpIsNearStatusStreamSubject.onError(Error())

        verify(analytics).onboardingShown(false)
    }

    @Test
    fun `navigate to landing on action click`() {
        presenter.onActionButtonClick()

        verify(router).navigateTo(
            MarketWebActivityTargetScreen(
                MarketWebActivityArguments.builder()
                    .link(landingLink)
                    .isShowLoadedTitle(true)
                    .isUrlOverridingEnabled(true)
                    .build()
            )
        )
    }

    @Test
    fun `close onboarding after action click`() {
        presenter.onActionButtonClick()

        verify(viewState).closeSelf()
    }

    @Test
    fun `save onboarding wash shown on action click`() {
        presenter.onActionButtonClick()

        verify(useCases).setHelpIsNearLandingWasShown()
    }

    @Test
    fun `send navigate analytics for subscribed user on action click`() {
        presenter.onActionButtonClick()
        helpIsNearStatusStreamSubject.onNext(
            HelpIsNearSubscriptionStatus(
                isSubscribed = true,
                donatedTotal = Money.zeroRub()
            )
        )

        verify(analytics).onboardingNavigate(true)
    }

    @Test
    fun `send navigate analytics for not subscribed user on action click`() {
        presenter.onActionButtonClick()
        helpIsNearStatusStreamSubject.onNext(HelpIsNearSubscriptionStatus.NOT_SUBSCRIBED)

        verify(analytics).onboardingNavigate(false)
    }

    @Test
    fun `send dismiss analytics for subscribed user on dismiss click`() {
        presenter.onDismiss()
        helpIsNearStatusStreamSubject.onNext(
            HelpIsNearSubscriptionStatus(
                isSubscribed = true,
                donatedTotal = Money.zeroRub()
            )
        )

        verify(analytics).onboardingDismiss(true)
    }

    @Test
    fun `send dismiss analytics for not subscribed user on dismiss click`() {
        presenter.onDismiss()
        helpIsNearStatusStreamSubject.onNext(HelpIsNearSubscriptionStatus.NOT_SUBSCRIBED)

        verify(analytics).onboardingDismiss(false)
    }

    @Test
    fun `Do not send dismiss analytics if receive dismiss action after navigate action click`() {
        presenter.onActionButtonClick()
        presenter.onDismiss()
        helpIsNearStatusStreamSubject.onNext(HelpIsNearSubscriptionStatus.NOT_SUBSCRIBED)

        verify(analytics, never()).onboardingDismiss(any())
    }
}

