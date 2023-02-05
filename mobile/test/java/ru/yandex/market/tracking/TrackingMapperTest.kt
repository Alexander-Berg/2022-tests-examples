package ru.yandex.market.tracking

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.checkout.pickup.multiple.WorkScheduleFormatter
import ru.yandex.market.checkout.summary.AddressFormatter
import ru.yandex.market.clean.data.mapper.DeliveryTimeIntervalMapper
import ru.yandex.market.clean.data.mapper.WorkScheduleMapper
import ru.yandex.market.clean.presentation.feature.cancel.OrderIdFormatter
import ru.yandex.market.clean.presentation.feature.checkout.success.DeliveryDateTimeFormatter
import ru.yandex.market.clean.presentation.formatter.SupportPhoneFormatter
import ru.yandex.market.data.order.OrderStatus
import ru.yandex.market.data.order.OrderSubstatus
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.domain.delivery.model.DeliveryType
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter
import ru.yandex.market.tracking.model.domain.CreationCheckpoint
import ru.yandex.market.tracking.model.domain.OrderTracking
import ru.yandex.market.utils.createDate

@RunWith(MockitoJUnitRunner::class)
class TrackingMapperTest {
    private val moneyFormatter = mock<MoneyFormatter>()
    private val resourcesDataStore = mock<ResourcesManager>()
    private val workScheduleFormatter = mock<WorkScheduleFormatter>()
    private val deliveryDateTimeFormatter = mock<DeliveryDateTimeFormatter>()
    private val deliveryIntervalMapper = mock<DeliveryTimeIntervalMapper>()
    private val orderIdFormatter = mock<OrderIdFormatter>()
    private val dateTimeProvider = mock<DateTimeProvider>()
    private val addressFormatter = mock<AddressFormatter>()
    private val workScheduleMapper = mock<WorkScheduleMapper>()
    private val checkpointTitleFormatter = mock<CheckpointTitleFormatter>()
    private val supportPhoneFormatter = mock<SupportPhoneFormatter>()

    private val mapper = TrackingMapper(
        moneyFormatter,
        resourcesDataStore,
        workScheduleFormatter,
        deliveryDateTimeFormatter,
        orderIdFormatter,
        dateTimeProvider,
        addressFormatter,
        deliveryIntervalMapper,
        workScheduleMapper,
        checkpointTitleFormatter,
        supportPhoneFormatter
    )

    @Test
    fun `Returns empty title when delivery date is already passed`() {
        val checkpoint = CreationCheckpoint(0L, 0L)
        val yesterday = createDate(2018, 5, 21)
        val today = createDate(2018, 5, 22)
        val tracking = OrderTracking(
            checkpoints = listOf(checkpoint),
            code = "",
            orderId = 0,
            orderItems = emptyList(),
            isAwaitingCancellation = false,
            timeFrom = null,
            timeTo = null,
            hasDeliveryByShop = false,
            deliveryType = DeliveryType.DELIVERY,
            beginDate = yesterday,
            endDate = yesterday,
            orderStatus = OrderStatus.PLACING,
            orderSubstatus = OrderSubstatus.RESERVATION_EXPIRED
        )
        whenever(dateTimeProvider.currentDateTime).thenReturn(today)

        val title = mapper.getTitle(tracking)
        assertThat(title).isNotNull.isEmpty()
    }
}
