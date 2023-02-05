package ru.yandex.market.clean.presentation.feature.plustrial

import io.reactivex.subjects.CompletableSubject
import io.reactivex.subjects.SingleSubject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.beru.android.R
import ru.yandex.market.activity.web.MarketWebActivityArguments
import ru.yandex.market.activity.web.MarketWebActivityTargetScreen
import ru.yandex.market.activity.web.MarketWebParams
import ru.yandex.market.activity.web.MarketWebTargetScreen
import ru.yandex.market.analytics.facades.PlusTrialAnalytics
import ru.yandex.market.clean.domain.model.plustrial.PlusTrialInfo
import ru.yandex.market.clean.domain.model.plustrial.plusTrialInfoTestInstance
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.presentationSchedulersMock

class GetPlusTrialPresenterTest {

    private val infoSubject: SingleSubject<PlusTrialInfo> = SingleSubject.create()
    private val getTrialSubject: CompletableSubject = CompletableSubject.create()
    private val landingUrl = "landingUrl"
    private val info = plusTrialInfoTestInstance(promoUrl = landingUrl)
    private val infoVo = mock<GetPlusTrialVo>()
    private val infoLoadErrorVo = mock<GetPlusTrialVo>()
    private val plusReceivedVo = mock<GetPlusTrialVo>()
    private val plusReceiveErrorVo = mock<GetPlusTrialVo>()
    private val viewState = mock<`GetPlusTrialView$$State`>()
    private val router = mock<Router>()
    private val analytics = mock<PlusTrialAnalytics>()

    private val useCases = mock<GetPlusTrialUseCases> {
        on { getPlusTrialInfo() } doReturn infoSubject
        on { getPlusTrial() } doReturn getTrialSubject
    }

    private val formatter = mock<GetPlusTrialFormatter> {
        on { formatInfo(eq(info), any()) } doReturn infoVo
        on { formatInfoError() } doReturn infoLoadErrorVo
        on { formatPlusReceived(any()) } doReturn plusReceivedVo
        on { formatPlusError(any()) } doReturn plusReceiveErrorVo
    }

    private val presenter = GetPlusTrialPresenter(
        presentationSchedulersMock(),
        GetPlusTrialArguments(GetPlusTrialArguments.State.GET_PRESENT),
        router,
        useCases,
        formatter,
        analytics
    )

    @Before
    fun setup() {
        presenter.setViewState(viewState)
    }


    @Test
    fun `load info on first attach`() {
        presenter.attachView(viewState)
        infoSubject.onSuccess(info)

        val inOrder = inOrder(viewState)
        inOrder.verify(viewState).showProgress()
        inOrder.verify(viewState).showContent(infoVo)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `show error if received error on info load`() {
        presenter.attachView(viewState)
        infoSubject.onError(RuntimeException())

        val inOrder = inOrder(viewState)
        inOrder.verify(viewState).showProgress()
        inOrder.verify(viewState).showContent(infoLoadErrorVo)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `get trial on get trial click`() {
        presenter.onButtonClick(GetPlusTrialVo.ButtonAction.GET_PLUS)
        getTrialSubject.onComplete()

        val inOrder = inOrder(viewState)
        inOrder.verify(viewState).showProgress()
        inOrder.verify(viewState).showContent(plusReceivedVo)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `set navigation result when plus trial received`() {
        presenter.onButtonClick(GetPlusTrialVo.ButtonAction.GET_PLUS)
        getTrialSubject.onComplete()

        verify(router).setResult(PlusTrialReceived)
    }

    @Test
    fun `show error if was error on get trial`() {
        presenter.onButtonClick(GetPlusTrialVo.ButtonAction.GET_PLUS)
        getTrialSubject.onError(RuntimeException())

        val inOrder = inOrder(viewState)
        inOrder.verify(viewState).showProgress()
        inOrder.verify(viewState).showContent(plusReceiveErrorVo)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `reload info on retry load info click`() {
        presenter.onButtonClick(GetPlusTrialVo.ButtonAction.RELOAD_INFO)

        verify(viewState).showProgress()
        verify(useCases).getPlusTrialInfo()
    }

    @Test
    fun `navigate to about plus on about click`() {
        presenter.onButtonClick(GetPlusTrialVo.ButtonAction.ABOUT_PLUS)

        verify(router).navigateTo(
            MarketWebActivityTargetScreen(
                MarketWebActivityArguments.builder()
                    .linkRes(R.string.yandex_plus_link)
                    .isUrlOverridingEnabled(true)
                    .build()
            )
        )
    }

    @Test
    fun `navigate to landing on see more click`() {
        presenter.attachView(viewState)
        infoSubject.onSuccess(info)

        presenter.onButtonClick(GetPlusTrialVo.ButtonAction.SEE_PROMO)

        verify(viewState).closeSelf()
        verify(router).navigateTo(MarketWebTargetScreen(MarketWebParams(landingUrl)))
    }

    @Test
    fun `close view state on close click`() {
        presenter.onButtonClick(GetPlusTrialVo.ButtonAction.CLOSE)

        verify(viewState).closeSelf()
    }

    @Test
    fun `send visible event for plus trial info state`() {
        presenter.attachView(viewState)
        infoSubject.onSuccess(info)

        verify(analytics).getPlusTrialDialogVisible()
    }

    @Test
    fun `send navigate event on get trial click`() {
        presenter.onButtonClick(GetPlusTrialVo.ButtonAction.GET_PLUS)

        verify(analytics).getPlusTrialClick()
    }

    @Test
    fun `send visible event for plus trial received state`() {
        presenter.onButtonClick(GetPlusTrialVo.ButtonAction.GET_PLUS)
        getTrialSubject.onComplete()

        verify(analytics).plusTrialReceivedDialogVisible()
    }

    @Test
    fun `send navigate event on see promo click`() {
        presenter.onButtonClick(GetPlusTrialVo.ButtonAction.SEE_PROMO)

        verify(analytics).plusTrialReceivedButtonClick()
    }

    @Test
    fun `send navigate event on close button click`() {
        presenter.onButtonClick(GetPlusTrialVo.ButtonAction.CLOSE)

        verify(analytics).plusTrialReceivedButtonClick()
    }
}