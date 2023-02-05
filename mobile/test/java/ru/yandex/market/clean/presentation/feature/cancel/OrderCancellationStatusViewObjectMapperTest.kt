package ru.yandex.market.clean.presentation.feature.cancel

import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.beru.android.R
import ru.yandex.market.data.order.OrderStatus
import ru.yandex.market.domain.delivery.model.DeliveryType
import ru.yandex.market.feature.manager.OrderCancellationPeriodFeatureManager
import ru.yandex.market.common.android.ResourcesManager

@RunWith(Parameterized::class)
class OrderCancellationStatusViewObjectMapperTest(
    private val orderStatus: OrderStatus,
    private val isDsbs: Boolean,
    private val isPayed: Boolean,
    private val deliveryType: DeliveryType,
    private val expectedResult: OrderCancellationStatusVo
) {

    @Test
    fun `Test order cancellation status is mapped correctly`() {
        Assertions
            .assertThat(mapper.map(ORDER_ID, orderStatus, deliveryType, isPayed, isDsbs, null))
            .isEqualTo(expectedResult)
    }

    companion object {

        private val resourcesDataStore = mock<ResourcesManager>()
        private val orderIdFormatter = mock<OrderIdFormatter>()
        private val orderCancellationPeriodFeatureManager = mock<OrderCancellationPeriodFeatureManager>()

        private lateinit var mapper: OrderCancellationStatusViewObjectMapper

        private const val ORDER_ID: Long = 12345678
        private const val FORMATTED_ORDER_ID = "№$ORDER_ID"

        private const val TEMPLATE_ORDER_X_IS_AWAITS_CANCELLATION =
            "Заказ $FORMATTED_ORDER_ID ждёт отмены"

        private const val ORDER_PROCESSING_CANCELLATION_SUB_STATUS =
            "Мы перестали собирать заказ.\n\nЕсли вы его уже оплатили, деньги вернутся в течение нескольких дней."

        private const val WEB_VIEW_ERROR_SSL_CERTIFICATE_CONTINUE =
            "Продолжить"

        private const val ORDER_DELIVERY_CANCELLATION_DSBS_SUB_STATUS =
            "Отправили запрос продавцу, напишем вам в течение 48 часов"

        private const val ORDER_DELIVERY_CANCELLATION_SUB_STATUS =
            "Мы перестали собирать заказ и предупредили службу поддержки.\\n\\n" +
                    "Если вы его уже оплатили, деньги вернутся в течение 10 дней после того, " +
                    "как статус поменяется на «Отменён»."

        private const val TEMPLATE_ORDER_X_IS_CANCELLED =
            "Ваш заказ $FORMATTED_ORDER_ID отменён"

        private const val CLOSE = "Закрыть"

        @JvmStatic
        @BeforeClass
        fun setUp() {
            mapper = OrderCancellationStatusViewObjectMapper(
                resourcesDataStore,
                orderIdFormatter,
                orderCancellationPeriodFeatureManager
            )

            whenever(orderIdFormatter.format(ORDER_ID)).thenReturn(FORMATTED_ORDER_ID)

            whenever(
                resourcesDataStore.getFormattedString(
                    R.string.template_order_x_is_awaits_cancellation, FORMATTED_ORDER_ID
                )
            ).thenReturn(TEMPLATE_ORDER_X_IS_AWAITS_CANCELLATION)

            whenever(
                resourcesDataStore.getString(
                    R.string.order_processing_cancellation_sub_status
                )
            ).thenReturn(ORDER_PROCESSING_CANCELLATION_SUB_STATUS)

            whenever(
                resourcesDataStore.getString(
                    R.string.web_view_error_ssl_certificate_continue
                )
            ).thenReturn(WEB_VIEW_ERROR_SSL_CERTIFICATE_CONTINUE)

            whenever(
                resourcesDataStore.getString(
                    R.string.order_delivery_cancellation_dsbs_sub_status
                )
            ).thenReturn(ORDER_DELIVERY_CANCELLATION_DSBS_SUB_STATUS)

            whenever(
                resourcesDataStore.getString(
                    R.string.order_delivery_cancellation_sub_status
                )
            ).thenReturn(ORDER_DELIVERY_CANCELLATION_SUB_STATUS)

            whenever(
                resourcesDataStore.getString(
                    R.string.empty_string
                )
            ).thenReturn("")

            whenever(
                resourcesDataStore.getFormattedString(
                    R.string.template_order_x_is_cancelled, FORMATTED_ORDER_ID
                )
            ).thenReturn(TEMPLATE_ORDER_X_IS_CANCELLED)

            whenever(
                resourcesDataStore.getString(
                    R.string.close
                )
            ).thenReturn(CLOSE)
        }

        @Parameterized.Parameters(name = "{index}: \"{0};dsbs={1}\" -> {2}")
        @JvmStatic
        fun data(): List<Array<*>> {
            return listOf<Array<*>>(
                arrayOf(
                    OrderStatus.PROCESSING,
                    false,
                    true,
                    DeliveryType.DELIVERY,
                    OrderCancellationStatusVo.builder()
                        .statusTitle(TEMPLATE_ORDER_X_IS_AWAITS_CANCELLATION)
                        .statusDescription(ORDER_PROCESSING_CANCELLATION_SUB_STATUS)
                        .subStatusTitle("")
                        .continueText(WEB_VIEW_ERROR_SSL_CERTIFICATE_CONTINUE)
                        .build()
                ),
                arrayOf(
                    OrderStatus.PROCESSING,
                    true,
                    true,
                    DeliveryType.DELIVERY,
                    OrderCancellationStatusVo.builder()
                        .statusTitle(TEMPLATE_ORDER_X_IS_AWAITS_CANCELLATION)
                        .statusDescription(ORDER_PROCESSING_CANCELLATION_SUB_STATUS)
                        .subStatusTitle("")
                        .continueText(WEB_VIEW_ERROR_SSL_CERTIFICATE_CONTINUE)
                        .build()
                ),
                arrayOf(
                    OrderStatus.DELIVERY,
                    false,
                    true,
                    DeliveryType.DELIVERY,
                    OrderCancellationStatusVo.builder()
                        .statusTitle(TEMPLATE_ORDER_X_IS_AWAITS_CANCELLATION)
                        .statusDescription(ORDER_DELIVERY_CANCELLATION_SUB_STATUS)
                        .subStatusTitle("")
                        .continueText(WEB_VIEW_ERROR_SSL_CERTIFICATE_CONTINUE)
                        .build()
                ),
                arrayOf(
                    OrderStatus.DELIVERY,
                    true,
                    true,
                    DeliveryType.DELIVERY,
                    OrderCancellationStatusVo.builder()
                        .statusTitle(TEMPLATE_ORDER_X_IS_AWAITS_CANCELLATION)
                        .statusDescription(ORDER_DELIVERY_CANCELLATION_DSBS_SUB_STATUS)
                        .subStatusTitle("")
                        .continueText(WEB_VIEW_ERROR_SSL_CERTIFICATE_CONTINUE)
                        .build()
                ),
                arrayOf(
                    OrderStatus.PICKUP,
                    false,
                    true,
                    DeliveryType.DELIVERY,
                    OrderCancellationStatusVo.builder()
                        .statusTitle(TEMPLATE_ORDER_X_IS_AWAITS_CANCELLATION)
                        .statusDescription(ORDER_DELIVERY_CANCELLATION_SUB_STATUS)
                        .subStatusTitle("")
                        .continueText(WEB_VIEW_ERROR_SSL_CERTIFICATE_CONTINUE)
                        .build()
                ),
                arrayOf(
                    OrderStatus.PICKUP,
                    true,
                    true,
                    DeliveryType.DELIVERY,
                    OrderCancellationStatusVo.builder()
                        .statusTitle(TEMPLATE_ORDER_X_IS_AWAITS_CANCELLATION)
                        .statusDescription(ORDER_DELIVERY_CANCELLATION_DSBS_SUB_STATUS)
                        .subStatusTitle("")
                        .continueText(WEB_VIEW_ERROR_SSL_CERTIFICATE_CONTINUE)
                        .build()
                ),
                arrayOf(
                    OrderStatus.PENDING,
                    false,
                    true,
                    DeliveryType.DELIVERY,
                    OrderCancellationStatusVo.builder()
                        .statusTitle(TEMPLATE_ORDER_X_IS_CANCELLED)
                        .statusDescription("")
                        .subStatusTitle("")
                        .continueText(CLOSE)
                        .build()
                ),
                arrayOf(
                    OrderStatus.PENDING,
                    true,
                    true,
                    DeliveryType.DELIVERY,
                    OrderCancellationStatusVo.builder()
                        .statusTitle(TEMPLATE_ORDER_X_IS_CANCELLED)
                        .statusDescription("")
                        .subStatusTitle("")
                        .continueText(CLOSE)
                        .build()
                ),
                arrayOf(
                    OrderStatus.UNPAID,
                    false,
                    false,
                    DeliveryType.DELIVERY,
                    OrderCancellationStatusVo.builder()
                        .statusTitle(TEMPLATE_ORDER_X_IS_CANCELLED)
                        .statusDescription("")
                        .subStatusTitle("")
                        .continueText(CLOSE)
                        .build()
                ),
                arrayOf(
                    OrderStatus.UNPAID,
                    true,
                    false,
                    DeliveryType.DELIVERY,
                    OrderCancellationStatusVo.builder()
                        .statusTitle(TEMPLATE_ORDER_X_IS_CANCELLED)
                        .statusDescription("")
                        .subStatusTitle("")
                        .continueText(CLOSE)
                        .build()
                ),
                arrayOf(
                    OrderStatus.RESERVED,
                    false,
                    true,
                    DeliveryType.DELIVERY,
                    OrderCancellationStatusVo.builder()
                        .statusTitle(TEMPLATE_ORDER_X_IS_CANCELLED)
                        .statusDescription("")
                        .subStatusTitle("")
                        .continueText(CLOSE)
                        .build()
                ),
                arrayOf(
                    OrderStatus.RESERVED,
                    true,
                    true,
                    DeliveryType.DELIVERY,
                    OrderCancellationStatusVo.builder()
                        .statusTitle(TEMPLATE_ORDER_X_IS_CANCELLED)
                        .statusDescription("")
                        .subStatusTitle("")
                        .continueText(CLOSE)
                        .build()
                ),
            )
        }
    }
}