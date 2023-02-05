package ru.yandex.market.clean.presentation.feature.profile.promo.helpisnear

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.activity.web.MarketWebActivityArguments
import ru.yandex.market.activity.web.MarketWebActivityTargetScreen
import ru.yandex.market.analytics.facades.HelpIsNearAnalytics
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.presentationSchedulersMock

class ProfileHelpIsNearPresenterTest {

    private val needShowHelpIsNearNewsSubject = PublishSubject.create<Boolean>()
    private val landingLink = "https://help.yandex.ru/mobile-constructor?platform=market_android"

    private val useCases = mock<ProfileHelpIsNearUseCases> {
        on { needShowHelpIsNearNewsBadgeUseCase() } doReturn needShowHelpIsNearNewsSubject
        on { setHelpIsNearLandingWasShown() } doReturn Completable.complete()
        on { getHelpIsNearLandingLink(any()) } doReturn Single.just(landingLink)
    }

    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.PROFILE
    }
    private val analytics = mock<HelpIsNearAnalytics>()
    private val viewState = mock<ProfileHelpIsNearView>()

    private val presenter = ProfileHelpIsNearPresenter(
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
    fun `send item shown event with subscribed status on help is near item shown `() {
        presenter.onShown(true)
        verify(analytics).profileItemShown(true)
    }

    @Test
    fun `send item shown event without subscribed status on help is near item shown`() {
        presenter.onShown(false)
        verify(analytics).profileItemShown(false)
    }

    @Test
    fun `show news badge if has news`() {
        needShowHelpIsNearNewsSubject.onNext(true)

        verify(viewState).setNewsBadgeVisible(true)
    }

    @Test
    fun `hide news badge if has no news`() {
        needShowHelpIsNearNewsSubject.onNext(false)

        verify(viewState).setNewsBadgeVisible(false)
    }

    @Test
    fun `hide news badge if got error on news stream`() {
        needShowHelpIsNearNewsSubject.onError(Error())

        verify(viewState).setNewsBadgeVisible(false)
    }

    @Test
    fun `save onboarding was shown on action click`() {
        presenter.onClick(true)

        verify(useCases).setHelpIsNearLandingWasShown()
    }

    @Test
    fun `navigate to landing action click`() {
        presenter.onClick(true)

        verify(router).navigateTo(
            MarketWebActivityTargetScreen(
                MarketWebActivityArguments.builder()
                    .link(landingLink)
                    .isShowLoadedTitle(true)
                    .isUrlOverridingEnabled(true)
                    .allowAddQueryParameters(false)
                    .build()
            )
        )
    }

    @Test
    fun `send profile item navigate event on action click with subscribed status`() {
        presenter.onClick(true)

        verify(analytics).profileItemNavigate(true)
    }

    @Test
    fun `send profile item navigate event on action click without subscribed status`() {
        presenter.onClick(false)

        verify(analytics).profileItemNavigate(false)
    }
}
