package ru.yandex.market.clean.presentation.feature.review.success

import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.activity.web.MarketWebParams
import ru.yandex.market.activity.web.MarketWebTargetScreen
import ru.yandex.market.analytics.facades.ReferralProgramAnalytics
import ru.yandex.market.clean.domain.model.referralprogram.ReferralProgramStatus
import ru.yandex.market.clean.domain.model.referralprogram.ReferralProgramUserDescription
import ru.yandex.market.clean.domain.model.referralprogram.referralProgramStatusTestInstance
import ru.yandex.market.clean.domain.model.referralprogram.referralProgramStatus_EnabledTestInstance
import ru.yandex.market.clean.domain.model.review.ReviewPaymentInfo
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeArguments
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeFlowAnalyticsInfo
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeTargetScreen
import ru.yandex.market.clean.presentation.feature.referralprogram.ReferralProgramTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.clean.presentation.parcelable.review.toParcelable
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.presentationSchedulersMock

class ReviewSuccessPresenterTest {

    private val view = mock<ReviewSuccessView>()

    private val referralProgramStatusUseCaseSubject: BehaviorSubject<ReferralProgramStatus> = BehaviorSubject.create()
    private val presentationSchedulers = presentationSchedulersMock()
    private val userDescription = mock<ReferralProgramUserDescription>()
    private val reviewSuccessUseCases = mock<ReviewSuccessUseCases> {
        on { checkReferralProgramStatus() } doReturn referralProgramStatusUseCaseSubject
        on { getReferralProgramUserDescription() } doReturn Single.just(userDescription)
    }
    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.REVIEW_SUCCESS
    }
    private val resourcesDataStore = mock<ResourcesManager> {
        on { getString(any()) } doReturn DUMMY_URL
    }
    private val reviewSuccessFragmentArguments = ReviewSuccessFragment.Arguments(
        hasPlus = true,
        didUploadNewPhotos = false,
        hasAnyTextComment = true,
        reviewPaymentInfo = ReviewPaymentInfo(10, null).toParcelable()
    )
    private val reviewSuccessVo = mock<ReviewSuccessVo>()
    private val reviewSuccessFormatter = mock<ReviewSuccessFormatter> {
        on { format(any(), any(), any(), any(), any(), anyOrNull()) } doReturn reviewSuccessVo
    }
    private val analytics = mock<ReferralProgramAnalytics>()

    private fun createPresenter() =
        ReviewSuccessPresenter(
            presentationSchedulers,
            reviewSuccessUseCases,
            router,
            resourcesDataStore,
            reviewSuccessFragmentArguments,
            reviewSuccessFormatter,
            false,
            analytics
        )

    @Test
    fun `Should finish flow when close clicked`() {
        val presenter = createPresenter()

        presenter.onCloseClick()

        verify(router).finishFlow()
    }

    @Test
    fun `Should finish flow when back clicked`() {
        val presenter = createPresenter()

        presenter.onBackClicked()

        verify(router).finishFlow()
    }

    @Test
    fun `Should finish flow when ok button clicked`() {
        val presenter = createPresenter()

        presenter.onOkButtonClick()

        verify(router).finishFlow()
    }

    @Test
    fun `Should open plus home when badge clicked`() {
        val presenter = createPresenter()

        presenter.onBadgeClick()

        verify(router).navigateTo(
            PlusHomeTargetScreen(PlusHomeArguments(PlusHomeFlowAnalyticsInfo(Screen.REVIEW_SUCCESS.toString())))
        )
    }

    @Test
    fun `Should open webview when read about plus clicked`() {
        val presenter = createPresenter()
        presenter.attachView(view)
        whenever(reviewSuccessVo.secondaryActionVo) doReturn ReviewSuccessVo.SecondaryActionVo(
            "",
            ReviewSuccessVo.ButtonActon.ABOUT_PLUS,
        )

        referralProgramStatusUseCaseSubject.onNext(referralProgramStatusTestInstance())
        presenter.onSecondaryButtonClick()

        verify(router).navigateTo(
            MarketWebTargetScreen(
                MarketWebParams(DUMMY_URL)
            )
        )
    }

    @Test
    fun `Should open referral program when recommend clicked`() {
        val presenter = createPresenter()
        presenter.attachView(view)
        whenever(reviewSuccessVo.secondaryActionVo) doReturn ReviewSuccessVo.SecondaryActionVo(
            "",
            ReviewSuccessVo.ButtonActon.REFERRAL_PROGRAM,
        )

        referralProgramStatusUseCaseSubject.onNext(referralProgramStatus_EnabledTestInstance())
        presenter.onSecondaryButtonClick()

        verify(router).navigateTo(ReferralProgramTargetScreen())
    }

    @Test
    fun `Should call render when view attached`() {
        val presenter = createPresenter()

        presenter.attachView(view)

        verify(view).render(reviewSuccessVo)
    }

    @Test
    fun `Should render view after referral program after referral program status received`() {
        val initialVo = mock<ReviewSuccessVo>()
        val referralStatusVo = mock<ReviewSuccessVo>()
        whenever(
            reviewSuccessFormatter.format(
                any(),
                any(),
                any(),
                any(),
                eq(ReferralProgramStatus.Disabled),
                anyOrNull()
            )
        ) doReturn initialVo
        whenever(
            reviewSuccessFormatter.format(
                any(),
                any(),
                any(),
                any(),
                eq(referralProgramStatus_EnabledTestInstance()),
                anyOrNull()
            )
        ) doReturn referralStatusVo

        val presenter = createPresenter()

        presenter.attachView(view)

        referralProgramStatusUseCaseSubject.onNext(referralProgramStatus_EnabledTestInstance())

        val inOrderCheck = inOrder(view)
        inOrderCheck.verify(view).render(initialVo)
        inOrderCheck.verify(view).render(referralStatusVo)
    }

    @Test
    fun `Should render view after referral program status error received`() {
        val presenter = createPresenter()

        presenter.attachView(view)

        referralProgramStatusUseCaseSubject.onError(Error())

        verify(view, times(2)).render(reviewSuccessVo)
    }

    @Test
    fun `Send visible analytic for active state`() {
        val presenter = createPresenter()

        presenter.attachView(view)
        referralProgramStatusUseCaseSubject.onNext(referralProgramStatus_EnabledTestInstance())

        verify(analytics).referralEntryPointShown(
            referralProgramStatus = referralProgramStatus_EnabledTestInstance(),
            referralProgramUserDescription = userDescription,
            screen = Screen.REVIEW_SUCCESS
        )
    }

    @Test
    fun `Send click analytic when navigate`() {
        val presenter = createPresenter()

        presenter.attachView(view)
        referralProgramStatusUseCaseSubject.onNext(referralProgramStatus_EnabledTestInstance())
        presenter.openReferralProgram()

        verify(analytics).referralEntryPointNavigate(
            referralProgramStatus = referralProgramStatus_EnabledTestInstance(),
            referralProgramUserDescription = userDescription,
            screen = Screen.REVIEW_SUCCESS
        )
    }

    private companion object {
        const val DUMMY_URL = "DUMMY_URL"
    }
}
