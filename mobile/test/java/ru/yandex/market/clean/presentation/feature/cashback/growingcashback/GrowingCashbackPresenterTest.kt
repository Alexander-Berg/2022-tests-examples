package ru.yandex.market.clean.presentation.feature.cashback.growingcashback

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.SingleSubject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.activity.web.MarketWebActivityArguments
import ru.yandex.market.activity.web.MarketWebActivityTargetScreen
import ru.yandex.market.analytics.facades.GrowingCashbackAnalytics
import ru.yandex.market.clean.domain.model.referralprogram.ReferralProgramStatus
import ru.yandex.market.clean.domain.model.referralprogram.referralProgramStatusTestInstance
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.formatter.GrowingCashbackActonFormatter
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.vo.GrowingCashbackActionVo
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.vo.GrowingCashbackButtonsVo
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.vo.GrowingCashbackInfoButtonVo
import ru.yandex.market.clean.presentation.feature.cms.HomeTargetScreen
import ru.yandex.market.clean.presentation.feature.referralprogram.ReferralProgramTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.domain.cashback.model.GrowingCashbackActionInfo
import ru.yandex.market.domain.cashback.model.GrowingCashbackActionState
import ru.yandex.market.domain.cashback.model.growingCashbackActionInfoTestInstance
import ru.yandex.market.domain.cashback.model.growingCashbackActionInfo_OrderRewardTestInstance
import ru.yandex.market.domain.cashback.model.growingCashbackActionStateTestInstance
import ru.yandex.market.domain.user.model.userProfileTestInstance
import ru.yandex.market.optional.Optional
import ru.yandex.market.presentationSchedulersMock

class GrowingCashbackPresenterTest {

    private val view = mock<`GrowingCashbackView$$State`>()
    private val contentVo = mock<GrowingCashbackActionVo.GrowingCashbackActionStateVo>()
    private val errorVo = mock<GrowingCashbackActionVo.GrowingCashbackInfoVo> {
        on { button } doReturn GrowingCashbackInfoButtonVo(
            "",
            GrowingCashbackInfoButtonVo.ButtonAction.RELOAD
        )
    }
    private val router = mock<Router>()
    private val analytics = mock<GrowingCashbackAnalytics>()
    private val contentSubject: SingleSubject<GrowingCashbackActionInfo> = SingleSubject.create()
    private val referralSubject: BehaviorSubject<ReferralProgramStatus> = BehaviorSubject.create()
    private val userProfile = userProfileTestInstance()
    private val useCases = mock<GrowingCashbackUseCases> {
        on { getGrowingCashbackActionInfo() } doReturn contentSubject
        on { getReferralProgramStatus() } doReturn referralSubject
        on { hideGrowingCashbackCommunication() } doReturn Completable.complete()
        on { getCachedUserProfile() } doReturn Observable.just(Optional.of(userProfile))
    }
    private val formatter = mock<GrowingCashbackActonFormatter> {
        on { format(any(), any()) } doReturn contentVo
        on { formatError(any()) } doReturn errorVo
    }

    private val presenter = GrowingCashbackPresenter(
        presentationSchedulersMock(),
        router,
        useCases,
        formatter,
        analytics
    )

    @Before
    fun setup() {
        presenter.setViewState(view)
    }

    @Test
    fun `load content on first view attach`() {
        presenter.attachView(view)
        presenter.attachView(view)
        contentSubject.onSuccess(growingCashbackActionInfoTestInstance())
        referralSubject.onNext(referralProgramStatusTestInstance())

        val inOrder = inOrder(view)
        inOrder.verify(view).showProgress()
        inOrder.verify(view).showContent(contentVo)
    }

    @Test
    fun `do not show error if got referral program status error`() {
        presenter.attachView(view)
        contentSubject.onSuccess(growingCashbackActionInfoTestInstance())
        referralSubject.onError(Error())

        verify(formatter).format(any(), eq(ReferralProgramStatus.Disabled))
        verify(view).showContent(contentVo)
    }

