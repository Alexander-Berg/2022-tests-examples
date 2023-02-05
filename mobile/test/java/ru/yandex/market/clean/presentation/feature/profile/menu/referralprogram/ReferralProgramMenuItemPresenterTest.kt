package ru.yandex.market.clean.presentation.feature.profile.menu.referralprogram

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analytics.facades.ReferralProgramAnalytics
import ru.yandex.market.clean.domain.model.referralprogram.ReferralProgramUserDescription
import ru.yandex.market.clean.domain.model.referralprogram.referralProgramStatus_EnabledTestInstance
import ru.yandex.market.clean.presentation.feature.referralprogram.ReferralProgramTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.presentationSchedulersMock

class ReferralProgramMenuItemPresenterTest {

    private val referralProgramStatus = referralProgramStatus_EnabledTestInstance()
    private val initialVo = mock<ReferralProgramMenuVo>()
    private val stateVo = mock<ReferralProgramMenuVo>()
    private val userDescription = mock<ReferralProgramUserDescription>()
    private val useCases = mock<ReferralProgramProfileMenuItemUseCases> {
        on { getReferralProgramStatus() } doReturn Observable.just(referralProgramStatus)
        on { getReferralProgramUserDescription() } doReturn Single.just(userDescription)
        on { needShowReferralProgramNewsBadgeUseCase() } doReturn Observable.just(false)
        on { setReferralProgramWasShownUseCase() } doReturn Completable.complete()
    }
    private val formatter = mock<ProfileReferralProgramMenuItemFormatter> {
        on { formatSimple() } doReturn initialVo
        on { format(referralProgramStatus) } doReturn stateVo
    }

    private val router = mock<Router>()
    private val viewState = mock<`ReferralProgramMenuItemView$$State`>()
    private val analytics = mock<ReferralProgramAnalytics>()
    private val presenter = ReferralProgramMenuItemPresenter(
        presentationSchedulersMock(),
        useCases,
        router,
        formatter,
        analytics
    )

    @Before
    fun setup() {
        presenter.setViewState(viewState)
    }

    @Test
    fun `Show initial data attach`() {
        presenter.attachView(viewState)

        verify(viewState).showContent(initialVo)
    }

    @Test
    fun `Show referral program state on loaded `() {
        presenter.attachView(viewState)

        verify(viewState).showContent(stateVo)
    }

    @Test
    fun `Show initial data on error when load info`() {
        whenever(useCases.getReferralProgramStatus()) doReturn Observable.error(Error())
        whenever(useCases.needShowReferralProgramNewsBadgeUseCase()) doReturn Observable.just(false)
        presenter.attachView(viewState)

        inOrder(viewState).apply {
            verify(viewState).showContent(initialVo)
            verify(viewState).setNewsBadgeVisible(false)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `Navigate to referral program on item click`() {
        presenter.onItemClick()

        verify(router).navigateTo(ReferralProgramTargetScreen())
    }

    @Test
    fun `Send visible analytic for active state`() {
        presenter.attachView(viewState)

        verify(analytics).profileButtonVisible(referralProgramStatus, userDescription)
    }

    @Test
    fun `Send click analytic when navigate`() {
        presenter.attachView(viewState)

        presenter.onItemClick()

        verify(analytics).profileButtonNavigate(referralProgramStatus, userDescription)
    }
}
