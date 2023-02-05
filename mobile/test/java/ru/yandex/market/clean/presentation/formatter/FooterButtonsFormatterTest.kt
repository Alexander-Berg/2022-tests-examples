package ru.yandex.market.clean.presentation.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.activity.order.ReorderAvailability
import ru.yandex.market.activity.order.ReorderAvailabilityMapper
import ru.yandex.market.clean.domain.model.SupportChannel
import ru.yandex.market.clean.domain.model.chat.MessengerConfig
import ru.yandex.market.domain.product.model.offer.OfferColor
import ru.yandex.market.clean.domain.model.order.AvailableOption
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.clean.domain.model.order.OrderConsultationState
import ru.yandex.market.clean.domain.model.order.OrderOptionsAvailabilities
import ru.yandex.market.clean.domain.model.order.ReceiptStatus
import ru.yandex.market.clean.domain.model.order.orderCancelPolicyTestInstance
import ru.yandex.market.clean.domain.model.order.orderItemDomainTestInstance
import ru.yandex.market.clean.domain.model.order.orderTestInstance
import ru.yandex.market.clean.domain.model.order.receiptTestInstance
import ru.yandex.market.clean.domain.usecase.SupportChannels
import ru.yandex.market.clean.presentation.feature.order.details.buttons.FooterButtonVo
import ru.yandex.market.clean.presentation.feature.order.details.buttons.FooterButtonsFormatter
import ru.yandex.market.data.order.OrderStatus
import ru.yandex.market.common.android.ResourcesManager

@RunWith(Parameterized::class)
class FooterButtonsFormatterTest(
    private val order: Order,
    private val availabilities: OrderOptionsAvailabilities,
    private val isBoxbotEnabled: Boolean,
    private val consultationState: OrderConsultationState,
    private val supportChannels: SupportChannels,
    private val expectedResult: List<FooterButtonVo>
) {

    private val orderItemDomain = orderItemDomainTestInstance()

    private val resourceDataSource = mock<ResourcesManager> {
        on {
            getString(R.string.order_details_repeat_order)
        } doReturn REPEAT

        on {
            getString(R.string.order_list_cancel)
        } doReturn CANCEL

        on {
            getString(R.string.tab_open_postamats)
        } doReturn OPEN_POSTAMAT

        on {
            getString(R.string.order_documents)
        } doReturn RECEIPT

        on {
            getString(R.string.order_button_connect_with_support)
        } doReturn CONNECT_WITH_SUPPORT

        on {
            getString(R.string.order_consultation)
        } doReturn CONSULTATION

        on {
            getString(R.string.order_consultation_dsbs)
        } doReturn CONSULTATION_DBS

        on {
            getString(R.string.return_order)
        } doReturn RETURN
    }

    private val reorderAvailabilityMapper = mock<ReorderAvailabilityMapper>() {
        on {
            map(orderItemDomain)
        } doReturn reorderAvailable
    }

    private val formatter = FooterButtonsFormatter(resourceDataSource, reorderAvailabilityMapper)

    @Test
    fun format() {
        val formatted = formatter.format(order, availabilities, isBoxbotEnabled, consultationState, supportChannels)
        assertThat(formatted.size).isEqualTo(expectedResult.size)
    }

    companion object {

        private const val REPEAT = "Повторить заказ"
        private const val CANCEL = "Отменить заказ"
        private const val OPEN_POSTAMAT = "Открыть постамат"
        private const val RECEIPT = "Документы по заказу"
        private const val CONNECT_WITH_SUPPORT = "Связаться с поддержкой"
        private const val CONSULTATION = "Чат с поддержкой"
        private const val CONSULTATION_DBS = "Чат c продавцом"
        private const val RETURN = "Вернуть заказ"

        private val orderItemDomain = orderItemDomainTestInstance()
        private val reorderAvailable = ReorderAvailability.AVAILABLE
        private val emptySupportChannel = SupportChannels(
            emptyList(),
            MessengerConfig(false, null, null)
        )
        private val orderCancelPolicy = orderCancelPolicyTestInstance(
            expirationDate = "2022-05-21"
        )

        @Parameterized.Parameters
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            arrayOf(
                orderTestInstance(
                    hasCheckpointWithExceptionStatus = true,
                ),
                mock<OrderOptionsAvailabilities>(),
                true,
                mock<OrderConsultationState>(),
                emptySupportChannel,
                emptyList<FooterButtonVo>(),
            ),
            //1
            arrayOf(
                orderTestInstance(
                    hasCheckpointWithExceptionStatus = false,
                    receipts = listOf(
                        receiptTestInstance(
                            status = ReceiptStatus.PRINTED
                        )
                    ),
                    status = OrderStatus.CANCELLED,
                    isClickAndCollect = true,
                    items = listOf(orderItemDomain),
                    offerColor = OfferColor.BLUE
                ),
                OrderOptionsAvailabilities.EMPTY,
                false,
                OrderConsultationState.Forbidden,
                emptySupportChannel,
                listOf(
                    FooterButtonVo(REPEAT, FooterButtonVo.FooterButtonType.Repeat()),
                    FooterButtonVo(RECEIPT, FooterButtonVo.FooterButtonType.PrintReceipt()),
                )
            ),
            //2
            arrayOf(
                orderTestInstance(
                    hasCheckpointWithExceptionStatus = false,
                    status = OrderStatus.UNPAID,
                    isClickAndCollect = true,
                    offerColor = OfferColor.BLUE,
                    awaitCancelation = false,
                    orderCancelPolicy = null,
                    isEstimated = false,
                ),
                OrderOptionsAvailabilities(
                    123L,
                    listOf(AvailableOption.OPEN_PICKUP_TERMINAL)
                ),
                true,
                OrderConsultationState.Allowed,
                emptySupportChannel,
                listOf(
                    FooterButtonVo(OPEN_POSTAMAT, FooterButtonVo.FooterButtonType.OpenPostamat()),
                    FooterButtonVo(CONSULTATION, FooterButtonVo.FooterButtonType.Consultation()),
                    FooterButtonVo(CANCEL, FooterButtonVo.FooterButtonType.Cancel())
                )
            ),
            //3
            arrayOf(
                orderTestInstance(
                    hasCheckpointWithExceptionStatus = false,
                    isDsbs = false,
                ),
                mock<OrderOptionsAvailabilities>(),
                true,
                OrderConsultationState.Forbidden,
                SupportChannels(
                    listOf(SupportChannel.Chat("url"), SupportChannel.Phone("phone")),
                    MessengerConfig(false, null, null)
                ),
                listOf(FooterButtonVo(CONNECT_WITH_SUPPORT, FooterButtonVo.FooterButtonType.ConnectWithSupport()))
            ),
            //4
            arrayOf(
                orderTestInstance(
                    hasCheckpointWithExceptionStatus = false,
                    status = OrderStatus.UNPAID,
                    isClickAndCollect = true,
                    offerColor = OfferColor.BLUE,
                    awaitCancelation = false,
                    orderCancelPolicy = orderCancelPolicy,
                    isEstimated = true,
                ),
                OrderOptionsAvailabilities(
                    123L,
                    listOf(AvailableOption.OPEN_PICKUP_TERMINAL)
                ),
                true,
                OrderConsultationState.Allowed,
                emptySupportChannel,
                listOf(
                    FooterButtonVo(OPEN_POSTAMAT, FooterButtonVo.FooterButtonType.OpenPostamat()),
                    FooterButtonVo(CONSULTATION, FooterButtonVo.FooterButtonType.Consultation()),
                )
            ),
        )
    }
}
