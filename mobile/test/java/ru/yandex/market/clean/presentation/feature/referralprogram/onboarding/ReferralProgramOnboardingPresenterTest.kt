package ru.yandex.market.clean.presentation.feature.referralprogram.onboarding

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatcher
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import ru.beru.android.R
import ru.yandex.market.activity.web.MarketWebParams
import ru.yandex.market.activity.web.MarketWebTargetScreen
import ru.yandex.market.analytics.facades.ReferralProgramAnalytics
import ru.yandex.market.clean.domain.model.referralprogram.ReferralProgramInfo
import ru.yandex.market.clean.domain.model.referralprogram.ReferralProgramUserDescription
import ru.yandex.market.clean.domain.model.referralprogram.referralProgramInfo_EnabledTestInstance
import ru.yandex.market.clean.presentation.error.CommonErrorHandler
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.common.errors.common.commonErrorVoTestInstance
import ru.yandex.market.presentationSchedulersMock

class ReferralProgramOnboardingPresenterTest {

    private val infoSubject: SingleSubject<ReferralProgramInfo> = SingleSubject.create()
    private val info = referralProgramInfo_EnabledTestInstance()
    private val contentVo = referralProgramOnboardingVoTestInstance()
    private val disabledErrorVo = commonErrorVoTestInstance(title = "disabled error")
    private val loadErrorVo = commonErrorVoTestInstance(title = "load error")
    private val loadError = Error()
    private val userDescription = mock<ReferralProgramUserDescription>()

    private var buttonAction: (() -> Unit)? = null
    private val actionExtractorMatcher = AnyActionExtractorMatcher { buttonAction = it }

    private val router = mock<Router>()
    private val useCases = mock<ReferralProgramOnboardingUseCases> {
        on { getReferralProgramInfo() } doReturn infoSubject
        on { copyPromoToClipboard(info.promocode) } doReturn Completable.complete()
        on { getReferralProgramUserDescription() } doReturn Single.just(userDescription)
        on { isPartnerProgramPopupWasShown() } doReturn Observable.just(false)
    }
    private val formatter = mock<ReferralProgramOnboardingFormatter> {
        on { formatEnabled(info) } doReturn contentVo
        on { formatDisabled(anyOrNull(), argThat(actionExtractorMatcher)) } doReturn disabledErrorVo
    }
    private val errorHandler = mock<CommonErrorHandler> {
        on {
            format(
                eq(loadError),
                eq(router),
                anyOrNull(),
                anyOrNull(),
                argThat(actionExtractorMatcher)
            )
        } doReturn loadErrorVo
    }
    private val referralProgramShareMessageFormatter = mock<ReferralProgramShareMessageFormatter> {
        on { format(info) } doReturn SHARE_MESSAGE
    }
    private val resourcesDataStore = mock<ResourcesManager> {
        on { getString(R.string.about_referral_program) } doReturn RULES_LINK
    }
    private val analytics = mock<ReferralProgramAnalytics>()
    private val view = mock<`ReferralProgramOnboardingView$$State`>()

    private val presenter = ReferralProgramOnboardingPresenter(
        presentationSchedulersMock(),
        router,
        useCases,
        formatter,
        errorHandler,
        referralProgramShareMessageFormatter,
        resourcesDataStore,
        analytics
    )

    @Before
    fun setup() {
        presenter.attachView(view)
        buttonAction = null
    }

    @Test
    fun `load and show promocode info on attach`() {
        infoSubject.onSuccess(info)

        verify(view).showContent(contentVo)
    }

    @Test
    fun `show disabled error promo is disabled`() {
        infoSubject.onSuccess(ReferralProgramInfo.Disabled)

        verify(view).showError(disabledErrorVo)
    }

    @Test
    fun `show load error if was error on info load`() {
        infoSubject.onError(loadError)

        verify(view).showError(loadErrorVo)
    }

    @Test
    fun `router goes back on back click`() {
        presenter.onBackClick()

        verify(router).back()
    }

    @Test
    fun `router goes back on close click`() {
        presenter.onCloseClick()

        verify(router).back()
    }

    @Test
    fun `router goes back on disabled state action click`() {
        infoSubject.onSuccess(ReferralProgramInfo.Disabled)
        buttonAction?.invoke()

        verify(router).back()
    }

    @Test
    fun `reload content on error state action click`() {
        infoSubject.onError(loadError)

        buttonAction?.invoke()

        verify(useCases, times(2)).getReferralProgramInfo()
    }

    @Test
    fun `show promocode copied on copy promocode`() {
        infoSubject.onSuccess(info)

        presenter.onCopyPromoClick()

        verify(useCases).copyPromoToClipboard(info.promocode)
        verify(view).showPromoCodeIsCopied()
    }

    @Test
    fun `show share dialog on share promocode click`() {
        infoSubject.onSuccess(info)

        presenter.onShareClick()

        verify(view).showShare(SHARE_MESSAGE)
    }

    @Test
    fun `navigate to rules whet about click`() {
        presenter.onAboutClick()

        verify(router).navigateTo(
            MarketWebTargetScreen(
                MarketWebParams(RULES_LINK)
            )
        )
    }

    @Test
    fun `send share button visible analytic for enabled state`() {
        infoSubject.onSuccess(info)

        verify(analytics).sharePromocodeVisible(info, userDescription)
    }

    @Test
    fun `send share button navigate analytic on share click`() {
        infoSubject.onSuccess(info)

        presenter.onShareClick()

        verify(analytics).sharePromocodeNavigate(info, userDescription)
    }

    @Test
    fun `send copy button visible analytic for enabled state`() {
        infoSubject.onSuccess(info)

        verify(analytics).copyPromocodeVisible(info, userDescription)
    }

    @Test
    fun `send copy button click analytic on share click`() {
        infoSubject.onSuccess(info)

        presenter.onCopyPromoClick()

        verify(analytics).copyPromocodeNavigate(info, userDescription)
    }

    private class AnyActionExtractorMatcher(
        private val argumentsExtractor: (() -> Unit) -> Unit
    ) : ArgumentMatcher<(() -> Unit)> {
        override fun matches(argument: (() -> Unit)?): Boolean {
            return argument?.let {
                argumentsExtractor.invoke(argument)
                true
            } ?: false
        }
    }

    companion object {
        private const val SHARE_MESSAGE = "Поделитесь промокодом"
        private const val RULES_LINK = "https://some.rules.com"
    }
}