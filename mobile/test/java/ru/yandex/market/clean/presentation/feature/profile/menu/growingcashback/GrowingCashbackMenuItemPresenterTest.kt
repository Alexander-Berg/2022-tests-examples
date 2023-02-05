package ru.yandex.market.clean.presentation.feature.profile.menu.growingcashback

import io.reactivex.Completable
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analytics.facades.GrowingCashbackAnalytics
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.GrowingCashbackTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.domain.user.model.userProfileTestInstance
import ru.yandex.market.optional.Optional
import ru.yandex.market.presentationSchedulersMock

class GrowingCashbackMenuItemPresenterTest {

    private val userProfile = userProfileTestInstance()
    private val router = mock<Router>()
    private val useCases = mock<GrowingCashbackMenuItemUseCases> {
        on { getCachedUserProfile() } doReturn Observable.just(Optional.of(userProfile))
        on { hideGrowingCashbackCommunication() } doReturn Completable.complete()
    }
    private val analytics = mock<GrowingCashbackAnalytics>()
    private val view = mock<`GrowingCashbackMenuItemView$$State`>()
    private val presenter = GrowingCashbackMenuItemPresenter(
        presentationSchedulersMock(),
        useCases,
        router,
        analytics
    )

    @Before
    fun setup() {
        presenter.setViewState(view)
    }

    @Test
    fun `send visible analytics on first attach`() {
        presenter.attachView(view)
        verify(analytics).profileMenuItemVisible(true, userProfile.hasYandexPlus)
    }


    @Test
    fun `menu item click navigate to growing cashback screen`() {
        presenter.onItemClick()
        verify(router).navigateTo(GrowingCashbackTargetScreen())
    }

    @Test
    fun `close button click save closed `() {
        presenter.onCloseClick()
        verify(useCases).hideGrowingCashbackCommunication()
    }

    @Test
    fun `send navigate analytics on item click`() {
        presenter.onItemClick()
        verify(analytics).profileMenuItemNavigate(true, userProfile.hasYandexPlus)
    }
}