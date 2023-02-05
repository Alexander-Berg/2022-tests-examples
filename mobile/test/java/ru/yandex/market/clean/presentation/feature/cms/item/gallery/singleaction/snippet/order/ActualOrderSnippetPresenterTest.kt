package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.order

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyZeroInteractions
import ru.beru.android.R
import ru.yandex.market.activity.order.details.OrderDetailsParams
import ru.yandex.market.activity.order.details.OrderDetailsTargetScreen
import ru.yandex.market.activity.web.MarketWebActivityArguments
import ru.yandex.market.activity.web.MarketWebActivityTargetScreen
import ru.yandex.market.analitycs.events.OrderTrackPageVisibleEvent
import ru.yandex.market.analytics.facades.ActualOrderSnippetAnalytics
import ru.yandex.market.checkout.pickup.single.PickupPointArguments
import ru.yandex.market.clean.data.mapper.DeliveryDateTimeIntervalMapper
import ru.yandex.market.clean.domain.model.lavka.OnDemandCourierLink
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.clean.presentation.feature.auth.RequestAuthParams
import ru.yandex.market.clean.presentation.feature.auth.RequestAuthResult
import ru.yandex.market.clean.presentation.feature.auth.RequestAuthTargetScreen
import ru.yandex.market.clean.presentation.feature.cms.model.CmsActualOrderVo
import ru.yandex.market.clean.presentation.feature.cms.model.cmsActualOrderVoTestInstance
import ru.yandex.market.clean.presentation.feature.ondemand.OnDemandCourierScreenManager
import ru.yandex.market.clean.presentation.feature.ondemand.OnDemandHealthFacade
import ru.yandex.market.clean.presentation.feature.order.change.prepayment.flow.ChangePrepaymentFlowFragment
import ru.yandex.market.clean.presentation.feature.order.change.prepayment.flow.ChangePrepaymentFlowTargetScreen
import ru.yandex.market.clean.presentation.feature.order.consultation.ConsultationFlowArguments
import ru.yandex.market.clean.presentation.feature.order.consultation.ConsultationFlowTargetScreen
import ru.yandex.market.clean.presentation.feature.order.feedback.dialog.OrderFeedbackQuestionAnalytics
import ru.yandex.market.clean.presentation.feature.order.feedback.dialog.OrderFeedbackQuestionsDialogFragment
import ru.yandex.market.clean.presentation.feature.order.feedback.dialog.OrderFeedbackQuestionsDialogTargetScreen
import ru.yandex.market.clean.presentation.feature.order.feedback.flow.OrderFeedbackFlowFragment
import ru.yandex.market.clean.presentation.feature.order.feedback.flow.OrderFeedbackFlowTargetScreen
import ru.yandex.market.clean.presentation.feature.orderfeedback.OrderFeedbackDialogFragment
import ru.yandex.market.clean.presentation.feature.orderfeedback.OrderFeedbackDialogTargetScreen
import ru.yandex.market.clean.presentation.feature.orderfeedback.OrderFeedbackHealthFacade
import ru.yandex.market.clean.presentation.feature.payment.PayerParams
import ru.yandex.market.clean.presentation.navigation.ResultListener
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.data.order.options.point.OutletPoint
import ru.yandex.market.fragment.order.container.AllOrdersFlowTargetScreen
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.util.manager.InstalledApplicationManager

class ActualOrderSnippetPresenterTest {

