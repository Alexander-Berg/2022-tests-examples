package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.advertisingcamaign

import io.reactivex.Completable
import io.reactivex.subjects.SingleSubject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyZeroInteractions
import org.mockito.kotlin.whenever
import ru.beru.android.R
import ru.yandex.market.analytics.facades.WelcomeCashbackAnalytics
import ru.yandex.market.clean.presentation.feature.auth.RequestAuthParams
import ru.yandex.market.clean.presentation.feature.auth.RequestAuthResult
import ru.yandex.market.clean.presentation.feature.auth.RequestAuthTargetScreen
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeArguments
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeFlowAnalyticsInfo
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeOnboardingRequest
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeTargetScreen
import ru.yandex.market.clean.presentation.navigation.ResultListener
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.domain.cashback.model.WelcomeCashbackInfo
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.presentationSchedulersMock
import java.math.BigDecimal

class AdvertisingCampaignSnippetPresenterTest {

    private val primaryAction = mock<AdvertisingCampaignButtonAction>()
    private val secondaryAction = mock<AdvertisingCampaignButtonAction>()
    private val viewObject = mock<CmsAdvertisingCampaignVo> {
        on { primaryAction } doReturn primaryAction
        on { secondaryAction } doReturn secondaryAction
    }
    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.HOME
    }
    private val viewState = mock<`AdvertisingCampaignSnippetView$$State`>()
    private val welcomeCashbackInfoSubject: SingleSubject<WelcomeCashbackInfo> = SingleSubject.create()
    private val useCases = mock<AdvertisingCampaignUseCases> {
        on { saveInteracted() } doReturn Completable.complete()
        on { getWelcomeCashbackInfo() } doReturn welcomeCashbackInfoSubject
    }
    private val resourceDataSource = mock<ResourcesManager>()
    private val welcomeCashbackAnalytics = mock<WelcomeCashbackAnalytics>()

    private val presenter = AdvertisingCampaignSnippetPresenter(
        presentationSchedulersMock(),
        viewObject,
        router,
        useCases,
        resourceDataSource,
        welcomeCashbackAnalytics
    )

    @Before
    fun setup() {
        presenter.setViewState(viewState)
    }

    @Test
    fun `Show data on attach`() {
        presenter.attachView(viewState)

        verify(viewState).showData(viewObject)
    }

    @Test
    fun `Navigate to plus home screen on about button click`() {
        whenever(primaryAction.action).doReturn(AdvertisingCampaignButtonAction.Action.ABOUT_PLUS)
        whenever(secondaryAction.action).doReturn(AdvertisingCampaignButtonAction.Action.ABOUT_PLUS)

        presenter.onPrimaryButtonClick()
        presenter.onSecondaryButtonClick()

        val expectedTarget = PlusHomeTargetScreen(
            PlusHomeArguments(
                analyticsInfo = PlusHomeFlowAnalyticsInfo((router.currentScreen.toString())),
                plusHomeOnboardingRequest = PlusHomeOnboardingRequest.NONE
            )
        )

        verify(router, atLeast(2)).navigateTo(expectedTarget)
        verify(useCases, atLeast(2)).saveInteracted()
    }

    @Test
    fun `Navigate to login on login button click`() {
        whenever(primaryAction.action).doReturn(AdvertisingCampaignButtonAction.Action.LOGIN)
        whenever(secondaryAction.action).doReturn(AdvertisingCampaignButtonAction.Action.LOGIN)

        val expectedLoginTarget = RequestAuthTargetScreen(RequestAuthParams(tryAutoLogin = true))

        presenter.onPrimaryButtonClick()
        presenter.onSecondaryButtonClick()

        verify(router, atLeast(2)).navigateForResult(
            eq(expectedLoginTarget),
            argThat { actualListener ->
                actualListener.onResult(RequestAuthResult(true))
                true
            }
        )

        verify(useCases, atLeast(2)).saveInteracted()
    }

    @Test
    fun `Show snackbar after success login if promo not available`() {
        whenever(primaryAction.action).doReturn(AdvertisingCampaignButtonAction.Action.LOGIN)
        whenever(
            resourceDataSource.getFormattedString(
                R.string.welcome_cashback_already_has_orders_snackbar_title,
                BigDecimal.TEN
            )
        ).doReturn("Акция не действует")

        presenter.onPrimaryButtonClick()

        var resultListener: ResultListener? = null

        verify(router).navigateForResult(
            any(),
            argThat { actualListener ->
                resultListener = actualListener
                true
            }
        )

        resultListener?.onResult(RequestAuthResult(true))

        welcomeCashbackInfoSubject.onSuccess(
            WelcomeCashbackInfo(
                isWelcomeCashbackEmitOrderAvailable = false,
                cashbackAmount = BigDecimal.TEN,
                priceFrom = BigDecimal.TEN
            )
        )

        verify(viewState).showAlreadyHasOrdersSnackbar("Акция не действует")
    }

    @Test
    fun `Save widget interacted on close button click`() {
        whenever(primaryAction.action).doReturn(AdvertisingCampaignButtonAction.Action.CLOSE)
        whenever(secondaryAction.action).doReturn(AdvertisingCampaignButtonAction.Action.CLOSE)

        presenter.onPrimaryButtonClick()
        presenter.onSecondaryButtonClick()

        verify(useCases, atLeast(2)).saveInteracted()
    }

    @Test
    fun `Do nothing on secondary acton click if secondary action is null`() {
        whenever(viewObject.secondaryAction).doReturn(null)

        presenter.onSecondaryButtonClick()

        verifyZeroInteractions(useCases)
        verifyZeroInteractions(router)
    }
}