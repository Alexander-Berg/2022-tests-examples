package ru.yandex.market.clean.data.mapper.agitations

import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.fapi.dto.AgitationDto
import ru.yandex.market.clean.data.mapper.ImageMapper
import ru.yandex.market.domain.product.model.offer.OfferColor
import ru.yandex.market.clean.domain.model.agitations.AgitationType
import ru.yandex.market.clean.domain.model.agitations.OrderDiff
import ru.yandex.market.clean.domain.model.agitations.OrderItemDiff
import ru.yandex.market.clean.domain.model.agitations.OrderItemDiffStage
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.clean.domain.model.order.orderItemDomainTestInstance
import ru.yandex.market.clean.presentation.formatter.DeliveryDateFormatter
import ru.yandex.market.data.order.OrderStatus
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.domain.money.model.moneyTestInstance
import ru.yandex.market.domain.media.model.ImageReference
import ru.yandex.market.images.ImageUrlFormatter
import ru.yandex.market.data.media.image.avatars.AvatarsUrlFormatter
import ru.yandex.market.data.media.image.avatars.AvatarsUrlParser
import ru.yandex.market.data.media.image.mapper.ImageReferenceMapper
import ru.yandex.market.net.sku.SkuType
import ru.yandex.market.utils.createDate
import java.math.BigDecimal