    private val viewObject = cmsActualOrderVoTestInstance()
    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.HOME
    }

    private val order = mock<Order> {
        on { isClickAndCollect } doReturn false
        on { id } doReturn 0
    }
    private val onDemandLinkSubject: SingleSubject<OnDemandCourierLink> = SingleSubject.create()
    private val trackOrderSubject: SingleSubject<String> = SingleSubject.create()
    private val useCases = mock<ActualOrderSnippetUseCases> {
        on { getOrder(any(), any()) } doReturn Single.just(order)
        on {
            setOrderReviewDenied(
                viewObject.orderId,
                viewObject.feedback?.orderFeedbackGrade?.gradeValue
            )
        } doReturn Completable.complete()
        on {
            getOnDemandCourierLink(
                true,
                viewObject.trackingCode!!,
                viewObject.orderId.toString(),
                viewObject.onDemandWarehouseType,
                viewObject.regionId,
                viewObject.callLavkaCourierPath,
            )
        } doReturn onDemandLinkSubject
        on { requestReloadWidgetData(PARENT_WIDGET_ID) } doReturn Completable.complete()
        on { getTrackingUrl(viewObject.orderId) } doReturn trackOrderSubject
        on { canShowPickupRenewalButton(any()) } doReturn Single.just(false)
    }
    private val resourceDataSource = mock<ResourcesManager> {
        on { getString(any()) } doReturn "test string"
    }
    private val orderFeedbackQuestionsAnalytics = mock<OrderFeedbackQuestionAnalytics>()
    private val onDemandHeathFacade = mock<OnDemandHealthFacade>()
    private val actualOrderSnippetAnalytics = mock<ActualOrderSnippetAnalytics>()
    private val installedApplicationManager = mock<InstalledApplicationManager> {
        on { isApplicationInstalled(R.string.taxi_app_id) } doReturn Single.just(true)
    }
    private val onDemandCourierScreenManager = mock<OnDemandCourierScreenManager>()
    private val deliveryDateTimeIntervalMapper = mock<DeliveryDateTimeIntervalMapper>()
    private val orderFeedbackHealthFacade = mock<OrderFeedbackHealthFacade>()
    private val viewState = mock<`ActualOrderView$$State`>()

    private val presenter = ActualOrderSnippetPresenter(
        presentationSchedulersMock(),
        viewObject,
        PARENT_WIDGET_ID,
        router,
        useCases,
        resourceDataSource,
        orderFeedbackQuestionsAnalytics,
        onDemandHeathFacade,
        installedApplicationManager,
        actualOrderSnippetAnalytics,
        onDemandCourierScreenManager,
        deliveryDateTimeIntervalMapper,
        orderFeedbackHealthFacade
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
    fun `Navigate to order info`() {
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.MORE_INFO)

        val expectedTarget = OrderDetailsTargetScreen(
            OrderDetailsParams(
                orderId = viewObject.orderId.toString(),
                shopOrderId = viewObject.shopId,
                trackDeliveryServiceId = viewObject.trackDeliveryServiceId,
                trackingCode = viewObject.trackingCode,
                isClickAndCollect = viewObject.isClickAndCollect,
                isArchived = false,
                isShowChangeDateTime = false
            )
        )
        verify(router).navigateTo(expectedTarget)
    }

    @Test
    fun `Navigate to order map`() {
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.SHOW_IN_MAP)

        val expectedTarget = PickupPointArguments.Builder()
            .title("")
            .isClickAndCollect(order.isClickAndCollect)
            .showNearestDelivery(true)
            .showSelectButton(false)
            .showBuildRouteButton(true)
            .outlet(OutletPoint(null))
            .orderId(order.id)
            .sourceScreen(router.currentScreen)
            .buildTarget()

        verify(router).navigateTo(expectedTarget)
    }

    @Test
    fun `Navigate to pay`() {
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.PAY)

        val payer = viewObject.buyer?.let {
            PayerParams(
                fullName = it.name.orEmpty(),
                phoneNum = it.phone.orEmpty(),
                email = it.email.orEmpty()
            )
        }
        val expectedTarget = ChangePrepaymentFlowTargetScreen(
            ChangePrepaymentFlowFragment.Arguments(
                orderId = viewObject.orderId.toString(),
                isPreorder = viewObject.isPreorder,
                isFromCheckout = false,
                isSpasiboPayEnabled = false,
                viewObject.paymentMethod,
                payer,
                isStationSubscription = viewObject.isStationSubscriptionItem,
            )
        )

        verify(router).navigateTo(expectedTarget)
    }

    @Test
    fun `Navigate to login and orders`() {
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.LOGIN_IN_ORDER)

        val expectedLoginTarget = RequestAuthTargetScreen(RequestAuthParams(tryAutoLogin = true))
        var resultListener: ResultListener? = null
        val inOrderChecks = inOrder(router)

        inOrderChecks.verify(router).navigateForResult(
            eq(expectedLoginTarget),
            argThat { actualListener ->
                resultListener = actualListener
                true
            }
        )

        resultListener?.onResult(RequestAuthResult(true))

        inOrderChecks.verify(router)
            .navigateTo(AllOrdersFlowTargetScreen.toMarketOrdersTab())
    }

    @Test
    fun `Navigate to show courier with valid track link`() {
        trackOrderSubject.onSuccess("trackUrl")
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.SHOW_COURIER)

        val expectedScreenArgs = MarketWebActivityArguments.builder()
            .isUrlOverridingEnabled(true)
            .link("trackUrl")
            .titleRes(R.string.order_track_on_map_title)
            .onWebPageShownEvent(
                OrderTrackPageVisibleEvent(
                    viewObject.orderId,
                    viewObject.formattedDeliveryDate
                )
            )
            .build()

        verify(router).navigateTo(MarketWebActivityTargetScreen(expectedScreenArgs))
    }

    @Test
    fun `Navigate to show courier with empty track link`() {
        trackOrderSubject.onSuccess("")
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.SHOW_COURIER)

        val expectedTarget = OrderDetailsTargetScreen(
            OrderDetailsParams(
                orderId = viewObject.orderId.toString(),
                shopOrderId = viewObject.shopId,
                trackDeliveryServiceId = viewObject.trackDeliveryServiceId,
                trackingCode = viewObject.trackingCode,
                isClickAndCollect = viewObject.isClickAndCollect,
                isArchived = false,
                isShowChangeDateTime = false
            )
        )
        verify(router).navigateTo(expectedTarget)
    }

    @Test
    fun `Navigate to show courier on error resolve track link`() {
        trackOrderSubject.onError(Error("test error"))
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.SHOW_COURIER)

        val expectedTarget = OrderDetailsTargetScreen(
            OrderDetailsParams(
                orderId = viewObject.orderId.toString(),
                shopOrderId = viewObject.shopId,
                trackDeliveryServiceId = viewObject.trackDeliveryServiceId,
                trackingCode = viewObject.trackingCode,
                isClickAndCollect = viewObject.isClickAndCollect,
                isArchived = false,
                isShowChangeDateTime = false
            )
        )
        verify(router).navigateTo(expectedTarget)
    }

    @Test
    fun `Navigate to show courier click send event`() {
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.SHOW_COURIER)

        verify(actualOrderSnippetAnalytics).sendCourierButtonClickEvent(viewObject)
    }

    @Test
    fun `Navigate to write feedback`() {
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.WRITE_FEEDBACK)

        val expectedTarget = OrderFeedbackFlowTargetScreen(
            OrderFeedbackFlowFragment.Arguments(
                deliveryType = viewObject.deliveryType,
                orderId = viewObject.orderId.toString(),
                isArchived = viewObject.isArchived,
                shopId = viewObject.shopId,
                isClickAndCollect = viewObject.isClickAndCollect,
                grade = null
            )
        )

        verify(router).navigateTo(expectedTarget)
    }

    @Test
    fun `Navigate to confirm delivery`() {
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.CONFIRM_DELIVERY)

        val expectedTarget = OrderFeedbackDialogTargetScreen(
            OrderFeedbackDialogFragment.Arguments(
                orderId = viewObject.orderId.toString(),
                sourceScreen = router.currentScreen
            )
        )

        verify(orderFeedbackQuestionsAnalytics).sendOrderFeedbackWidgetAccepted(
            viewObject.orderId.toString(),
            router.currentScreen
        )
        verify(router).navigateTo(expectedTarget)
    }

    @Test
    fun `Navigate to decline delivery`() {
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.DECLINE_DELIVERY)

        val expectedTarget = OrderFeedbackQuestionsDialogTargetScreen(
            OrderFeedbackQuestionsDialogFragment.Arguments(
                orderId = viewObject.orderId.toString(),
                sourceScreen = router.currentScreen,
                isDsbs = viewObject.isDsbs
            )
        )

        verify(orderFeedbackQuestionsAnalytics).sendOrderFeedbackWidgetDecline(
            viewObject.orderId.toString(),
            router.currentScreen
        )
        verify(router).navigateTo(expectedTarget)
    }

    @Test
    fun `Request reload parent widget on feedback close`() {
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.FEEDBACK_CLOSE)

        verify(useCases).requestReloadWidgetData(PARENT_WIDGET_ID)
    }

    @Test
    fun `On track lavka courier click navigate to on demand screen`() {
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.LAVKA_COURIER)

        val onDemandCourierLink = OnDemandCourierLink(
            url = "onDemandTestUrl",
            appLink = "onDemandTestAppLink",
            isOnDemandCourierLinkToggleEnable = true,
            lavkaPath = null,
            groupedOrdersFeatureEnabled = false,
        )
        onDemandLinkSubject.onSuccess(onDemandCourierLink)

        verify(onDemandCourierScreenManager).openOnDemandDeliveryScreen(
            router,
            viewObject.orderId.toString(),
            viewObject.trackingCode!!,
            true,
            onDemandCourierLink
        )
    }

    @Test
    fun `On track lavka courier click without valid tracking code send analytics`() {
        val presenter = ActualOrderSnippetPresenter(
            presentationSchedulersMock(),
            viewObject.copy(trackingCode = ""),
            PARENT_WIDGET_ID,
            router,
            useCases,
            resourceDataSource,
            orderFeedbackQuestionsAnalytics,
            onDemandHeathFacade,
            installedApplicationManager,
            actualOrderSnippetAnalytics,
            onDemandCourierScreenManager,
            deliveryDateTimeIntervalMapper,
            orderFeedbackHealthFacade
        )
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.LAVKA_COURIER)

        verify(onDemandHeathFacade).sendOnTrackingCodeEvent(router.currentScreen, viewObject.orderId.toString())
    }

    @Test
    fun `On error handle lavka courier track link send analytics`() {
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.LAVKA_COURIER)

        val error = RuntimeException("testError")
        onDemandLinkSubject.onError(error)

        verify(onDemandHeathFacade).sendOnDemandCourierLinkError(
            error,
            router.currentScreen,
            viewObject.orderId.toString(),
            viewObject.trackingCode,
            null,
            null
        )
    }

    @Test
    fun `Open order consultation chat`() {
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.CHAT)

        val expectedTarget = ConsultationFlowTargetScreen(
            ConsultationFlowArguments.SellerConsultation(
                null,
                false,
                viewObject.orderId,
                viewObject.chatId,
            )
        )
        verify(router).navigateTo(expectedTarget)
    }

    @Test
    fun `Do nothing on none action click`() {
        presenter.handleButtonAction(CmsActualOrderVo.ActionType.NONE)

        verifyZeroInteractions(viewState)
        verifyZeroInteractions(useCases)
        verifyZeroInteractions(router)
        verifyZeroInteractions(onDemandCourierScreenManager)
    }

    companion object {
        private const val PARENT_WIDGET_ID = "parentId"
    }
}
