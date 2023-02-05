package ru.yandex.market.clean.presentation.feature.order.change.agitation

import io.reactivex.Completable
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.common.dateformatter.DateFormatter
import ru.yandex.market.clean.data.mapper.DeliveryDateTimeIntervalMapper
import ru.yandex.market.clean.domain.model.agitations.AgitationType
import ru.yandex.market.clean.domain.usecase.agitation.OrderAgitationUseCases
import ru.yandex.market.clean.presentation.feature.ondemand.OnDemandAnalytics
import ru.yandex.market.clean.presentation.feature.ondemand.OnDemandCourierScreenManager
import ru.yandex.market.clean.presentation.feature.order.change.carousel.OrderItemVo
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.parcelable.media.EmptyImageReferenceParcelable
import ru.yandex.market.common.featureconfigs.managers.OrderConsultationToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.data.order.OrderStatus
import ru.yandex.market.data.order.PaymentType
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.feature.money.viewobject.MoneyVo
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.util.manager.InstalledApplicationManager

class OrderAgitationPresenterTest {


    @Test
    fun `Presenter automatically completes agitation that does not expect user feedback`() {

        val useCases = mock<OrderAgitationUseCases>()
        whenever(useCases.completeAgitation(any())).thenReturn(Completable.complete())

        val args = buildArgs(AgitationType.ORDER_ITEM_REMOVAL)

        val presenter = OrderAgitationPresenter(
            schedulers,
            router,
            useCases,
            resources,
            featureConfigsProvider,
            args,
            deliveryDateTimeIntervalMapper,
            onDemandCourierScreenManager,
            installedApplicationManager,
            onDemandAnalytics,
            dateFormatter,
        )

        presenter.processAgitation()

        verify(useCases, times(1)).completeAgitation(any())
    }

    @Test
    fun `Presenter does not complete agitation automatically when user feedback is expected`() {

        val useCases = mock<OrderAgitationUseCases>()
        whenever(useCases.completeAgitation(any())).thenReturn(Completable.complete())

        val args = buildArgs(AgitationType.ORDER_ITEM_REMOVED_BY_USER_EXTERNALLY)

        val presenter = OrderAgitationPresenter(
            schedulers,
            router,
            useCases,
            resources,
            featureConfigsProvider,
            args,
            deliveryDateTimeIntervalMapper,
            onDemandCourierScreenManager,
            installedApplicationManager,
            onDemandAnalytics,
            dateFormatter,
        )

        presenter.processAgitation()

        verify(useCases, never()).completeAgitation(any())
    }

    private fun buildArgs(agitationType: AgitationType): OrderAgitationVo {
        return OrderAgitationVo(
            agitationId = "agitation-id",
            agitationType = agitationType,
            orderId = "order-id",
            eventId = "1234",
            orderStatus = OrderStatus.DELIVERY,
            orderSubstatus = null,
            orderItems = listOf(OrderItemVo(EmptyImageReferenceParcelable(), "", "sku-id", null)),
            orderTotal = MoneyVo.empty(),
            isDsbs = true,
            deliveryInfo = "когда-нибудь",
            isChangeDeliveryDatesAvailable = false,
            timeFrom = null,
            timeTo = null,
            beginDate = null,
            endDate = null,
            delta = MoneyVo.empty(),
            paymentType = PaymentType.PREPAID,
            trackingCode = null,
            onDemandWarehouseType = null,
            regionId = null,
        )
    }

    companion object {

        private val schedulers = presentationSchedulersMock()
        private val router = mock<Router>()
        private val resources = mock<ResourcesManager>()
        private val featureConfigsProvider = mock<FeatureConfigsProvider>()
        private val toggleManager = mock<OrderConsultationToggleManager>()
        private val onDemandCourierScreenManager = mock<OnDemandCourierScreenManager>()
        private val installedApplicationManager = mock<InstalledApplicationManager>()
        private val onDemandAnalytics = mock<OnDemandAnalytics>()
        private val deliveryDateTimeIntervalMapper = mock<DeliveryDateTimeIntervalMapper>()
        private val dateFormatter: DateFormatter = mock<DateFormatter>()

        @JvmStatic
        @BeforeClass
        fun setUp() {
            whenever(resources.getString(any())).thenReturn("")
            whenever(resources.getFormattedString(any(), any())).thenReturn("")
            whenever(featureConfigsProvider.orderConsultationToggleManager).thenReturn(toggleManager)
            whenever(toggleManager.get()).thenReturn(FeatureToggle(true))
        }
    }
}