@RunWith(Parameterized::class)
class OrderAgitationMapperTest(
    private val agitationId: String,
    private val type: String,
    private val agitationType: AgitationType,
    private val entityId: String,
    private val referenceId: String,
    private val orderStatus: OrderStatus,
    private val orderDiff: OrderDiff?
) {
    @Test
    fun `Test agitation mapping`() {

        val dto = AgitationDto(
            id = agitationId,
            type = type,
            entityId = entityId,
            referencedEntity = referenceId
        )

        val agitation = orderAgitationMapper.map(
            dto = dto,
            order = buildOrder(orderStatus),
            isChangeDeliveryDatesAvailable = false,
            orderDiff = orderDiff
        )


        if (agitationType.isOrderDiffRequired) {
            if (orderDiff == null) {
                assertThat(agitation).isNull()
                return
            } else {
                assertThat(agitation?.orderDiff).isNotNull()
            }
        }

        assertThat(agitation?.type).isEqualTo(agitationType)
        assertThat(agitation?.orderId).isEqualTo(entityId)
        assertThat(agitation?.orderStatus).isEqualTo(orderStatus)
        assertThat(agitation?.orderItems).hasSize(2)
        assertThat(agitation?.orderItems?.get(0)?.skuId).isEqualTo(skuId1)
        assertThat(agitation?.orderItems?.get(1)?.countBadgeText).isEqualTo("2")
        assertThat(agitation?.isDsbs).isEqualTo(true)
    }

    private fun buildOrder(orderStatus: OrderStatus): Order {

        return Order.generateTestInstance().copy(
            status = orderStatus,
            offerColor = OfferColor.WHITE,
            hasDeliveryByShop = true,
            isDsbs = true,
            beginDate = createDate(2021, 11, 11),
            endDate = createDate(2021, 11, 11),
            totalPrice = Money(BigDecimal.TEN, Currency.RUR),
            items = orderItems
        )
    }

    companion object {

        const val skuId1 = "item-sku-id1"

        private lateinit var orderAgitationMapper: OrderAgitationMapper
        private lateinit var agitationTypeMapper: AgitationTypeMapper
        private lateinit var imageMapper: ImageMapper
        private lateinit var itemCountMapper: AgitationOrderItemCountBadgeTextMapper
        private lateinit var deliveryDateFormatter: DeliveryDateFormatter
        private lateinit var agitationOrderItemMapper: AgitationOrderItemMapper

        val orderItems = listOf(
            orderItemDomainTestInstance(offerId = "item-id1", count = 1, skuId = skuId1),
            orderItemDomainTestInstance(offerId = "item-id2", count = 2, skuId = "item-sku-id2"),
        )

        @JvmStatic
        @BeforeClass
        fun setUp() {
            imageMapper = ImageMapper(
                ImageReferenceMapper(AvatarsUrlParser(), emptyList()),
                ImageUrlFormatter(AvatarsUrlFormatter())
            )
            agitationTypeMapper = AgitationTypeMapper()
            itemCountMapper = AgitationOrderItemCountBadgeTextMapper()
            agitationOrderItemMapper = AgitationOrderItemMapper(itemCountMapper)
            deliveryDateFormatter = mock<DeliveryDateFormatter>()
            orderAgitationMapper =
                OrderAgitationMapper(agitationTypeMapper, agitationOrderItemMapper, deliveryDateFormatter)

            whenever(deliveryDateFormatter.format(any<Order>())).thenReturn("11-12-2021")
        }

        private fun buildOrderDiff(entityId: String): OrderDiff {
            val orderItemDiff = OrderItemDiff(
                ImageReference.empty(),
                "",
                "123",
                null,
                null,
                null,
                null,
                SkuType.MARKET,
                null,
                null,
                null,
                "Test",
                null,
                1,
                0,
                null,
                "Test",
                null,
                null,
                false,
                false,
                false,
                null,
                OrderItemDiffStage.OTHER
            )
            return OrderDiff("123abc", entityId, moneyTestInstance(), listOf(orderItemDiff))
        }

        @Parameterized.Parameters
        @JvmStatic
        fun parameters(): Iterable<Array<*>> = listOf(
            arrayOf(
                "agitation-id-ORDER_CANCELLED_BY_USER_EXTERNALLY",
                "ORDER_CANCELLED_BY_USER_EXTERNALLY",
                AgitationType.ORDER_CANCELLED_BY_USER_EXTERNALLY,
                "entity-id-ORDER_CANCELLED_BY_USER_EXTERNALLY",
                "reference-id-ORDER_CANCELLED_BY_USER_EXTERNALLY",
                OrderStatus.DELIVERY,
                null
            ),
            arrayOf(
                "agitation-id-ORDER_CANCELLATION_REJECTED_BY_SHOP",
                "ORDER_CANCELLATION_REJECTED_BY_SHOP",
                AgitationType.ORDER_CANCELLATION_REJECTED_BY_SHOP,
                "entity-id-ORDER_CANCELLATION_REJECTED_BY_SHOP",
                "reference-id-ORDER_CANCELLATION_REJECTED_BY_SHOP",
                OrderStatus.PICKUP,
                null
            ),
            arrayOf(
                "agitation-id-ORDER_DELIVERY_DATE_CHANGED_BY_SHOP",
                "ORDER_DELIVERY_DATE_CHANGED_BY_SHOP",
                AgitationType.ORDER_DELIVERY_DATE_CHANGED_BY_SHOP,
                "entity-id-ORDER_DELIVERY_DATE_CHANGED_BY_SHOP",
                "reference-id-ORDER_DELIVERY_DATE_CHANGED_BY_SHOP",
                OrderStatus.PROCESSING,
                null
            ),
            arrayOf(
                "agitation-id-ORDER_DELIVERY_DATE_CHANGED_BY_USER_EXTERNALLY",
                "ORDER_DELIVERY_DATE_CHANGED_BY_USER_EXTERNALLY",
                AgitationType.ORDER_DELIVERY_DATE_CHANGED_BY_USER_EXTERNALLY,
                "entity-id-ORDER_DELIVERY_DATE_CHANGED_BY_USER_EXTERNALLY",
                "reference-id-ORDER_DELIVERY_DATE_CHANGED_BY_USER_EXTERNALLY",
                OrderStatus.DELIVERY,
                null
            ),
            arrayOf(
                "agitation-id-ORDER_ITEM_REMOVAL",
                "ORDER_ITEM_REMOVAL",
                AgitationType.ORDER_ITEM_REMOVAL,
                "entity-id-ORDER_ITEM_REMOVAL",
                "reference-id-ORDER_ITEM_REMOVAL",
                OrderStatus.DELIVERY,
                buildOrderDiff("entity-id-ORDER_ITEM_REMOVAL")
            ),
            arrayOf(
                "agitation-id-ORDER_ITEM_REMOVAL",
                "ORDER_ITEM_REMOVAL",
                AgitationType.ORDER_ITEM_REMOVAL,
                "entity-id-ORDER_ITEM_REMOVAL",
                "reference-id-ORDER_ITEM_REMOVAL",
                OrderStatus.DELIVERY,
                null
            ),
        )
    }
}
