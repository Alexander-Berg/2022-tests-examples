package ru.yandex.market.activity.order.details

import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analytics.offer.OfferAnalyticsFacade
import ru.yandex.market.clean.presentation.feature.order.details.orderItems.OrderItemVO
import ru.yandex.market.feature.money.viewobject.MoneyVo
import ru.yandex.market.presentationSchedulersMock

class OrderDetailsPresenterTest {

    private val offerAnalyticsFacade = mock<OfferAnalyticsFacade>()
    private val schedulers = presentationSchedulersMock()

    private val presenter = OrderDetailsPresenter(
        schedulers = schedulers,
        params = mock(),
        useCases = mock(),
        orderItemMapper = mock(),
        router = mock(),
        resourcesManager = mock(),
        marketWebUrlProviderFactory = mock(),
        installedApplicationManager = mock(),
        metricaSender = mock(),
        alreadyDeliveredQuestionFeatureManager = mock(),
        orderDetailsAnalytics = mock(),
        servicesAnalytics = mock(),
        orderDetailsHealthFacade = mock(),
        requestContextMapper = mock(),
        deliveryDateTimeIntervalMapper = mock(),
        deliveryServiceContactsFormatter = mock(),
        commonActionHelper = mock(),
        featureConfigsProvider = mock(),
        onDemandCourierScreenManager = mock(),
        agitationFormatter = mock(),
        orderFeedbackQuestionAnalytics = mock(),
        orderItemInfoMapper = mock(),
        merchantsInfoFormatter = mock(),
        repeatOrderAnalyticsFacade = mock(),
        offerAnalyticsFacade = offerAnalyticsFacade,
        onDemandAnalytics = mock(),
        orderItemsVoFormatter = mock(),
        orderDetailsStatusFormatter = mock(),
        orderGeneralInformationFormatter = mock(),
        summaryFormatter = mock(),
        orderFooterButtonsFormatter = mock(),
        orderChangeNotificationVoFormatter = mock(),
        orderServicesFormatter = mock(),
        firebaseEcommAnalyticsFacade = mock(),
        errorVoFormatter = mock(),
        onDemandAnalyticFacade = mock(),
        orderFeedbackHealthFacade = mock(),
        changeAddressAnalytics = mock(),
    )

    @Test
    fun `Test order snippet navigate event`() {
        val priceMock = mock<MoneyVo>()
        val vo = mock<OrderItemVO>() {
            on { skuId } doReturn "id"
            on { modelId } doReturn "id"
            on { offerId } doReturn "id"
            on { orderId } doReturn 1
            on { price } doReturn priceMock
            on { offerCpc } doReturn "id"
            on { isInCart } doReturn false
            on { cartCounterArguments } doReturn null
        }
        presenter.onOrderItemClicked(vo)
        verify(offerAnalyticsFacade).orderSnippetNavigate("id", 1, priceMock, null, false, "")
    }
}