    @Test
    fun `show error when error received`() {
        presenter.attachView(view)

        contentSubject.onError(Error())
        verify(view).showContent(errorVo)
    }

    @Test
    fun `close button click`() {
        presenter.onCloseClick()

        verify(router).finishFlow()
    }

    @Test
    fun `go to home content button click`() {
        presenter.onContentButtonClick(GrowingCashbackButtonsVo.ButtonAction.Home)

        val inOrder = inOrder(router)
        inOrder.verify(router).finishFlow()
        inOrder.verify(router).navigateTo(HomeTargetScreen())
    }

    @Test
    fun `about content button click`() {
        presenter.onContentButtonClick(GrowingCashbackButtonsVo.ButtonAction.About("aboutLink"))

        verify(router).navigateTo(
            MarketWebActivityTargetScreen(
                MarketWebActivityArguments
                    .builder()
                    .link("aboutLink")
                    .isUrlOverridingEnabled(false)
                    .build()
            )
        )
    }

    @Test
    fun `referral info button click`() {
        presenter.onInfoButtonClick(GrowingCashbackInfoButtonVo.ButtonAction.REFERRAL)

        verify(router).replace(ReferralProgramTargetScreen())
    }

    @Test
    fun `reload info button click`() {
        presenter.onInfoButtonClick(GrowingCashbackInfoButtonVo.ButtonAction.RELOAD)

        contentSubject.onSuccess(growingCashbackActionInfoTestInstance())
        referralSubject.onNext(referralProgramStatusTestInstance())

        val inOrder = inOrder(view)
        inOrder.verify(view).showProgress()
        inOrder.verify(view).showContent(contentVo)
    }

    @Test
    fun `close info button click`() {
        presenter.onInfoButtonClick(GrowingCashbackInfoButtonVo.ButtonAction.CLOSE)

        verify(router).finishFlow()
    }

    @Test
    fun `hide growing cashback communication on action complete state shown`() {
        presenter.attachView(view)
        contentSubject.onSuccess(
            growingCashbackActionInfoTestInstance(
                actionState = growingCashbackActionStateTestInstance(
                    state = GrowingCashbackActionState.State.COMPLETE
                )
            )
        )
        referralSubject.onNext(referralProgramStatusTestInstance())

        verify(useCases).hideGrowingCashbackCommunication()
    }

    @Test
    fun `send landing visible event on content loaded`() {
        presenter.attachView(view)
        contentSubject.onSuccess(
            growingCashbackActionInfoTestInstance(
                ordersReward = listOf(
                    growingCashbackActionInfo_OrderRewardTestInstance(isOrderDelivered = true),
                    growingCashbackActionInfo_OrderRewardTestInstance(isOrderDelivered = true),
                    growingCashbackActionInfo_OrderRewardTestInstance(isOrderDelivered = false)
                )
            )
        )
        referralSubject.onNext(referralProgramStatusTestInstance())

        verify(analytics).landingVisible(true, userProfile.hasYandexPlus, 2)
    }

    @Test
    fun `send referral button visible if button shown`() {
        presenter.attachView(view)
        val infoVo = mock<GrowingCashbackActionVo.GrowingCashbackInfoVo> {
            on { button } doReturn GrowingCashbackInfoButtonVo(
                "",
                GrowingCashbackInfoButtonVo.ButtonAction.REFERRAL
            )
        }
        whenever(formatter.format(any(), any())) doReturn infoVo
        contentSubject.onSuccess(growingCashbackActionInfoTestInstance())
        referralSubject.onNext(referralProgramStatusTestInstance())

        verify(analytics).landingReferralButtonVisible()
    }

    @Test
    fun `send referral button navigate on referral button click `() {
        presenter.onInfoButtonClick(GrowingCashbackInfoButtonVo.ButtonAction.REFERRAL)

        verify(analytics).landingReferralButtonNavigate()
    }
}
